package cloud.pposiraegi.ecommerce.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

    public record ShippingAddressResponse(
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
            List<ProductResponse> products,
            BigDecimal totalAmount,
            ShippingAddressResponse shippingAddress
    ) {
    }

    public record ProductResponse(
            String name,
            String thumbnailUrl,
            List<OrderItemResponse> orderItems
    ) {
    }

    public record OrderItemResponse(
            String name,
            Integer quantity,
            BigDecimal originUnitPrice,
            BigDecimal saleUnitPrice
    ) {
    }

    public record OrderRequest(
            @NotNull Long checkoutId,
            @NotNull Long shippingAddressId,
            @NotBlank String paymentMethod
    ) {
    }

    public record OrderResponse(
            String orderNumber,
            String orderName,
            Long amount,
            //CustomerInfo customerInfo,
            PgConfig pgConfig
    ) {
    }

    public record PgConfig(
            String successUrl,
            String failUrl
    ) {
    }
}
