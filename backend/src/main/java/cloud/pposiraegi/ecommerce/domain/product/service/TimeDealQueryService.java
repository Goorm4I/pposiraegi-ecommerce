package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.dto.TimeDealInfoDto;
import cloud.pposiraegi.ecommerce.domain.product.entity.TimeDeal;
import cloud.pposiraegi.ecommerce.domain.product.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealQueryService {
    private final TimeDealRepository timeDealRepository;

    public List<TimeDealInfoDto.TimeDealInfo> getTimeDeals(List<Long> timeDealIds) {
        if (timeDealIds == null || timeDealIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<TimeDeal> timeDeals = timeDealRepository.findAllByIdIn(timeDealIds);
        LocalDateTime now = LocalDateTime.now();
        return timeDeals.stream()
                .map(timeDeal -> TimeDealInfoDto.TimeDealInfo.from(timeDeal, now))
                .toList();
    }
}
