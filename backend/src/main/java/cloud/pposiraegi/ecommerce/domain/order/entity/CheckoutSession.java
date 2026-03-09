package cloud.pposiraegi.ecommerce.domain.order.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSession implements Serializable {
    private Long checkoutId;
    private Long userId;
    private List<CheckoutItem> orderItems;
    private BigDecimal totalAmount;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItem implements Serializable {
        private Long skuId;
        private int quantity;
        private BigDecimal unitPrice;
    }
}
