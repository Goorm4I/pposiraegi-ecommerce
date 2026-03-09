package cloud.pposiraegi.ecommerce.domain.order.entity;

import cloud.pposiraegi.ecommerce.domain.order.enums.OrderItemStatus;
import cloud.pposiraegi.ecommerce.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseCreatedEntity {
    @Id
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "sku_name", nullable = false, length = 100)
    private String skuName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column
    private OrderItemStatus status;

    @Builder
    public OrderItem(Long id, Long orderId, Long productId, Long skuId, String productName, String skuName, Integer quantity, BigDecimal unitPrice, BigDecimal discountAmount) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.skuId = skuId;
        this.productName = productName;
        this.skuName = skuName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountAmount = discountAmount;
        this.status = OrderItemStatus.PROCESSING;
    }
}
