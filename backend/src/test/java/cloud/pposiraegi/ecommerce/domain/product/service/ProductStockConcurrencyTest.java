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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductStockConcurrencyTest {

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
                .id(TEST_SKU_ID)
                .productId(1L)
                .skuCode("TEST-CONCURRENCY")
                .status(SkuStatus.AVAILABLE)
                .stockQuantity(INITIAL_STOCK)
                .build();

        productSkuRepository.saveAndFlush(sku);

        String stockKey = "stock:sku:" + TEST_SKU_ID;
        redissonClient.getAtomicLong(stockKey).set(INITIAL_STOCK);
    }

    @AfterEach
    void tearDown() {
        productSkuRepository.deleteAll();
        redissonClient.getKeys().flushall();
    }

    @Test
    @DisplayName("100개의 재고에 1,000명이 동시에 1개씩 구매 요청을 하면 100명만 성공해야 한다.")
    void decreaseStock_ConcurrencyTest() throws InterruptedException {
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productStockService.decreaseStock(TEST_SKU_ID, 1);
                    successCount.incrementAndGet();
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

        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(failCount.get()).isEqualTo(threadCount - INITIAL_STOCK);
        assertThat(remainStock).isEqualTo(0);
    }

    @Test
    @DisplayName("재고보다 큰 수량을 요청하면 실패하고 남은 재고에 맞는 요청은 성공해야 한다.")
    void decreaseStock_ExceedingQuantityTest() throws InterruptedException {
        redissonClient.getAtomicLong("stock:sku:" + TEST_SKU_ID).set(10);

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        AtomicInteger successTotalQuantity = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        int[] requestQuantities = {5, 6, 5, 2};

        for (int quantity : requestQuantities) {
            executorService.submit(() -> {
                try {
                    productStockService.decreaseStock(TEST_SKU_ID, quantity);
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

        assertThat(successTotalQuantity.get() + remainStock).isEqualTo(10);
        assertThat(failCount.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("구매 요청과 재고 복구 요청이 동시에 발생해도 데이터 정합성이 유지되어야 한다.")
    void decreaseAndIncreaseStock_ConcurrentTest() throws InterruptedException {
        redissonClient.getAtomicLong("stock:sku:" + TEST_SKU_ID).set(100);

        int purchaseThreadCount = 150;
        int restoreQuantity = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(purchaseThreadCount + 1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

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

        executorService.submit(() -> {
            try {
                productStockService.increaseStock(TEST_SKU_ID, restoreQuantity);
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        long remainStock = redissonClient.getAtomicLong("stock:sku:" + TEST_SKU_ID).get();
        int expectedTotalStock = 100 + restoreQuantity;

        assertThat(successCount.get() + remainStock).isEqualTo(expectedTotalStock);
    }
}