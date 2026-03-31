package cloud.pposiraegi.ecommerce.domain.order.service;

import cloud.pposiraegi.ecommerce.domain.order.dto.OrderDto;
import cloud.pposiraegi.ecommerce.domain.order.entity.CheckoutSession;
import cloud.pposiraegi.ecommerce.domain.order.entity.IdempotencyRecord;
import cloud.pposiraegi.ecommerce.domain.order.entity.Order;
import cloud.pposiraegi.ecommerce.domain.order.entity.OrderItem;
import cloud.pposiraegi.ecommerce.domain.order.enums.IdempotencyStatus;
import cloud.pposiraegi.ecommerce.domain.order.enums.OrderStatus;
import cloud.pposiraegi.ecommerce.domain.order.generator.OrderNumberGenerator;
import cloud.pposiraegi.ecommerce.domain.order.repository.IdempotencyRecordRepository;
import cloud.pposiraegi.ecommerce.domain.order.repository.OrderItemRepository;
import cloud.pposiraegi.ecommerce.domain.order.repository.OrderRepository;
import cloud.pposiraegi.ecommerce.domain.order.repository.RedisPurchaseLimitRepository;
import cloud.pposiraegi.ecommerce.domain.product.service.ProductQueryService;
import cloud.pposiraegi.ecommerce.domain.product.service.ProductStockService;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTransactionProcessor {
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final CheckoutSessionService checkoutSessionService;
    private final TsidFactory tsidFactory;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductStockService productStockService;
    private final ObjectMapper objectMapper;
    private final ProductQueryService productQueryService;
    private final RedisPurchaseLimitRepository redisPurchaseLimitRepository;

    @Value("${pg.success-url}")
    private String pgSuccessUrl;

    @Value("${pg.fail-url}")
    private String pgFailUrl;

    @Transactional(timeout = 3)
    public OrderDto.OrderResponse executeCreateOrder(String idempotencyKey, Long userId, OrderDto.OrderRequest request, String requestHash) {
        cancelExistingPendingOrders(userId);

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
                .orderNumber(OrderNumberGenerator.getInstance().generate())
                .totalAmount(session.totalAmount())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> stockDecreaseRequests = new HashMap<>();
        Map<Long, Integer> purchaseLimitRollbackRequests = new HashMap<>();

        orderRepository.save(order);

        String orderName = "";

        for (CheckoutSession.Item item : session.orderItems()) {
            if (orderName.isEmpty()) {
                orderName = session.products().get(item.productId()).name();
            }

            Integer limit = productQueryService.getSkuPurchaseLimit(item.skuId());

            if (limit != null && limit > 0) {
                //TODO: 수량 제한 초기화 로직 작성
                boolean isAllowed = redisPurchaseLimitRepository.checkAndIncreasePurchaseCount(
                        item.skuId(),
                        userId,
                        limit,
                        item.quantity(),
                        0
                );

                if (!isAllowed) {
                    throw new BusinessException(ErrorCode.PURCHASE_LIMIT_EXCEEDED);
                }

                purchaseLimitRollbackRequests.merge(item.skuId(), item.quantity(), Integer::sum);
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
                    .discountAmount(BigDecimal.ZERO)
                    .build();

            stockDecreaseRequests.put(item.skuId(), item.quantity());

            orderItems.add(orderItem);
        }

        orderItemRepository.saveAll(orderItems);

        productStockService.decreaseStocks(stockDecreaseRequests);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        log.warn("주문 DB 저장 실패, Redis 재고 복구 시도");
                        productStockService.increaseStocks(stockDecreaseRequests);
                    } catch (Exception e) {
                        log.error("CRITICAL: Redis 재고 복구 실패", e);
                    }

                    purchaseLimitRollbackRequests.forEach((skuId, qty) -> {
                        try {
                            redisPurchaseLimitRepository.decreasePurchaseCount(skuId, userId, qty);
                        } catch (Exception e) {
                            log.error("CRITICAL: Redis 구매 수량 복구 실패", e);
                        }
                    });
                }
            }
        });

        int totalQuantity = session.products().size();

        if (totalQuantity > 1) {
            orderName += (" 외 " + (totalQuantity - 1) + " 건");
        }

        OrderDto.PgConfig pgConfig = new OrderDto.PgConfig(pgSuccessUrl, pgFailUrl);

        OrderDto.OrderResponse response = new OrderDto.OrderResponse(
                order.getOrderNumber().toString(),
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
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.PENDING);

        for (Order pendingOrder : pendingOrders) {
            pendingOrder.updateStatus(OrderStatus.CANCELED);

            List<OrderItem> items = orderItemRepository.findByOrderId(pendingOrder.getId());
            Map<Long, Integer> stockRestoreRequest = new HashMap<>();
            Map<Long, Integer> purchaseLimitRestoreRequest = new HashMap<>();

            for (OrderItem item : items) {
                stockRestoreRequest.put(item.getSkuId(), item.getQuantity());

                Integer limit = productQueryService.getSkuPurchaseLimit(item.getSkuId());
                if (limit != null && limit > 0) {
                    purchaseLimitRestoreRequest.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
                }

            }

            productStockService.increaseStocks(stockRestoreRequest);

            purchaseLimitRestoreRequest.forEach((skuId, qty) -> {
                redisPurchaseLimitRepository.decreasePurchaseCount(skuId, userId, qty);
            });
        }
    }
}
