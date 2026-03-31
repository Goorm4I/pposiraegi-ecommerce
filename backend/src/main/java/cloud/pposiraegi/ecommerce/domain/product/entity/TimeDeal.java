package cloud.pposiraegi.ecommerce.domain.product.entity;

import cloud.pposiraegi.ecommerce.domain.product.enums.TimeDealStatus;
import cloud.pposiraegi.ecommerce.global.common.entity.BaseUpdatedEntity;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "time_deals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeDeal extends BaseUpdatedEntity {
    @Id
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "original_product_id")
    private Long originalProductId;

    @Column(name = "purchase_limit")
    private Integer purchaseLimit;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "remain_quantity", nullable = false)
    private Integer remainQuantity;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeDealStatus status;

    @Builder
    public TimeDeal(Long id, Long productId, Integer totalQuantity, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.productId = productId;
        this.purchaseLimit = 1;
        this.totalQuantity = totalQuantity;
        this.remainQuantity = totalQuantity;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = TimeDealStatus.PENDING;
    }

    public void startTimeDeal() {
        if (this.status != TimeDealStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_TIMEDEAL_START_STATE);
        }
        this.status = TimeDealStatus.ACTIVE;
    }

    public void endTimeDeal() {
        if (this.status != TimeDealStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_TIMEDEAL_END_STATE);
        }
        this.status = TimeDealStatus.EXPIRED;
    }

    public void stopTimeDeal() {
        if (this.status != TimeDealStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.TIMEDEAL_ALREADY_EXPIRED);
        }
        this.status = TimeDealStatus.SUSPENDED;
    }

    public void decreaseQuantity(int amount) {
        if (this.status != TimeDealStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.TIMEDEAL_NOT_ACTIVE);
        }
        if (this.remainQuantity < amount) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }

        this.remainQuantity -= amount;

        if (this.remainQuantity == 0) {
            this.status = TimeDealStatus.EXPIRED;
        }
    }
}
