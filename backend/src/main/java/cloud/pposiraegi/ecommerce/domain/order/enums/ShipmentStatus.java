package cloud.pposiraegi.ecommerce.domain.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShipmentStatus {
    PENDING("배송 대기"),
    PREPARING("상품 준비 중"),
    SHIPPING("배송 중"),
    DELIVERED("배송 완료"),
    RETURN_COLLECTING("반품 수거 중"),
    RETURN_COMPLETED("반품 완료");

    private final String description;
}
