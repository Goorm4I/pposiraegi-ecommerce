package cloud.pposiraegi.order.domain.service;

import cloud.pposiraegi.common.exception.BusinessException;
import cloud.pposiraegi.common.exception.ErrorCode;
import cloud.pposiraegi.order.domain.dto.OrderDto;
import cloud.pposiraegi.order.domain.entity.CheckoutSession;
import cloud.pposiraegi.order.domain.entity.IdempotencyRecord;
import cloud.pposiraegi.order.domain.entity.Order;
import cloud.pposiraegi.order.domain.entity.OrderItem;
import cloud.pposiraegi.order.domain.enums.IdempotencyStatus;
import cloud.pposiraegi.order.domain.enums.OrderItemStatus;
import cloud.pposiraegi.order.domain.enums.OrderStatus;
import cloud.pposiraegi.order.domain.generator.OrderNumberGenerator;
import cloud.pposiraegi.order.domain.repository.IdempotencyRecordRepository;
import cloud.pposiraegi.order.domain.repository.OrderItemRepository;
import cloud.pposiraegi.order.domain.repository.OrderRepository;
import cloud.pposiraegi.order.domain.repository.RedisPurchaseLimitRepository;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTransactionProcessor {
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final CheckoutSessionService checkoutSessionService;
    private final TsidFactory tsidFactory;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    //    private final ProductStockService productStockService;
    private final ObjectMapper objectMapper;
    //    private final ProductQueryService productQueryService;
    private final RedisPurchaseLimitRepository redisPurchaseLimitRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    @Value("${pg.success-url}")
    private String pgSuccessUrl;

    @Value("${pg.fail-url}")
    private String pgFailUrl;

    @Transactional(timeout = 10)
    public OrderDto.OrderResponse executeCreateOrder(String idempotencyKey, Long userId, OrderDto.OrderRequest request, String requestHash) {
        IdempotencyRecord newRecord = IdempotencyRecord.builder()
                .id(idempotencyKey)
                .handlerName("CREATE_ORDER")
                .requestHash(requestHash)
                .build();
        idempotencyRecordRepository.save(newRecord);

        CheckoutSession session = checkoutSessionService.getCheckoutSession(request.checkoutId());

        if (!session.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHECKOUT_USER_MISMATCH);
        }

        Long orderId = tsidFactory.create().toLong();

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .checkoutId(request.checkoutId())
                .orderNumber(orderNumberGenerator.generate())
                .totalAmount(session.totalAmount())
                .build();

        orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        String orderName = "";

        for (CheckoutSession.Item item : session.orderItems()) {
            if (orderName.isEmpty()) {
                orderName = session.products().get(item.productId()).name();
            }

            OrderItem orderItem = OrderItem.builder()
                    .id(tsidFactory.create().toLong())
                    .orderId(orderId)
                    .productId(item.productId())
                    .skuId(item.skuId())
                    .productName(session.products().get(item.productId()).name())
                    .skuName(item.optionCombination())
                    .quantity(item.quantity())
                    .unitPrice(item.saleUnitPrice())
                    .discountAmount(java.math.BigDecimal.ZERO)
                    .build();

            orderItems.add(orderItem);
        }

        orderItemRepository.saveAll(orderItems);

        int totalQuantity = session.products().size();
        if (totalQuantity > 1) {
            orderName += (" 외 " + (totalQuantity - 1) + " 건");
        }

        OrderDto.PgConfig pgConfig = new OrderDto.PgConfig(pgSuccessUrl, pgFailUrl);

        OrderDto.OrderResponse response = new OrderDto.OrderResponse(
                order.getOrderNumber(),
                orderName,
                order.getTotalAmount().longValue(),
                pgConfig
        );

        try {
            newRecord.updateResponse(IdempotencyStatus.SUCCESS, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    private void cancelExistingPendingOrders(Long userId) {
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.PENDING_PAYMENT);

        for (Order pendingOrder : pendingOrders) {
            processOrderCancellation(pendingOrder, userId);
        }
    }

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.updateStatus(OrderStatus.PAID);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            item.updateStatus(OrderItemStatus.PAID);
        }
    }

    @Transactional
    public void cancelPendingOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PROCESSED);
        }

        processOrderCancellation(order, userId);
    }

    private void processOrderCancellation(Order order, Long userId) {
//        order.updateStatus(OrderStatus.CANCELED);
//
//        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
//        Map<Long, Integer> stockRestoreRequest = new HashMap<>();
//        Map<Long, Integer> purchaseLimitRestoreRequest = new HashMap<>();
//
//        for (OrderItem item : items) {
//            item.updateStatus(OrderItemStatus.CANCELED);
//            stockRestoreRequest.put(item.getSkuId(), item.getQuantity());
//
//            Integer limit = productQueryService.getSkuPurchaseLimit(item.getSkuId());
//            if (limit != null && limit > 0) {
//                purchaseLimitRestoreRequest.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
//            }
//        }
//
//        productStockService.increaseStocks(stockRestoreRequest);
//
//        purchaseLimitRestoreRequest.forEach((skuId, qty) -> {
//            redisPurchaseLimitRepository.decreasePurchaseCount(skuId, userId, qty);
//        });
    }

}
