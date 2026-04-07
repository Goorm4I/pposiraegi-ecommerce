package cloud.pposiraegi.ecommerce.domain.order.controller;

import cloud.pposiraegi.ecommerce.domain.order.dto.OrderDto;
import cloud.pposiraegi.ecommerce.domain.order.service.OrderService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderDto.OrderSheetResponse> createCheckoutSession(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody OrderDto.OrderSheetRequest request) {
        return ApiResponse.success(orderService.createOrderSheet(Long.parseLong(userId), request));
    }

    @GetMapping("/{checkoutId}")
    public ApiResponse<OrderDto.OrderSheetResponse> getCheckoutSession(
            @PathVariable Long checkoutId,
            @AuthenticationPrincipal String userId
    ) {
        return ApiResponse.success(orderService.getOrderSheet(Long.parseLong(userId), checkoutId));
    }

    @PostMapping("/submit")
    public ApiResponse<OrderDto.OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody OrderDto.OrderRequest request
    ) {
        return ApiResponse.success(orderService.createOrder(idempotencyKey, Long.parseLong(userId), request));
    }

    @GetMapping("/success")
    public ApiResponse<Void> confirmPayment(
            @RequestParam String paymentKey,
            @RequestParam String orderNumber,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal String userId
    ) {
        orderService.confirmPayment(paymentKey, orderNumber, amount, Long.parseLong(userId));
        return ApiResponse.success(null);
    }

    @GetMapping("/fail")
    public ApiResponse<Void> failPayment(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam Long orderId,
            @AuthenticationPrincipal String userId
    ) {
        orderService.failPayment(orderId, code, message, Long.parseLong(userId));
        return ApiResponse.success(null);
    }

}
