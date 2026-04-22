package cloud.pposiraegi.user.domain.auth.controller;

import cloud.pposiraegi.common.constants.AuthConstants;
import cloud.pposiraegi.common.dto.ApiResponse;
import cloud.pposiraegi.common.exception.BusinessException;
import cloud.pposiraegi.common.exception.ErrorCode;
import cloud.pposiraegi.user.domain.auth.dto.AuthDto;
import cloud.pposiraegi.user.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String deviceInfo = request.getHeader("Device-Info");

        AuthDto.LoginResponse response = authService.login(loginRequest, ipAddress, deviceInfo);

        return ApiResponse.success(response);
    }

    @PostMapping("/reissue")
    public ApiResponse<AuthDto.RefreshResponse> reissue(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        AuthDto.RefreshResponse response = authService.reissue(refreshToken);

        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(
            @RequestHeader(AuthConstants.USER_ID_HEADER) String userId,
            @RequestBody AuthDto.LogoutRequest logoutRequest
    ) {
        authService.logout(Long.parseLong(userId), logoutRequest);

        return ApiResponse.success(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr(); // 위 헤더들이 없으면 기본 IP 사용
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
