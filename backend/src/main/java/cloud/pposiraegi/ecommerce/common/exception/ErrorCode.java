package cloud.pposiraegi.ecommerce.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C003", "권한이 없습니다."),

    // User & Cart (User 도메인 통합)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST, "U002", "이미 존재하는 이메일입니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "U003", "장바구니 아이템이 존재하지 않습니다."),

    // Product & Review (Product 도메인 통합)
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "재고가 부족합니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "리뷰를 찾을 수 없습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문 내역을 찾을 수 없습니다."),

    // Promotion
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "M001", "만료된 쿠폰입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
