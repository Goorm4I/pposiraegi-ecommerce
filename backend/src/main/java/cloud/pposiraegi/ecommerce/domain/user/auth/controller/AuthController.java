package cloud.pposiraegi.ecommerce.domain.user.auth.controller;

import cloud.pposiraegi.ecommerce.domain.user.auth.dto.AuthDto;
import cloud.pposiraegi.ecommerce.domain.user.auth.service.AuthService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/logout")
    public ApiResponse<?> logout(
            @AuthenticationPrincipal String userId,
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
