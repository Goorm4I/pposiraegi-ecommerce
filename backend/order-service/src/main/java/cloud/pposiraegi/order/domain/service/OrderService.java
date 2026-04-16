package cloud.pposiraegi.order.domain.service;

import cloud.pposiraegi.common.exception.BusinessException;
import cloud.pposiraegi.common.exception.ErrorCode;
import cloud.pposiraegi.grpc.product.SkuInfo;
import cloud.pposiraegi.order.domain.dto.OrderDto;
import cloud.pposiraegi.order.domain.dto.PaymentDto;
import cloud.pposiraegi.order.domain.entity.CheckoutSession;
import cloud.pposiraegi.order.domain.entity.IdempotencyRecord;
import cloud.pposiraegi.order.domain.entity.Order;
import cloud.pposiraegi.order.domain.enums.IdempotencyStatus;
import cloud.pposiraegi.order.domain.grpc.ProductGrpcClient;
import cloud.pposiraegi.order.domain.grpc.UserGrpcClient;
import cloud.pposiraegi.order.domain.infrastructure.payment.PaymentClient;
import cloud.pposiraegi.order.domain.entity.OrderItem;
import cloud.pposiraegi.order.domain.repository.IdempotencyRecordRepository;
import cloud.pposiraegi.order.domain.repository.OrderItemRepository;
import cloud.pposiraegi.order.domain.repository.OrderRepository;
import cloud.pposiraegi.order.domain.repository.RedisPurchaseLimitRepository;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final TsidFactory tsidFactory;
    private final OrderItemRepository orderItemRepository;
    private final OrderTransactionProcessor orderTransactionProcessor;
    private final ObjectMapper objectMapper;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final RedissonClient redissonClient;
    private final CheckoutSessionService checkoutSessionService;
    private final RedisPurchaseLimitRepository redisPurchaseLimitRepository;
    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final ProductGrpcClient productGrpcClient;
    private final UserGrpcClient userGrpcClient;

    @Transactional(readOnly = true)
    public OrderDto.OrderSheetResponse createOrderSheet(Long userId, OrderDto.OrderSheetRequest request) {
        Long checkoutId = tsidFactory.create().toLong();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<Long> requestSkuIds = request.orderItems().stream()
                .map(OrderDto.OrderItemRequest::skuId)
                .toList();

        List<SkuInfo> grpcSkuInfos = productGrpcClient.getSkuInfos(requestSkuIds);
        Map<Long, SkuInfo> skuMap = grpcSkuInfos.stream()
                .collect(Collectors.toMap(SkuInfo::getSkuId, info -> info));

        Map<Long, CheckoutSession.ProductSnapshot> sessionProducts = new HashMap<>();
        List<CheckoutSession.Item> sessionItems = new ArrayList<>();
        Map<Long, List<OrderDto.OrderItemResponse>> orderItemsByProductId = new HashMap<>();

        for (OrderDto.OrderItemRequest itemRequest : request.orderItems()) {
            SkuInfo sku = skuMap.get(itemRequest.skuId());

            if (sku == null) {
                throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
            }

            if (sku.getStockQuantity() < itemRequest.quantity()) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }

            Integer purchaseLimit = productGrpcClient.getSkuPurchaseLimit(sku.getSkuId());
            if (purchaseLimit != null && purchaseLimit > 0) {
                int alreadyPurchaseCount = redisPurchaseLimitRepository.getCurrentPurchaseCount(sku.getSkuId(), userId);
                if (alreadyPurchaseCount + itemRequest.quantity() > purchaseLimit) {
                    throw new BusinessException(ErrorCode.PURCHASE_LIMIT_EXCEEDED);
                }
            }

            BigDecimal originPrice = new BigDecimal(sku.getOriginUnitPrice());
            BigDecimal salePrice = new BigDecimal(sku.getSaleUnitPrice());
            BigDecimal lineAmount = salePrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalAmount = totalAmount.add(lineAmount);

            sessionProducts.putIfAbsent(sku.getProductId(), new CheckoutSession.ProductSnapshot(sku.getProductName(), sku.getThumbnailUrl()));

            sessionItems.add(new CheckoutSession.Item(sku.getProductId(), sku.getSkuId(), sku.getCombinationKey(), itemRequest.quantity(), originPrice, salePrice));

            orderItemsByProductId.computeIfAbsent(sku.getProductId(), k -> new ArrayList<>())
                    .add(new OrderDto.OrderItemResponse(sku.getCombinationKey(), itemRequest.quantity(), originPrice, salePrice));
        }

        List<OrderDto.ProductResponse> productResponses = sessionProducts.entrySet().stream()
                .map(entry -> new OrderDto.ProductResponse(
                        entry.getValue().name(),
                        entry.getValue().imageUrl(),
                        orderItemsByProductId.get(entry.getKey())
                )).toList();

        CheckoutSession checkoutSession = new CheckoutSession(checkoutId, userId, sessionProducts, sessionItems, totalAmount);
        checkoutSessionService.saveSession(checkoutId, checkoutSession, 15);

        return new OrderDto.OrderSheetResponse(
                checkoutId.toString(),
                productResponses,
                totalAmount,
                getUserShippingAddress(userId)
        );
    }

    public OrderDto.OrderSheetResponse getOrderSheet(Long userId, Long checkoutId) {
        CheckoutSession session = checkoutSessionService.getCheckoutSession(checkoutId);

        if (!session.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHECKOUT_USER_MISMATCH);
        }

        Map<Long, List<OrderDto.OrderItemResponse>> orderItemsByProductId = new HashMap<>();

        for (CheckoutSession.Item item : session.orderItems()) {
            orderItemsByProductId.computeIfAbsent(item.productId(), k -> new ArrayList<>())
                    .add(new OrderDto.OrderItemResponse(
                            item.optionCombination(),
                            item.quantity(),
                            item.originUnitPrice(),
                            item.saleUnitPrice()
                    ));
        }

        List<OrderDto.ProductResponse> productResponses = session.products().entrySet().stream()
                .map(entry -> new OrderDto.ProductResponse(
                        entry.getValue().name(),
                        entry.getValue().imageUrl(),
                        orderItemsByProductId.get(entry.getKey())
                )).toList();

        return new OrderDto.OrderSheetResponse(
                checkoutId.toString(),
                productResponses,
                session.totalAmount(),
                getUserShippingAddress(userId)
        );
    }

    public OrderDto.OrderResponse createOrder(String idempotencyKey, Long userId, OrderDto.OrderRequest request) {
        String requestHash = generateRequestHash(request);
        RLock lock = redissonClient.getLock("lock:order:" + request.checkoutId());

        try {
            boolean isLocked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.HANDLE_ACCESS_DENIED);
            }

            Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findById(idempotencyKey);
            if (recordOpt.isPresent()) {
                IdempotencyRecord existingRecord = recordOpt.get();
                if (!existingRecord.getRequestHash().equals(requestHash)) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                }
                if (IdempotencyStatus.SUCCESS.equals(existingRecord.getStatus())) {
                    try {
                        return objectMapper.readValue(existingRecord.getResponsePayload(), OrderDto.OrderResponse.class);
                    } catch (Exception e) {
                        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                    }
                }
                if (IdempotencyStatus.PENDING.equals(existingRecord.getStatus())) {
                    throw new BusinessException(ErrorCode.ORDER_ALREADY_PROCESSING);
                }
            }

            return orderTransactionProcessor.executeCreateOrder(idempotencyKey, userId, request, requestHash);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void confirmPayment(String paymentKey, String orderNumber, BigDecimal amount, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        if (order.getTotalAmount().compareTo(amount) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        PaymentDto.PaymentResponse pgResponse = paymentClient.confirm(paymentKey, orderNumber, amount);

        if (!"DONE".equals(pgResponse.status())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        // TODO: 응답에 따른 예외처리 구현

        orderTransactionProcessor.completeOrder(order.getId());

        checkoutSessionService.deleteSession(order.getCheckoutId());
    }

    public void failPayment(Long orderNumber, String code, String message, Long userId) {
        Order order = orderRepository.findById(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        orderTransactionProcessor.cancelPendingOrder(order.getId(), userId);
    }


    @Transactional(readOnly = true)
    public List<OrderDto.MyOrderResponse> getMyOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        return orders.stream().map(order -> {
            List<OrderDto.MyOrderItemResponse> itemResponses = itemsByOrderId
                    .getOrDefault(order.getId(), List.of())
                    .stream()
                    .map(item -> new OrderDto.MyOrderItemResponse(
                            item.getProductName(),
                            item.getSkuName(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getDiscountAmount(),
                            item.getStatus() != null ? item.getStatus().name() : null
                    ))
                    .toList();

            return new OrderDto.MyOrderResponse(
                    order.getId().toString(),
                    order.getOrderNumber(),
                    order.getStatus().name(),
                    order.getTotalAmount(),
                    order.getCreatedAt(),
                    itemResponses
            );
        }).toList();
    }

    private OrderDto.ShippingAddressResponse getUserShippingAddress(Long userId) {
        var lastUsedAddress = userGrpcClient.getLastUsedAddress(userId);
        if (lastUsedAddress == null) {
            return null;
        }

        return new OrderDto.ShippingAddressResponse(
                lastUsedAddress.getAddressId(),
                lastUsedAddress.getRecipientName(),
                lastUsedAddress.getZipCode(),
                lastUsedAddress.getBaseAddress(),
                lastUsedAddress.getDetailAddress(),
                lastUsedAddress.getPhoneNumber(),
                lastUsedAddress.getSecondaryPhoneNumber(),
                lastUsedAddress.getRequestMessage()
        );
    }

    private String generateRequestHash(OrderDto.OrderRequest request) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(request));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
