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
    CONCURRENCY_CONFLICT(HttpStatus.CONFLICT, "C004", "현재 접속자가 많아 처리가 지연되고 있습니다. 잠시 후 다시 시도해 주세요."),

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
    SKU_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "해당 상품의 옵션(SKU) 정보를 찾을 수 없습니다."),
    PRODUCT_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "P005", "현재 판매 중이지 않은 상품입니다."),
    PURCHASE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "P007", "1인당 구매 가능 수량을 초과했습니다."),

    SKU_MISMATCH(HttpStatus.BAD_REQUEST, "P006", "상품과 옵션 정보가 일치하지 않습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "재고가 부족합니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "리뷰를 찾을 수 없습니다."),

    // 타임딜 (T)
    TIMEDEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "타임딜 정보를 찾을 수 없습니다."),
    TIMEDEAL_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "T002", "현재 구매 가능한 타임딜 시간이 아닙니다."),
    INVALID_TIME_DEAL_PRODUCT(HttpStatus.BAD_REQUEST, "T003", "타임딜 진행중인 상품이 아닙니다."),
    INVALID_TIMEDEAL_START_STATE(HttpStatus.BAD_REQUEST, "T004", "대기 중인 타임딜만 시작할 수 있습니다."),
    INVALID_TIMEDEAL_END_STATE(HttpStatus.BAD_REQUEST, "T005", "진행 중인 타임딜만 종료할 수 있습니다."),
    TIMEDEAL_ALREADY_EXPIRED(HttpStatus.BAD_REQUEST, "T006", "이미 종료된 타임딜입니다."),
    INVALID_TIMEDEAL_TIME_RANGE(HttpStatus.BAD_REQUEST, "T007", "종료 시간은 시작 시간보다 앞설 수 없습니다."),
    INVALID_TIMEDEAL_START_TIME(HttpStatus.BAD_REQUEST, "T008", "시작 시간은 현재 시간보다 과거일 수 없습니다."),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문 내역을 찾을 수 없습니다."),
    CHECKOUT_NOT_FOUND(HttpStatus.NOT_FOUND, "O002", "존재하지 않거나 이미 만료된 주문서입니다."),
    CHECKOUT_USER_MISMATCH(HttpStatus.FORBIDDEN, "O003", "주문서의 소유자가 일치하지 않습니다."),
    CHECKOUT_ALREADY_PROCESSED(HttpStatus.GONE, "O004", "이미 결제가 완료되었거나 처리 중인 주문서입니다."),

    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "M001", "만료된 쿠폰입니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "쿠폰을 찾을 수 없습니다.");


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
/// / [추가된 상품/SKU 관련 에러 코드]
//SKU_NOT_FOUND(HttpStatus.NOT_FOUND, "P015", "요청한 상품 옵션(SKU)을 찾을 수 없습니다."),
//DUPLICATE_SKU_CODE(HttpStatus.BAD_REQUEST, "P016", "이미 등록된 SKU 코드입니다."),
//INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "P017", "재고 수량은 0 이상이어야 합니다."),
//PRODUCT_NOT_ON_SALE(HttpStatus.FORBIDDEN, "P018", "현재 판매 중인 상품이 아닙니다."),
//SKU_DISCONTINUED(HttpStatus.FORBIDDEN, "P019", "단종되어 더 이상 구매할 수 없는 옵션입니다."),
//OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P020", "상품 옵션 정보를 찾을 수 없습니다."),
//
/// / ... (이하 생략) ...