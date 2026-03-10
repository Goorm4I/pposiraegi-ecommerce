package cloud.pposiraegi.ecommerce.domain.product.scheduler;

import cloud.pposiraegi.ecommerce.domain.product.repository.ProductSkuRepository;
import cloud.pposiraegi.ecommerce.domain.product.repository.RedisStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncScheduler {
    private final RedissonClient redissonClient;
    private final ProductSkuRepository productSkuRepository;

    // TODO: 서비스의 환경, 피크타임을 고려하여 주기와 처리량을 조정할 것
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void syncStockToDatabase() {
        RSet<Long> dirtySet = redissonClient.getSet(RedisStockRepository.DIRTY_SKU_KEY, LongCodec.INSTANCE);
        Set<Long> dirtySkuIds = dirtySet.removeRandom(100);

        if (dirtySkuIds == null || dirtySkuIds.isEmpty()) {
            return;
        }

        for (Long skuId : dirtySkuIds) {
            String stockKey = RedisStockRepository.STOCK_KEY_PREFIX + skuId;
            if (redissonClient.getAtomicLong(stockKey).isExists()) {
                productSkuRepository.findById(skuId).ifPresent(productSku -> {
                    long currentStock = redissonClient.getAtomicLong(stockKey).get();
                    productSku.updateStock((int) currentStock);
                });
            }
        }

        log.info("Redis -> DB 재고 동기화 완료, 처리 건수: {}", dirtySkuIds.size());
    }
}
