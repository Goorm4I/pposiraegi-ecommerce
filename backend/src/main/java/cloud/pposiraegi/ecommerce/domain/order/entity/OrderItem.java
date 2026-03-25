package cloud.pposiraegi.ecommerce.domain.order.entity;

import cloud.pposiraegi.ecommerce.domain.order.enums.ItemSaleType;
import cloud.pposiraegi.ecommerce.domain.order.enums.OrderItemStatus;
import cloud.pposiraegi.ecommerce.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false)
    private ItemSaleType saleType;

    @Column(name = "time_deal_id")
    private Long timeDealId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "shipment_id")
    private Long shipmentId;

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

    @Enumerated(EnumType.STRING)
    @Column
    private OrderItemStatus status;

    @Builder
    public OrderItem(Long id, Long orderId, Long productId, Long skuId, String productName, String skuName, Integer quantity, BigDecimal unitPrice, BigDecimal discountAmount) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.saleType = ItemSaleType.Normal;
        this.skuId = skuId;
        this.productName = productName;
        this.skuName = skuName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountAmount = discountAmount;
        this.status = OrderItemStatus.PROCESSING;
    }

    public void setTimeDealItem(Long timeDealId) {
        this.timeDealId = timeDealId;
        this.saleType = ItemSaleType.Time_Deal;
    }

    public void registerShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }
}
