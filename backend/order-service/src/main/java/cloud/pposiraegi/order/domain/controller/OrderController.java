package cloud.pposiraegi.order.domain.controller;

import cloud.pposiraegi.common.constants.AuthConstants;
import cloud.pposiraegi.common.dto.ApiResponse;
import cloud.pposiraegi.order.domain.dto.OrderDto;
import cloud.pposiraegi.order.domain.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/my")
    public ApiResponse<List<OrderDto.MyOrderResponse>> getMyOrders(
            @RequestHeader(AuthConstants.USER_ID_HEADER) String userId
    ) {
        return ApiResponse.success(orderService.getMyOrders(Long.parseLong(userId)));
    }

    @PostMapping
    public ApiResponse<OrderDto.OrderSheetResponse> createCheckoutSession(
            @RequestHeader(AuthConstants.USER_ID_HEADER) String userId,
            @Valid @RequestBody OrderDto.OrderSheetRequest request) {
        return ApiResponse.success(orderService.createOrderSheet(Long.parseLong(userId), request));
    }

    @GetMapping("/{checkoutId}")
    public ApiResponse<OrderDto.OrderSheetResponse> getCheckoutSession(
            @PathVariable Long checkoutId,
            @RequestHeader(AuthConstants.USER_ID_HEADER) String userId
    ) {
        return ApiResponse.success(orderService.getOrderSheet(Long.parseLong(userId), checkoutId));
    }

    @PostMapping("/submit")
    public ApiResponse<OrderDto.OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(AuthConstants.USER_ID_HEADER) String userId,
            @Valid @RequestBody OrderDto.OrderRequest request
    ) {
        return ApiResponse.success(orderService.createOrder(idempotencyKey, Long.parseLong(userId), request));
    }

    @GetMapping("/success")
    public ApiResponse<Void> confirmPayment(
            @RequestParam String paymentKey,
            @RequestParam String orderNumber,
            @RequestParam BigDecimal amount,
            @RequestHeader(AuthConstants.USER_ID_HEADER) String userId
    ) {
        orderService.confirmPayment(paymentKey, orderNumber, amount, Long.parseLong(userId));
        return ApiResponse.success(null);
    }

    @GetMapping("/fail")
    public ApiResponse<Void> failPayment(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam Long orderId,
            @RequestHeader(AuthConstants.USER_ID_HEADER) String userId
    ) {
        orderService.failPayment(orderId, code, message, Long.parseLong(userId));
        return ApiResponse.success(null);
    }

}
