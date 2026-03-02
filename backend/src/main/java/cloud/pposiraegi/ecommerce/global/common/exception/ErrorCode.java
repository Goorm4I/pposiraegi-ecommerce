package cloud.pposiraegi.ecommerce.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common (공통)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C003", "접근 권한이 없습니다."),

    // Authentication & Authorization (인증/인가 도메인)
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A001", "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 액세스 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "만료된 리프레시 토큰입니다. 다시 로그인해주세요."),
    SESSION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A005", "존재하지 않거나 이미 로그아웃 처리된 세션입니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "A006", "로그아웃 처리된 토큰입니다. 다시 로그인해주세요."),
    TOKEN_USER_MISMATCH(HttpStatus.FORBIDDEN, "A007", "토큰의 사용자 정보가 일치하지 않습니다. 비정상적인 요청입니다."),

    // 상태로 인한 인가(Authorization) 실패
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "A008", "이용이 정지된 계정입니다. 고객센터에 문의해주세요."),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "A009", "탈퇴 처리된 회원입니다."),

    // User & Cart (유저 정보 도메인)
    // 순수하게 유저 정보를 조회/수정할 때 발생하는 에러만 남김
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자 정보를 찾을 수 없습니다."),
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST, "U002", "이미 존재하는 이메일입니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "U003", "장바구니 아이템이 존재하지 않습니다."),

    // Product & Review (상품 도메인)
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "재고가 부족합니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "리뷰를 찾을 수 없습니다."),

    // Order (주문 도메인)
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문 내역을 찾을 수 없습니다."),

    // Promotion (프로모션 도메인)
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "M001", "만료된 쿠폰입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
