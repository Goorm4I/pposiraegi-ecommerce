package cloud.pposiraegi.ecommerce.domain.order.entity;

import cloud.pposiraegi.ecommerce.domain.order.enums.OrderStatus;
import cloud.pposiraegi.ecommerce.global.common.entity.BaseUpdatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_orders_checkout_id", columnNames = "checkout_id"),
                @UniqueConstraint(name = "uq_orders_pg_imp_uid", columnNames = "pg_imp_uid")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseUpdatedEntity {
    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_number", unique = true, nullable = false, length = 25)
    private Long orderNumber;

    @Column(name = "checkout_id", nullable = false)
    private Long checkoutId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "pg_imp_uid", unique = true, nullable = false, length = 100)
    private String pgImpUid;

    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Builder
    public Order(Long id, Long userId, Long checkoutId, Long orderNumber, BigDecimal totalAmount, String pgImpUid) {
        this.id = id;
        this.userId = userId;
        this.checkoutId = checkoutId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.pgImpUid = pgImpUid;
        this.status = OrderStatus.CREATED;
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }
}