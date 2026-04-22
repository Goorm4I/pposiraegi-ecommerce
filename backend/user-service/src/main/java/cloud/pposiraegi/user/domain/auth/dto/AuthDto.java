package cloud.pposiraegi.user.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDto {

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType
    ) {
        public LoginResponse(String accessToken, String refreshToken) {
            this(accessToken, refreshToken, "Bearer");
        }
    }

    public record LogoutRequest(
            String refreshToken
    ) {
    }

    public record RefreshResponse(
            String accessToken
    ) {
    }


}
