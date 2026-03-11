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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// TODO: DB 동기화 로직 작성, 높은 정합성을 위해 배치 + Redis Stream 또는 이벤트 발행 방식 고려 가능
// TODO: DB 읽기와 Redis 쓰기 간 지연으로 데이터 불일치 가능성, DB 비관적 락 고려
@Service
@RequiredArgsConstructor
public class ProductStockService {
    private final RedissonClient redissonClient;
    private final ProductSkuRepository productSkuRepository;
    private final RedisStockRepository redisStockRepository;

    public void decreaseStock(Long skuId, int quantity) {
        decreaseStocks(Map.of(skuId, quantity));
    }

    public void increaseStock(Long skuId, int quantity) {
        increaseStocks(Map.of(skuId, quantity));
    }

    public void decreaseStocks(Map<Long, Integer> stockRequests) {
        processStock(true, stockRequests);
    }

    public void increaseStocks(Map<Long, Integer> stockRequests) {
        processStock(false, stockRequests);
    }

    private void processStock(boolean isDecrease, Map<Long, Integer> stockRequest) {
        if (stockRequest == null || stockRequest.isEmpty()) {
            return;
        }

        List<Long> skuIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        stockRequest.forEach((skuId, quantity) -> {
            skuIds.add(skuId);
            quantities.add(quantity);
        });

        while (true) {
            List<Object> result = redisStockRepository.executeBulkAtomic(isDecrease, skuIds, quantities);
            long status = (long) result.getFirst();

            if (status == 1L) {
                return;
            } else if (status == -1L) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            } else if (status == -2L) {
                int missingIndex = (int) (long) result.getLast() - 1;
                Long missingSkuId = skuIds.get(missingIndex);
                loadStockWithLock(missingSkuId);
            }
        }
    }

    private void loadStockWithLock(Long skuId) {
        String lockKey = "lock:sku:" + skuId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
            }

            if (redisStockRepository.hasStockKey(skuId)) {
                return;
            }

            ProductSku productSku = productSkuRepository.findById(skuId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
            redisStockRepository.setStock(skuId, productSku.getStockQuantity());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 대기 중 오류가 발생했습니다.");
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(readOnly = true)
    public void warmupProductStock(Long productId) {
        List<ProductSku> skus = productSkuRepository.findByProductId(productId);
        if (skus.isEmpty()) {
            return;
        }

        Map<Long, Integer> stockQuantityMap = skus.stream()
                .collect(Collectors.toMap(ProductSku::getId, ProductSku::getStockQuantity));

        redisStockRepository.setStockInBatch(stockQuantityMap);
    }
}
