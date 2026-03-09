package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.entity.TimeDeal;
import cloud.pposiraegi.ecommerce.domain.product.repository.TimeDealRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RedisTimeDealConcurrencyTest {

    @Autowired
    private RedisTimeDealFacade redisTimeDealFacade;

    @Autowired
    private TimeDealService timeDealService;

    @Autowired
    private TimeDealRepository timeDealRepository;

    @AfterEach
    public void tearDown() {
        timeDealRepository.deleteAll();
    }

    @Test
    @DisplayName("타임딜 재고 차감 시, 분산 락(Redis)을 적용하지 않으면 레이스 컨디션으로 인해 초과 판매가 발생한다.")
    public void badConcurrencyTest() throws InterruptedException {
        // given
        TimeDeal timeDeal = TimeDeal.builder()
                .id(1L)
                .productId(101L)
                .totalQuantity(100) // 초기 재고 100개
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        timeDealRepository.saveAndFlush(timeDeal);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 100명의 사용자가 동시에 재고 차감 요청 (Lock 없음)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 락이 없는 기본 Service 메서드 호출
                    timeDealService.decreaseStock(1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then: 레이스 컨디션 발생으로 인해 재고가 0이 아닐 확률이 높음
        TimeDeal finalTimeDeal = timeDealRepository.findById(1L).orElseThrow();
        System.out.println("락 없음 - 남은 재고: " + finalTimeDeal.getRemainQuantity());
        // 테스트 환경에 따라 성공할 수도 있으나, 대부분의 경우 초과 판매 방지가 안됨
    }

    @Test
    @DisplayName("타임딜 재고 차감 시, 분산 락(Redisson)을 적용하면 동시성 문제가 해결되어 정확히 재고가 차감된다.")
    public void goodConcurrencyTestWithRedis() throws InterruptedException {
        // given
        TimeDeal timeDeal = TimeDeal.builder()
                .id(2L)
                .productId(102L)
                .totalQuantity(100) // 초기 재고 100개
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        timeDealRepository.saveAndFlush(timeDeal);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 100명의 사용자가 동시에 재고 차감 요청 (Redisson 분산 락 적용)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // Redisson 락이 적용된 Facade 메서드 호출
                    redisTimeDealFacade.decreaseStockWithLock(2L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then: 완벽하게 제어되어 남은 재고는 0이어야 함
        TimeDeal finalTimeDeal = timeDealRepository.findById(2L).orElseThrow();
        System.out.println("Redisson 락 적용 - 남은 재고: " + finalTimeDeal.getRemainQuantity());
        assertThat(finalTimeDeal.getRemainQuantity()).isEqualTo(0);
    }
}
