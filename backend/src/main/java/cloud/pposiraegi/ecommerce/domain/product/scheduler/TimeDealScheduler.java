package cloud.pposiraegi.ecommerce.domain.product.scheduler;

import cloud.pposiraegi.ecommerce.domain.product.entity.TimeDeal;
import cloud.pposiraegi.ecommerce.domain.product.enums.TimeDealStatus;
import cloud.pposiraegi.ecommerce.domain.product.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TimeDealScheduler {
    private final TimeDealRepository timeDealRepository;

    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void updateTimeDealStatus() {
        LocalDateTime now = LocalDateTime.now();

        timeDealRepository.findByStatusAndStartTimeLessThanEqual(TimeDealStatus.PENDING, now)
                .forEach(TimeDeal::startTimeDeal);
        timeDealRepository.findByStatusAndEndTimeLessThanEqual(TimeDealStatus.ACTIVE, now)
                .forEach(TimeDeal::endTimeDeal);
    }
}
