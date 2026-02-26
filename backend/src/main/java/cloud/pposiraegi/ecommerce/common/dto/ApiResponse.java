package cloud.pposiraegi.ecommerce.common.dto;

public record ApiResponse<T>(String status, String code, String message, T data) {
    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<T>("SUCCESS", code, message, data);
    }
    public static <T> ApiResponse<T> success(String code, String message) {
        return new ApiResponse<T>("SUCCESS", code, message, null);
    }
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>("ERROR", code, message, null);
    }
}
