package cloud.pposiraegi.ecommerce.global.common.dto;

import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
        int status,
        boolean success,

        @Nullable
        T data,

        @Nullable
        ErrorResponse error
) {
    public static <T> ApiResponse<T> success(@Nullable T data) {
        return new ApiResponse<T>(HttpStatus.OK.value(), true, data, null);
    }

    public static <T> ApiResponse<T> success(HttpStatus status, @Nullable T data) {
        return new ApiResponse<T>(status.value(), true, data, null);
    }

    public static ApiResponse<?> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getStatus().value(), false, null, new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
    }

    public record ErrorResponse(String code, String message) {
    }
}
