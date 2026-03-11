package cloud.pposiraegi.ecommerce.domain.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderItemStatus {
    PROCESSING("결제 대기"),
    PAID("결제 완료"),

    SHIPPING_IN_PROGRESS("배송 진행 중"),
    DELIVERED("배송 완료"),

    CANCELED("주문 취소"),

    REFUND_REQUESTED("환불 요청"),
    REFUND_PROCESSING("환불 처리 중"),
    REFUNDED("환불 완료"),

    EXCHANGE_REQUESTED("교환 요청"),
    EXCHANGE_PROCESSING("교환 처리 중"),
    EXCHANGED("교환 완료"),

    PURCHASE_CONFIRMED("구매 확정");

    private final String description;
}
