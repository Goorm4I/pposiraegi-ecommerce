package cloud.pposiraegi.ecommerce.domain.user.user.dto;

import cloud.pposiraegi.ecommerce.domain.user.user.entity.UserEntity;
import cloud.pposiraegi.ecommerce.global.common.validator.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDto {
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank @Size(min = 2, max = 20) String name,
            String nickname,
            @NotBlank @ValidPhoneNumber String phoneNumber
    ) {
    }

    public record UpdateProfileRequest(
            String nickname,
            String phoneNumber,
            String profileImageUrl
    ) {
    }

    public record SimpleResponse(
            Long id,
            String nickname,
            String profileImageUrl
    ) {
        public static SimpleResponse from(UserEntity user) {
            return new SimpleResponse(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl()
            );
        }
    }
}
