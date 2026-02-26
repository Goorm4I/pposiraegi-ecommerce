package cloud.pposiraegi.ecommerce.common.dto;

import cloud.pposiraegi.ecommerce.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;

import java.net.http.HttpResponse;

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

    public record ErrorResponse(String code, String message) {}
}
