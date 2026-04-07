package cloud.pposiraegi.ecommerce.domain.product.scheduler;

import cloud.pposiraegi.ecommerce.domain.product.service.TimeDealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealScheduler {
    private final TimeDealService timeDealService;

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void updateTimeDealStatus() {
        timeDealService.updateTimeDealStatus();
    }

    @Scheduled(cron = "0 * * * * *")
    public void warmupUpcomingTimeDeals() {
        timeDealService.warmupUpcomingTimeDeals();
    }
}