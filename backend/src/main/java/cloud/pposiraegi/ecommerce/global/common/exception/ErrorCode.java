package cloud.pposiraegi.ecommerce.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C003", "접근 권한이 없습니다."),

    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A001", "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 액세스 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "만료된 리프레시 토큰입니다. 다시 로그인해주세요."),
    SESSION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A005", "존재하지 않거나 이미 로그아웃 처리된 세션입니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "A006", "로그아웃 처리된 토큰입니다. 다시 로그인해주세요."),
    TOKEN_USER_MISMATCH(HttpStatus.FORBIDDEN, "A007", "토큰의 사용자 정보가 일치하지 않습니다. 비정상적인 요청입니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "A008", "이용이 정지된 계정입니다. 고객센터에 문의해주세요."),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "A009", "탈퇴 처리된 회원입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자 정보를 찾을 수 없습니다."),
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST, "U002", "이미 존재하는 이메일입니다."),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "U003", "배송지를 찾을 수 없거나 접근 권한이 없습니다."),
    DEFAULT_ADDRESS_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "U004", "기본 배송지는 삭제할 수 없습니다. 다른 주소를 기본으로 설정해 주세요."),
    ADDRESS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "U005", "배송지는 최대 20개까지만 등록할 수 있습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "U010", "장바구니 아이템이 존재하지 않습니다."),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "P010", "요청한 카테고리를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    INVALID_DISCOUNT_VALUE(HttpStatus.BAD_REQUEST, "P011", "할인율이나 할인 금액이 유효하지 않습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P012", "파일 업로드 처리 중 오류가 발생했습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "P013", "지원하지 않는 파일 형식입니다."),
    PRODUCT_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "P014", "등록할 수 있는 상품 이미지 개수를 초과했습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "재고가 부족합니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "리뷰를 찾을 수 없습니다."),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문 내역을 찾을 수 없습니다."),

    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "M001", "만료된 쿠폰입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

/// / ... (기존 에러 코드) ...
//
//CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "P010", "요청한 카테고리를 찾을 수 없습니다."),
//PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
//INVALID_DISCOUNT_VALUE(HttpStatus.BAD_REQUEST, "P011", "할인율이나 할인 금액이 유효하지 않습니다."),
//FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P012", "파일 업로드 처리 중 오류가 발생했습니다."),
//INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "P013", "지원하지 않는 파일 형식입니다."),
//PRODUCT_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "P014", "등록할 수 있는 상품 이미지 개수를 초과했습니다."),
//OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "재고가 부족합니다."),
//REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "리뷰를 찾을 수 없습니다."),
//
//// [추가된 상품/SKU 관련 에러 코드]
//SKU_NOT_FOUND(HttpStatus.NOT_FOUND, "P015", "요청한 상품 옵션(SKU)을 찾을 수 없습니다."),
//DUPLICATE_SKU_CODE(HttpStatus.BAD_REQUEST, "P016", "이미 등록된 SKU 코드입니다."),
//INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "P017", "재고 수량은 0 이상이어야 합니다."),
//PRODUCT_NOT_ON_SALE(HttpStatus.FORBIDDEN, "P018", "현재 판매 중인 상품이 아닙니다."),
//SKU_DISCONTINUED(HttpStatus.FORBIDDEN, "P019", "단종되어 더 이상 구매할 수 없는 옵션입니다."),
//OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P020", "상품 옵션 정보를 찾을 수 없습니다."),
//
//// ... (이하 생략) ...