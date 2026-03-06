package cloud.pposiraegi.ecommerce.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class OrderDto {
    public record OrderSheetRequest(
            @Valid List<OrderItemRequest> orderItems
    ) {
    }

    public record OrderItemRequest(
            @NotNull Long skuId,
            @NotNull @Min(1) Integer quantity
    ) {
    }

    public record OrderItemDetail(
            Long skuId,
            String optionCombination,
            String imageUrl,
            Integer quantity,
            BigDecimal unitPrice
    ) {
    }

    public record ShippingAddress(
            Long addressId,
            String recipientName,
            String zipCode,
            String baseAddress,
            String detailAddress,
            String phoneNumber,
            String secondaryPhoneNumber,
            String requestMessage
    ) {
    }

    public record OrderSheetResponse(
            String checkoutId,
            List<OrderItemDetail> orderItems,
            BigDecimal totalAmount,
            ShippingAddress shippingAddress
    ) {
    }

    public record OrderRequest(
            @NotNull Long tempOrderId,
            @NotNull Long shippingAddressId,
            String requestMessage
    ) {
    }


}
