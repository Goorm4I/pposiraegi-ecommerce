package cloud.pposiraegi.ecommerce.domain.order.controller;

import cloud.pposiraegi.ecommerce.domain.order.dto.OrderDto;
import cloud.pposiraegi.ecommerce.domain.order.service.OrderService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody OrderDto.OrderRequest request
    ) {
        return ApiResponse.success(orderService.createOrder(Long.parseLong(userId), request));
    }


}
