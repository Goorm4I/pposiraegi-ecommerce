import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://pposiraegi-alb-1031682964.ap-northeast-2.elb.amazonaws.com';
const STOCK = Number(__ENV.STOCK || '20');
const ATTEMPTS = Number(__ENV.ATTEMPTS || '50');
const VUS = Number(__ENV.VUS || String(ATTEMPTS));
const USER_COUNT = Number(__ENV.USER_COUNT || String(VUS));
const CHECKOUT_P95_MS = Number(__ENV.CHECKOUT_P95_MS || '5000');
const SUBMIT_P95_MS = Number(__ENV.SUBMIT_P95_MS || '5000');
const PASSWORD = __ENV.PASSWORD || 'password123!';
const USER_PREFIX = __ENV.USER_PREFIX || 'soldout-k6';

export const options = {
  setupTimeout: '5m',
  scenarios: {
    soldout: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ATTEMPTS,
      maxDuration: '2m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95) < 2000'],
    order_checkout_duration: [`p(95) < ${CHECKOUT_P95_MS}`],
    order_submit_duration: [`p(95) < ${SUBMIT_P95_MS}`],
    soldout_order_success: [`count <= ${STOCK}`],
    oversell_detected: ['rate == 0'],
  },
};

const orderSuccess = new Counter('soldout_order_success');
const orderFailure = new Counter('soldout_order_failure');
const checkoutFailure = new Counter('soldout_checkout_failure');
const oversellDetected = new Rate('oversell_detected');
const checkoutDuration = new Trend('order_checkout_duration');
const submitDuration = new Trend('order_submit_duration');

function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  return { headers };
}

function parseJson(res, label) {
  try {
    return res.json();
  } catch (error) {
    throw new Error(`${label} JSON parse failed: status=${res.status}, body=${res.body?.slice(0, 200)}`);
  }
}

function postJson(path, body, token) {
  return http.post(`${BASE_URL}${path}`, JSON.stringify(body), jsonHeaders(token));
}

function futureIso(seconds) {
  const date = new Date(Date.now() + seconds * 1000);
  const pad = (value) => String(value).padStart(2, '0');
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + 'T' + [
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds()),
  ].join(':');
}

function createCategory(runId) {
  const res = postJson('/api/v1/categories', {
    name: `k6완판-${runId}`,
    displayOrder: 999,
  });

  check(res, { 'category created': (r) => r.status === 200 });
  if (res.status !== 200) {
    throw new Error(`category create failed: ${res.status} ${res.body?.slice(0, 200)}`);
  }

  return String(parseJson(res, 'category').data.id);
}

function createTimeDeal(runId, categoryId) {
  const productName = `k6 soldout product ${runId}`;
  const skuCode = `K6-SOLDOUT-${runId}`;

  const res = postJson('/api/v1/admin/time-deals/with-product', {
    product: {
      categoryId,
      name: productName,
      description: 'k6 sold-out concurrency verification product',
      brandName: 'k6',
      originPrice: 10000,
      salePrice: 1000,
      status: 'FOR_SALE',
      images: [
        {
          imageUrl: 'https://example.com/k6-soldout.png',
          imageType: 'THUMBNAIL',
          displayOrder: 1,
        },
      ],
      optionGroups: [],
      skus: [
        {
          skuCode,
          status: 'AVAILABLE',
          additionalPrice: 0,
          stockQuantity: STOCK,
          selectedOptionValues: [],
        },
      ],
    },
    dealQuantity: STOCK,
    startTime: futureIso(60),
    endTime: futureIso(3600),
  });

  check(res, { 'time deal created': (r) => r.status === 200 });
  if (res.status !== 200) {
    throw new Error(`time deal create failed: ${res.status} ${res.body?.slice(0, 200)}`);
  }

  return { productName, skuCode };
}

function findCreatedDeal(productName) {
  for (let i = 0; i < 12; i += 1) {
    const listRes = http.get(`${BASE_URL}/api/v1/time-deals`);
    const list = parseJson(listRes, 'time-deals').data || [];
    const deal = list.find((item) => item.product?.name === productName);

    if (deal) {
      const detailRes = http.get(`${BASE_URL}/api/v1/time-deals/${deal.timeDealId}`);
      const detail = parseJson(detailRes, 'time-deal-detail').data;
      const sku = detail.product.skus[0];

      return {
        timeDealId: String(detail.timeDealId),
        skuId: String(sku.skuId),
        status: detail.status,
      };
    }

    sleep(1);
  }

  throw new Error(`created deal not found: ${productName}`);
}

function waitUntilActive(timeDealId) {
  for (let i = 0; i < 60; i += 1) {
    const detailRes = http.get(`${BASE_URL}/api/v1/time-deals/${timeDealId}`);
    const detail = parseJson(detailRes, 'time-deal-detail').data;
    if (detail.status === 'ACTIVE') {
      return;
    }
    sleep(2);
  }

  throw new Error(`time deal did not become ACTIVE: ${timeDealId}`);
}

function registerLoginAndAddress(index, runId) {
  const email = `${USER_PREFIX}-${runId}-${index}@example.com`;

  const registerRes = postJson('/api/v1/users/register', {
    email,
    password: PASSWORD,
    name: '완판테스터',
    nickname: `soldout-${index}`,
    phoneNumber: '010-1234-5678',
  });

  check(registerRes, { 'register user': (r) => r.status === 200 });
  if (registerRes.status !== 200) {
    throw new Error(`register failed: ${registerRes.status} ${registerRes.body?.slice(0, 200)}`);
  }

  const loginRes = postJson('/api/v1/auth/login', { email, password: PASSWORD });
  check(loginRes, { 'login user': (r) => r.status === 200 });
  if (loginRes.status !== 200) {
    throw new Error(`login failed: ${loginRes.status} ${loginRes.body?.slice(0, 200)}`);
  }

  const token = parseJson(loginRes, 'login').data.accessToken;

  const addressRes = postJson('/api/v1/users/me/addresses', {
    recipientName: '완판테스터',
    phoneNumber: '010-1234-5678',
    secondaryPhoneNumber: null,
    zipCode: '12345',
    baseAddress: '서울시 테스트구',
    detailAddress: '101동 101호',
    requestMessage: '문앞',
    isDefault: true,
  }, token);

  check(addressRes, { 'address user': (r) => r.status === 200 });
  if (addressRes.status !== 200) {
    throw new Error(`address failed: ${addressRes.status} ${addressRes.body?.slice(0, 200)}`);
  }

  return token;
}

export function setup() {
  const runId = `${Date.now()}`;
  const categoryId = createCategory(runId);
  const created = createTimeDeal(runId, categoryId);
  const deal = findCreatedDeal(created.productName);

  console.log(`created soldout deal: timeDealId=${deal.timeDealId}, skuId=${deal.skuId}, stock=${STOCK}`);
  waitUntilActive(deal.timeDealId);
  console.log(`time deal ACTIVE: ${deal.timeDealId}`);

  const tokens = [];
  for (let i = 0; i < USER_COUNT; i += 1) {
    tokens.push(registerLoginAndAddress(i + 1, runId));
  }
  console.log(`users ready: ${tokens.length}/${USER_COUNT}, attempts=${ATTEMPTS}`);

  return { runId, deal, tokens };
}

export default function (data) {
  const token = data.tokens[__ITER % data.tokens.length];
  if (!token) {
    orderFailure.add(1);
    return;
  }

  const checkoutStartedAt = Date.now();
  const checkoutRes = postJson('/api/v1/orders', {
    orderItems: [{
      timeDealId: data.deal.timeDealId,
      skuId: data.deal.skuId,
      quantity: 1,
    }],
  }, token);
  checkoutDuration.add(Date.now() - checkoutStartedAt);

  if (checkoutRes.status !== 200) {
    checkoutFailure.add(1);
    orderFailure.add(1);
    return;
  }

  const checkoutId = String(parseJson(checkoutRes, 'checkout').data.checkoutId);
  const submitStartedAt = Date.now();
  const submitRes = http.post(
    `${BASE_URL}/api/v1/orders/submit`,
    JSON.stringify({
      checkoutId,
      shippingAddressId: null,
      paymentMethod: 'CARD',
    }),
    {
      headers: {
        ...jsonHeaders(token).headers,
        'Idempotency-Key': `soldout-${data.runId}-${__VU}-${__ITER}`,
      },
    },
  );
  submitDuration.add(Date.now() - submitStartedAt);

  if (submitRes.status === 200 && parseJson(submitRes, 'submit').success === true) {
    orderSuccess.add(1);
    oversellDetected.add(0);
    return;
  }

  orderFailure.add(1);
  oversellDetected.add(0);
}

export function teardown(data) {
  const detailRes = http.get(`${BASE_URL}/api/v1/time-deals/${data.deal.timeDealId}`);
  if (detailRes.status === 200) {
    const detail = parseJson(detailRes, 'teardown-detail').data;
    const sku = detail.product.skus[0];
    console.log(`final timeDealId=${detail.timeDealId}, status=${detail.status}, skuId=${sku.skuId}, skuStock=${sku.stockQuantity}`);
  }
  console.log(`expected: soldout_order_success <= ${STOCK}, attempts=${ATTEMPTS}`);
}
