package cloud.pposiraegi.ecommerce.domain.order.service;

import cloud.pposiraegi.ecommerce.domain.order.dto.OrderDto;
import cloud.pposiraegi.ecommerce.domain.order.entity.CheckoutSession;
import cloud.pposiraegi.ecommerce.domain.order.entity.IdempotencyRecord;
import cloud.pposiraegi.ecommerce.domain.order.enums.IdempotencyStatus;
import cloud.pposiraegi.ecommerce.domain.order.repository.IdempotencyRecordRepository;
import cloud.pposiraegi.ecommerce.domain.product.dto.ProductInfoDto;
import cloud.pposiraegi.ecommerce.domain.product.dto.TimeDealInfoDto;
import cloud.pposiraegi.ecommerce.domain.product.service.ProductQueryService;
import cloud.pposiraegi.ecommerce.domain.product.service.TimeDealQueryService;
import cloud.pposiraegi.ecommerce.domain.user.user.service.UserAddressQueryService;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
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
    private final ProductQueryService productQueryService;
    private final TimeDealQueryService timeDealQueryService;

    private final UserAddressQueryService userAddressQueryService;
    private final OrderTransactionProcessor orderTransactionProcessor;
    private final ObjectMapper objectMapper;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final RedissonClient redissonClient;
    private final CheckoutSessionService checkoutSessionService;

    @Transactional(readOnly = true)
    public OrderDto.OrderSheetResponse createOrderSheet(Long userId, OrderDto.OrderSheetRequest request) {
        Long checkoutId = tsidFactory.create().toLong();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<Long> requestSkuIds = request.orderItems().stream()
                .map(OrderDto.OrderItemRequest::skuId)
                .toList();

        List<Long> requestTimeDealIds = request.orderItems().stream()
                .map(OrderDto.OrderItemRequest::timeDealId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ProductInfoDto.ProductAndSkuInfo> skuMap = productQueryService.getSkuInfos(requestSkuIds).stream()
                .collect(Collectors.toMap(ProductInfoDto.ProductAndSkuInfo::skuId, product -> product));

        Map<Long, TimeDealInfoDto.TimeDealInfo> timeDealMap = requestTimeDealIds.isEmpty() ? Collections.emptyMap() :
                timeDealQueryService.getTimeDeals(requestTimeDealIds).stream()
                        .collect(Collectors.toMap(TimeDealInfoDto.TimeDealInfo::id, timeDeal -> timeDeal));

        Map<Long, CheckoutSession.ProductSnapshot> sessionProducts = new HashMap<>();
        List<CheckoutSession.Item> sessionItems = new ArrayList<>();
        Map<Long, List<OrderDto.OrderItemResponse>> orderItemsByProductId = new HashMap<>();

        for (OrderDto.OrderItemRequest itemRequest : request.orderItems()) {
            ProductInfoDto.ProductAndSkuInfo sku = skuMap.get(itemRequest.skuId());

            if (sku == null) {
                throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
            }

            if (sku.stockQuantity() < itemRequest.quantity()) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }

            if (itemRequest.timeDealId() != null) {
                TimeDealInfoDto.TimeDealInfo timeDeal = timeDealMap.get(itemRequest.timeDealId());
                if (timeDeal == null) {
                    throw new BusinessException(ErrorCode.TIMEDEAL_NOT_FOUND);
                }
                if (!timeDeal.productId().equals(sku.productId())) {
                    throw new BusinessException(ErrorCode.INVALID_TIME_DEAL_PRODUCT);
                }
                if (!timeDeal.isActive()) {
                    throw new BusinessException(ErrorCode.TIMEDEAL_NOT_ACTIVE);
                }
                if (timeDeal.purchaseLimit() != null && itemRequest.quantity() > timeDeal.purchaseLimit()) {
                    throw new BusinessException(ErrorCode.PURCHASE_LIMIT_EXCEEDED);
                }
            }

            BigDecimal lineAmount = sku.saleUnitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalAmount = totalAmount.add(lineAmount);

            sessionProducts.putIfAbsent(sku.productId(), new CheckoutSession.ProductSnapshot(sku.productName(), sku.thumbnailUrl()));

            sessionItems.add(new CheckoutSession.Item(sku.productId(), itemRequest.timeDealId(), sku.skuId(), sku.combinationKey(), itemRequest.quantity(), sku.originUnitPrice(), sku.saleUnitPrice()));

            orderItemsByProductId.computeIfAbsent(sku.productId(), k -> new ArrayList<>())
                    .add(new OrderDto.OrderItemResponse(sku.combinationKey(), itemRequest.quantity(), sku.originUnitPrice(), sku.saleUnitPrice()));
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
        RLock lock = redissonClient.getLock("lock:idempotency:" + idempotencyKey);

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
                if (existingRecord.getStatus() == IdempotencyStatus.SUCCESS) {
                    try {
                        return objectMapper.readValue(existingRecord.getResponsePayload(), OrderDto.OrderResponse.class);
                    } catch (Exception e) {
                        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                    }
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


    private OrderDto.ShippingAddressResponse getUserShippingAddress(Long userId) {
        var lastUsedAddress = userAddressQueryService.getLastUsedAddress(userId);
        if (lastUsedAddress == null) {
            return null;
        }

        return new OrderDto.ShippingAddressResponse(
                lastUsedAddress.addressId(),
                lastUsedAddress.recipientName(),
                lastUsedAddress.zipCode(),
                lastUsedAddress.baseAddress(),
                lastUsedAddress.detailAddress(),
                lastUsedAddress.phoneNumber(),
                lastUsedAddress.secondaryPhoneNumber(),
                lastUsedAddress.requestMessage()
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
