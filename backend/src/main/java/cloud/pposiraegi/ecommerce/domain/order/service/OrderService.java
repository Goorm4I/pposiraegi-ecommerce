package cloud.pposiraegi.ecommerce.domain.order.service;

import cloud.pposiraegi.ecommerce.domain.order.dto.OrderDto;
import cloud.pposiraegi.ecommerce.domain.order.entity.CheckoutSession;
import cloud.pposiraegi.ecommerce.domain.order.repository.OrderRepository;
import cloud.pposiraegi.ecommerce.domain.product.service.SkuQueryService;
import cloud.pposiraegi.ecommerce.domain.user.user.service.UserAddressQueryService;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;
    private final TsidFactory tsidFactory;
    private final SkuQueryService skuQueryService;

    private static final String REDIS_KEY_PREFIX = "checkout:session:";
    private final UserAddressQueryService userAddressQueryService;

    @Transactional(readOnly = true)
    public OrderDto.OrderSheetResponse createOrderSheet(Long userId, OrderDto.OrderSheetRequest request) {
        Long checkoutId = tsidFactory.create().toLong();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderDto.OrderItemDetail> orderItems = new ArrayList<>();
        List<CheckoutSession.CheckoutItem> redisItems = new ArrayList<>();

        for (OrderDto.OrderItemRequest itemRequest : request.orderItems()) {
            var sku = skuQueryService.getSkuDetailForOrder(itemRequest.skuId());

            if (sku.stockQuantity() < itemRequest.quantity()) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }

            BigDecimal lineAmount = sku.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalAmount = totalAmount.add(lineAmount);

            orderItems.add(new OrderDto.OrderItemDetail(
                    sku.skuId(),
                    sku.optionCombination(),
                    sku.imageUrl(),
                    itemRequest.quantity(),
                    sku.unitPrice()
            ));

            redisItems.add(new CheckoutSession.CheckoutItem(
                    sku.skuId(),
                    itemRequest.quantity(),
                    sku.unitPrice()
            ));
        }

        CheckoutSession checkoutSession = new CheckoutSession(checkoutId, userId, redisItems, totalAmount);

        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + checkoutId, checkoutSession, Duration.ofHours(1));

        var defaultAddress = userAddressQueryService.getDefaultAddress(userId);

        return new OrderDto.OrderSheetResponse(
                checkoutId.toString(),
                orderItems,
                totalAmount,
                defaultAddress == null ? null : new OrderDto.ShippingAddress(
                        defaultAddress.addressId(),
                        defaultAddress.recipientName(),
                        defaultAddress.zipCode(),
                        defaultAddress.baseAddress(),
                        defaultAddress.detailAddress(),
                        defaultAddress.phoneNumber(),
                        defaultAddress.secondaryPhoneNumber(),
                        defaultAddress.requestMessage()
                )
        );
    }

    public OrderDto.OrderSheetResponse getOrderSheet(String checkoutId, Long userId) {
        String redisKey = REDIS_KEY_PREFIX + checkoutId;
        CheckoutSession checkoutSession = (CheckoutSession) redisTemplate.opsForValue().get(redisKey);

        if (checkoutSession == null) {
            throw new BusinessException(ErrorCode.CHECKOUT_NOT_FOUND);
        }

        if (!checkoutSession.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHECKOUT_USER_MISMATCH);
        }

        List<OrderDto.OrderItemDetail> orderItems = new ArrayList<>();

        for (CheckoutSession.CheckoutItem item : checkoutSession.getOrderItems()) {
            var sku = skuQueryService.getSkuDetailForOrder(item.getSkuId());

            orderItems.add(new OrderDto.OrderItemDetail(
                    sku.skuId(),
                    sku.optionCombination(),
                    sku.imageUrl(),
                    item.getQuantity(),
                    sku.unitPrice()
            ));
        }
        var defaultAddress = userAddressQueryService.getDefaultAddress(userId);

        return new OrderDto.OrderSheetResponse(
                checkoutId,
                orderItems,
                checkoutSession.getTotalAmount(),
                defaultAddress == null ? null : new OrderDto.ShippingAddress(
                        defaultAddress.addressId(),
                        defaultAddress.recipientName(),
                        defaultAddress.zipCode(),
                        defaultAddress.baseAddress(),
                        defaultAddress.detailAddress(),
                        defaultAddress.phoneNumber(),
                        defaultAddress.secondaryPhoneNumber(),
                        defaultAddress.requestMessage()
                )
        );
    }
}
