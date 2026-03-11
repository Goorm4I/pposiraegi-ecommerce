package cloud.pposiraegi.ecommerce.domain.order.service;

import cloud.pposiraegi.ecommerce.domain.order.dto.OrderDto;
import cloud.pposiraegi.ecommerce.domain.order.entity.CheckoutSession;
import cloud.pposiraegi.ecommerce.domain.order.entity.Order;
import cloud.pposiraegi.ecommerce.domain.order.entity.OrderItem;
import cloud.pposiraegi.ecommerce.domain.order.generator.OrderNumberGenerator;
import cloud.pposiraegi.ecommerce.domain.order.repository.OrderItemRepository;
import cloud.pposiraegi.ecommerce.domain.order.repository.OrderRepository;
import cloud.pposiraegi.ecommerce.domain.product.dto.ProductInfoDto;
import cloud.pposiraegi.ecommerce.domain.product.service.ProductQueryService;
import cloud.pposiraegi.ecommerce.domain.product.service.ProductService;
import cloud.pposiraegi.ecommerce.domain.product.service.ProductStockService;
import cloud.pposiraegi.ecommerce.domain.user.user.service.UserAddressQueryService;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;
    private final TsidFactory tsidFactory;
    private final ProductQueryService productQueryService;

    private static final String REDIS_KEY_PREFIX = "checkout:session:";
    private final UserAddressQueryService userAddressQueryService;
    private final OrderItemRepository orderItemRepository;
    private final ProductStockService productStockService;
    private final ProductService productService;
    private final MockPgClient mockPgClient;

    @Transactional(readOnly = true)
    public OrderDto.OrderSheetResponse createOrderSheet(Long userId, OrderDto.OrderSheetRequest request) {
        Long checkoutId = tsidFactory.create().toLong();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<Long> requestSkuIds = request.orderItems().stream().
                map(OrderDto.OrderItemRequest::skuId)
                .toList();

        Map<Long, ProductInfoDto.ProductAndSkuInfo> skuMap = productQueryService.getSkuInfos(requestSkuIds).stream()
                .collect(Collectors.toMap(ProductInfoDto.ProductAndSkuInfo::skuId, product -> product));

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

            BigDecimal lineAmount = sku.saleUnitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalAmount = totalAmount.add(lineAmount);

            sessionProducts.putIfAbsent(sku.productId(), new CheckoutSession.ProductSnapshot(sku.productName(), sku.thumbnailUrl()));
            sessionItems.add(new CheckoutSession.Item(sku.productId(), sku.skuId(), sku.combinationKey(), itemRequest.quantity(), sku.originUnitPrice(), sku.saleUnitPrice()));

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
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + checkoutId, checkoutSession, Duration.ofMinutes(15));

        return new OrderDto.OrderSheetResponse(
                checkoutId.toString(),
                productResponses,
                totalAmount,
                getUserShippingAddress(userId)
        );
    }

    public OrderDto.OrderSheetResponse getOrderSheet(Long userId, Long checkoutId) {
        CheckoutSession session = getCheckoutSession(checkoutId);

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

    @Transactional
    public OrderDto.OrderResponse createOrder(Long userId, OrderDto.OrderRequest request) {
        CheckoutSession session = getCheckoutSession(request.checkoutId());

        if (!session.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHECKOUT_USER_MISMATCH);
        }

        mockPgClient.verifyPayment(request.pgImpUid(), session.totalAmount());

        Long orderId = tsidFactory.create().toLong();

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .checkoutId(request.checkoutId())
                .orderNumber(OrderNumberGenerator.getInstance().generate())
                .totalAmount(session.totalAmount())
                .pgImpUid(request.pgImpUid())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> stockDecreaseRequests = new HashMap<>();

        orderRepository.save(order);

        for (CheckoutSession.Item item : session.orderItems()) {
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

        String redisKey = REDIS_KEY_PREFIX + request.checkoutId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    redisTemplate.delete(redisKey);
                } else if (status == STATUS_ROLLED_BACK) {
                    try {
                        log.warn("주문 DB 저장 실패, Redis 재고 복구 시도");
                        productStockService.increaseStocks(stockDecreaseRequests);
                    } catch (Exception e) {
                        log.error("CRITICAL: Redis 재고 복구 실패", e);
                    }
                }
            }
        });

        return new OrderDto.OrderResponse(orderId.toString());
    }

    private CheckoutSession getCheckoutSession(Long checkoutId) {
        String redisKey = REDIS_KEY_PREFIX + checkoutId;
        CheckoutSession checkoutSession = (CheckoutSession) redisTemplate.opsForValue().get(redisKey);

        if (checkoutSession == null) {
            throw new BusinessException(ErrorCode.CHECKOUT_NOT_FOUND);
        }

        return checkoutSession;
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
}
