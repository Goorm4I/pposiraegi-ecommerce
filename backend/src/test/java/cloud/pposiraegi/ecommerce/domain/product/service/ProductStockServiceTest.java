package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.enums.SkuStatus;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductSkuRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class ProductStockServiceConcurrencyTest {

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private RedissonClient redissonClient;

    private final Long TEST_SKU_ID = 9999L;
    private final int INITIAL_STOCK = 100;

    @BeforeEach
    void setUp() {
        productSkuRepository.deleteAll();
        redissonClient.getKeys().flushall();

        ProductSku sku = ProductSku.builder()
                .id(TEST_SKU_ID) // 9999L
                .productId(1L)
                .skuCode("TEST-CONCURRENCY")
                .status(SkuStatus.AVAILABLE)
                .stockQuantity(INITIAL_STOCK) // 100
                .build();

        productSkuRepository.save(sku);

        String stockKey = "stock:sku:" + TEST_SKU_ID;
        redissonClient.getAtomicLong(stockKey).set(INITIAL_STOCK);

        assertThat(redissonClient.getAtomicLong(stockKey).get()).isEqualTo(INITIAL_STOCK);
    }

    @AfterEach
    void tearDown() {
        productSkuRepository.deleteAll();
        redissonClient.getKeys().flushall();
    }

    @Test
    @DisplayName("100개의 재고에 1,000명이 동시에 1개씩 구매 요청을 하면 100명만 성공하고 900명은 실패해야 한다.")
    void decreaseStock_ConcurrencyTest() throws InterruptedException {
        // given
        int threadCount = 1000;
        // 32개의 스레드가 동시에 작업하도록 스레드 풀 생성 (가상 유저)
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // 1000개의 요청이 모두 끝날 때까지 메인 스레드를 대기시키기 위한 Latch
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 재고 1개 차감 요청
                    productStockService.decreaseStock(TEST_SKU_ID, 1);
                    successCount.incrementAndGet(); // 예외가 발생하지 않으면 성공
                } catch (BusinessException e) {
                    // OUT_OF_STOCK 예외가 발생하면 실패로 카운트
                    if (e.getErrorCode() == ErrorCode.OUT_OF_STOCK) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown(); // 스레드 작업이 끝나면 카운트 감소
                }
            });
        }

        latch.await(); // 1000개의 스레드가 모두 완료될 때까지 대기

        // then
        // 1. Redis에 남은 잔여 재고가 0인지 확인
        String stockKey = "stock:sku:" + TEST_SKU_ID;
        long remainStock = redissonClient.getAtomicLong(stockKey).get();

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK); // 100명 성공
        assertThat(failCount.get()).isEqualTo(threadCount - INITIAL_STOCK); // 900명 실패
        assertThat(remainStock).isEqualTo(0); // 잔여 재고 0개

        System.out.println("✅ 성공한 요청 수: " + successCount.get());
        System.out.println("❌ 실패한 요청 수: " + failCount.get());
        System.out.println("📦 최종 잔여 재고: " + remainStock);
    }

    @Test
    @DisplayName("재고보다 큰 수량을 요청하면 실패하고, 남은 재고에 맞는 다른 사용자의 요청은 성공해야 한다.")
    void decreaseStock_ExceedingQuantityTest() throws InterruptedException {
        // given: 초기 재고를 10개로 세팅
        redissonClient.getAtomicLong("stock:sku:" + TEST_SKU_ID).set(10);

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        AtomicInteger successTotalQuantity = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 테스트 시나리오: 5개, 6개, 5개, 2개 구매 요청을 동시에 보냄
        int[] requestQuantities = {5, 6, 5, 2};

        // when
        for (int quantity : requestQuantities) {
            executorService.submit(() -> {
                try {
                    productStockService.decreaseStock(TEST_SKU_ID, quantity);
                    // 성공 시 차감된 '수량'을 누적
                    successTotalQuantity.addAndGet(quantity);
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.OUT_OF_STOCK) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long remainStock = redissonClient.getAtomicLong("stock:sku:" + TEST_SKU_ID).get();

        // then
        // 멀티스레드 환경이라 어떤 요청이 먼저 처리될지 알 수 없으나,
        // 무결성이 보장된다면 "성공적으로 차감된 총 수량 + 남은 재고 = 초기 재고(10)" 이어야 합니다.
        assertThat(successTotalQuantity.get() + remainStock).isEqualTo(10);
        // 총 18개(5+6+5+2)를 요청했으므로 초기 재고(10)를 초과하여 최소 1건 이상의 실패가 발생해야 합니다.
        assertThat(failCount.get()).isGreaterThan(0);

        System.out.println("✅ 성공적으로 차감된 총 수량: " + successTotalQuantity.get());
        System.out.println("❌ 실패한 요청 수: " + failCount.get());
        System.out.println("📦 최종 잔여 재고: " + remainStock);
    }

    @Test
    @DisplayName("구매 요청과 재고 복구 요청이 동시에 발생해도 데이터 정합성이 유지되어야 한다.")
    void decreaseAndRestoreStock_ConcurrentTest() throws InterruptedException {
        // given: 초기 재고 100개 세팅
        redissonClient.getAtomicLong("stock:sku:" + TEST_SKU_ID).set(100);

        int purchaseThreadCount = 150; // 150명이 1개씩 구매 시도
        int restoreQuantity = 50;      // 50개 복구

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // 구매 요청 스레드 150개 + 재고 복구 스레드 1개
        CountDownLatch latch = new CountDownLatch(purchaseThreadCount + 1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        // 1. 150명의 구매자가 동시에 구매 요청
        for (int i = 0; i < purchaseThreadCount; i++) {
            executorService.submit(() -> {
                try {
                    productStockService.decreaseStock(TEST_SKU_ID, 1);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 2. 누군가 취소하여 재고 50개가 추가(복구)됨
        executorService.submit(() -> {
            try {
                productStockService.restoreStock(TEST_SKU_ID, restoreQuantity);
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        // then
        long remainStock = redissonClient.getAtomicLong("stock:sku:" + TEST_SKU_ID).get();

        // 스레드 실행 순서를 보장할 수 없으므로, 무결성 공식으로 검증합니다.
        // 공식: (초기 재고 100) + (복구된 재고 50) = (성공적으로 차감된 수량) + (최종 남은 재고)
        int expectedTotalStock = 100 + restoreQuantity;
        assertThat(successCount.get() + remainStock).isEqualTo(expectedTotalStock);

        System.out.println("✅ 성공한 구매 요청 수: " + successCount.get());
        System.out.println("❌ 실패한 구매 요청 수: " + failCount.get());
        System.out.println("📦 최종 잔여 재고: " + remainStock);
    }
}