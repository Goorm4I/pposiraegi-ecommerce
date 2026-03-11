package cloud.pposiraegi.ecommerce.domain.order.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CheckoutSession(
        Long checkoutId,
        Long userId,
        Map<Long, ProductSnapshot> products,
        List<Item> orderItems,
        BigDecimal totalAmount
) implements Serializable {

    public record ProductSnapshot(
            String name,
            String imageUrl
    ) implements Serializable {
    }

    public record Item(
            Long productId,
            Long skuId,
            String optionCombination,
            int quantity,
            BigDecimal originUnitPrice,
            BigDecimal saleUnitPrice
    ) implements Serializable {
    }
}