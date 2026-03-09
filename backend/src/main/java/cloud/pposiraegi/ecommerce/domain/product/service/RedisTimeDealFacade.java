package cloud.pposiraegi.ecommerce.domain.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 타임딜 재고 방어를 위한 Redis 분산 락 퍼사드 (Redisson 사용)
 * 
 * 동시 다발적인 타임딜 구매 요청(선착순) 시 데이터베이스의 락(Pessimistic Lock)만으로는
 * 커넥션 풀 고갈 및 성능 저하가 발생할 수 있습니다.
 * 따라서 Redisson 기반의 분산 락을 사용하여, 한 번에 하나의 쓰레드만 
 * 특정 타임딜 상품의 재고 차감 로직에 접근하도록 제어합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTimeDealFacade {

    private final RedissonClient redissonClient;
    private final TimeDealService timeDealService;

    /**
     * 분산 락을 적용한 타임딜 재고 차감 메서드
     *
     * @param timeDealId 타임딜 ID
     */
    public void decreaseStockWithLock(Long timeDealId) {
        // 1. 타임딜 ID를 기반으로 고유한 락 키 생성
        String lockKey = "time_deal_lock:" + timeDealId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 락 획득 시도 (최대 10초 대기, 락 획득 후 3초 뒤 자동 해제)
            // 타임딜 특성상 많은 요청이 몰리므로 대기 시간(waitTime)을 적절히 설정해야 합니다.
            boolean available = lock.tryLock(10, 3, TimeUnit.SECONDS);
            
            if (!available) {
                // 락 획득 실패 시, 빠른 실패(Fast Fail) 처리 혹은 재시도 로직 적용
                log.error("타임딜 재고 차감 락 획득 실패: {}", timeDealId);
                throw new RuntimeException("현재 요청이 많아 처리가 지연되고 있습니다. 다시 시도해주세요.");
            }

            // 3. 락 획득 성공 시 실제 재고 차감 비즈니스 로직 실행 (트랜잭션)
            timeDealService.decreaseStock(timeDealId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재고 차감 중 인터럽트가 발생했습니다.");
        } finally {
            // 4. 로직 완료 후 락 해제 (현재 쓰레드가 락을 보유하고 있을 때만 해제)
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
