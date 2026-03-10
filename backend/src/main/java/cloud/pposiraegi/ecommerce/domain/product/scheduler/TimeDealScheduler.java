package cloud.pposiraegi.ecommerce.domain.product.scheduler;

import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.entity.TimeDeal;
import cloud.pposiraegi.ecommerce.domain.product.enums.TimeDealStatus;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductSkuRepository;
import cloud.pposiraegi.ecommerce.domain.product.repository.RedisStockRepository;
import cloud.pposiraegi.ecommerce.domain.product.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealScheduler {
    private final TimeDealRepository timeDealRepository;
    private final ProductSkuRepository productSkuRepository;
    private final RedissonClient redissonClient;
    private final RedisStockRepository redisStockRepository;

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void updateTimeDealStatus() {
        LocalDateTime now = LocalDateTime.now();

        timeDealRepository.findByStatusAndStartTimeLessThanEqual(TimeDealStatus.PENDING, now)
                .forEach(TimeDeal::startTimeDeal);
        timeDealRepository.findByStatusAndEndTimeLessThanEqual(TimeDealStatus.ACTIVE, now)
                .forEach(TimeDeal::endTimeDeal);
    }

    @Scheduled(cron = "0 * * * * *")
    public void warmupUpcomingTimeDeals() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesLater = now.plusMinutes(5);

        List<TimeDeal> upcomingDeals = timeDealRepository.findByStatusAndStartTimeBetween(TimeDealStatus.PENDING, now, fiveMinutesLater);

        for (TimeDeal upcomingDeal : upcomingDeals) {
            String warmupFlagKey = "warmup:product:" + upcomingDeal.getProductId();
            if (redissonClient.getBucket(warmupFlagKey).isExists()) {
                continue;
            }

            try {
                List<ProductSku> skus = productSkuRepository.findByProductId(upcomingDeal.getProductId());
                for (ProductSku sku : skus) {
                    redisStockRepository.setStockIfNotExists(sku.getId(), sku.getStockQuantity());
                }
                redissonClient.getBucket(warmupFlagKey).set(true, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.error("Redis: 타임딜 워밍업 중 오류 발생 - Product ID: {}", upcomingDeal.getProductId(), e);
            }
        }
    }
}
