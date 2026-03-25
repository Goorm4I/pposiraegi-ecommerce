package cloud.pposiraegi.ecommerce.domain.product.dto;

import cloud.pposiraegi.ecommerce.domain.product.entity.TimeDeal;

import java.time.LocalDateTime;

public class TimeDealInfoDto {
    public record TimeDealInfo(
            Long id,
            Long productId,
            boolean isActive,
            Integer purchaseLimit
    ) {
        public static TimeDealInfo from(TimeDeal timeDeal, LocalDateTime now) {
            return new TimeDealInfo(
                    timeDeal.getId(),
                    timeDeal.getProductId(),
                    timeDeal.getStartTime().isBefore(now) && timeDeal.getEndTime().isAfter(now),
                    timeDeal.getPurchaseLimit()
            );
        }
    }
}
