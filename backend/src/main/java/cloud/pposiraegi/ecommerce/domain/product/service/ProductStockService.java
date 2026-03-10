package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductSkuRepository;
import cloud.pposiraegi.ecommerce.domain.product.repository.RedisStockRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// TODO: DB 동기화 로직 작성, 높은 정합성을 위해 배치 + Redis Stream 또는 이벤트 발행 방식 고려 가능
// TODO: DB 읽기와 Redis 쓰기 간 지연으로 데이터 불일치 가능성, DB 비관적 락 고려
@Service
@RequiredArgsConstructor
public class ProductStockService {
    private final RedissonClient redissonClient;
    private final ProductSkuRepository productSkuRepository;
    private final RedisStockRepository redisStockRepository;

    public void decreaseStock(Long skuId, int quantity) {
        String stockKey = "stock:sku:" + skuId;

        Long decreaseResult = redisStockRepository.decreaseAtomic(stockKey, quantity);

        if (decreaseResult != null) {
            if (decreaseResult.equals(RedisStockRepository.OUT_OF_STOCK_CODE)) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }
            return;
        }

        String lockKey = "lock:sku:" + skuId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
            }

            Long retryResult = redisStockRepository.decreaseAtomic(stockKey, quantity);
            if (retryResult != null) {
                if (retryResult.equals(RedisStockRepository.OUT_OF_STOCK_CODE)) {
                    throw new BusinessException(ErrorCode.OUT_OF_STOCK);
                }
                return;
            }

            loadStockFromDB(skuId, stockKey);
            Long finalResult = redisStockRepository.decreaseAtomic(stockKey, quantity);
            if (finalResult.equals(RedisStockRepository.OUT_OF_STOCK_CODE)) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 대기 중 오류가 발생했습니다.");
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void restoreStock(Long skuId, int quantity) {
        String stockKey = "stock:sku:" + skuId;

        Long increaseResult = redisStockRepository.increaseAtomic(stockKey, quantity);
        if (increaseResult != null) {
            return;
        }

        String lockKey = "lock:sku:" + skuId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
            }

            Long retryResult = redisStockRepository.increaseAtomic(stockKey, quantity);
            if (retryResult != null) {
                return;
            }

            loadStockFromDB(skuId, stockKey);
            redisStockRepository.increaseAtomic(stockKey, quantity);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 대기 중 오류가 발생했습니다.");
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void loadStockFromDB(Long skuId, String stockKey) {
        ProductSku productSku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
        redisStockRepository.setStock(stockKey, productSku.getStockQuantity());
    }
}
