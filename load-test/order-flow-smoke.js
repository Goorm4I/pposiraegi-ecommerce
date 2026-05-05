import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://pposiraegi-alb-1031682964.ap-northeast-2.elb.amazonaws.com';
const USER_PREFIX = __ENV.USER_PREFIX || 'eks-k6';
const PASSWORD = __ENV.PASSWORD || 'password123!';
const QTY = Number(__ENV.QTY || '1');

export const options = {
  scenarios: {
    smoke: {
      executor: 'ramping-vus',
      stages: [
        { duration: '20s', target: 1 },
        { duration: '40s', target: 5 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate < 0.05'],
    http_req_duration: ['p(95) < 1500'],
    order_submit_success: ['count > 0'],
    order_flow_duration: ['p(95) < 3000'],
  },
};

const orderSubmitSuccess = new Counter('order_submit_success');
const orderSubmitFailure = new Counter('order_submit_failure');
const orderFlowDuration = new Trend('order_flow_duration');

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

function registerAndLogin(vu, iter) {
  const stamp = `${Date.now()}-${vu}-${iter}`;
  const email = `${USER_PREFIX}-${stamp}@example.com`;

  const registerRes = http.post(
    `${BASE_URL}/api/v1/users/register`,
    JSON.stringify({
      email,
      password: PASSWORD,
      name: '테스트유저',
      nickname: `k6-${vu}`,
      phoneNumber: '010-1234-5678',
    }),
    jsonHeaders(),
  );

  check(registerRes, {
    'register 200': (res) => res.status === 200,
  });

  if (registerRes.status !== 200) {
    throw new Error(`register failed: ${registerRes.status} ${registerRes.body?.slice(0, 200)}`);
  }

  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password: PASSWORD }),
    jsonHeaders(),
  );

  check(loginRes, {
    'login 200': (res) => res.status === 200,
    'login has token': (res) => Boolean(parseJson(res, 'login')?.data?.accessToken),
  });

  if (loginRes.status !== 200) {
    throw new Error(`login failed: ${loginRes.status} ${loginRes.body?.slice(0, 200)}`);
  }

  return parseJson(loginRes, 'login').data.accessToken;
}

function registerAddress(token) {
  const res = http.post(
    `${BASE_URL}/api/v1/users/me/addresses`,
    JSON.stringify({
      recipientName: '테스트유저',
      phoneNumber: '010-1234-5678',
      secondaryPhoneNumber: null,
      zipCode: '12345',
      baseAddress: '서울시 테스트구',
      detailAddress: '101동 101호',
      requestMessage: '문앞',
      isDefault: true,
    }),
    jsonHeaders(token),
  );

  check(res, {
    'address 200': (r) => r.status === 200,
  });

  if (res.status !== 200) {
    throw new Error(`address failed: ${res.status} ${res.body?.slice(0, 200)}`);
  }
}

function getActiveDealAndSku() {
  const listRes = http.get(`${BASE_URL}/api/v1/time-deals`);
  check(listRes, {
    'time-deals 200': (res) => res.status === 200,
  });

  const list = parseJson(listRes, 'time-deals')?.data || [];
  const active = list.find((deal) => deal.status === 'ACTIVE');
  if (!active) {
    throw new Error('ACTIVE time deal not found. Run scripts/seed.sh first.');
  }

  const detailRes = http.get(`${BASE_URL}/api/v1/time-deals/${active.timeDealId}`);
  check(detailRes, {
    'time-deal detail 200': (res) => res.status === 200,
  });

  const detail = parseJson(detailRes, 'time-deal-detail')?.data;
  const sku = detail?.product?.skus?.[0];
  if (!sku) {
    throw new Error(`SKU not found for timeDealId=${active.timeDealId}`);
  }

  return {
    // IDs are Java Long values and can exceed JavaScript's safe integer range.
    // Keep them as strings and let Spring/Jackson coerce them into Long.
    timeDealId: String(detail.timeDealId),
    skuId: String(sku.skuId),
  };
}

function createCheckout(token, item) {
  const res = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({
      orderItems: [{
        timeDealId: item.timeDealId,
        skuId: item.skuId,
        quantity: QTY,
      }],
    }),
    jsonHeaders(token),
  );

  check(res, {
    'checkout 200': (r) => r.status === 200,
  });

  if (res.status !== 200) {
    throw new Error(`checkout failed: ${res.status} ${res.body?.slice(0, 200)}`);
  }

  return parseJson(res, 'checkout').data.checkoutId;
}

function submitOrder(token, checkoutId) {
  const res = http.post(
    `${BASE_URL}/api/v1/orders/submit`,
    JSON.stringify({
      checkoutId: String(checkoutId),
      shippingAddressId: null,
      paymentMethod: 'CARD',
    }),
    {
      headers: {
        ...jsonHeaders(token).headers,
        'Idempotency-Key': `k6-${__VU}-${__ITER}-${Date.now()}`,
      },
    },
  );

  const ok = check(res, {
    'submit 200': (r) => r.status === 200,
    'submit has orderNumber': (r) => Boolean(parseJson(r, 'submit')?.data?.orderNumber),
  });

  if (ok) {
    orderSubmitSuccess.add(1);
  } else {
    orderSubmitFailure.add(1);
  }

  return res;
}

export default function () {
  const startedAt = Date.now();

  group('order happy path', () => {
    const token = registerAndLogin(__VU, __ITER);
    registerAddress(token);
    const item = getActiveDealAndSku();
    const checkoutId = createCheckout(token, item);
    const submitRes = submitOrder(token, checkoutId);

    if (submitRes.status !== 200) {
      console.warn(`submit failed status=${submitRes.status} body=${submitRes.body?.slice(0, 200)}`);
    }
  });

  orderFlowDuration.add(Date.now() - startedAt);
  sleep(1);
}
