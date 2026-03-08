package cloud.pposiraegi.ecommerce.domain.user.user.dto;

import cloud.pposiraegi.ecommerce.domain.user.user.entity.UserAddress;
import cloud.pposiraegi.ecommerce.global.common.validator.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UserAddressDto {
    public record Request(
            @NotBlank @Size(min = 2, max = 20) String recipientName,
            @NotBlank @ValidPhoneNumber String phoneNumber,
            @ValidPhoneNumber String secondaryPhoneNumber,
            @NotBlank @Size(min = 5, max = 5) @Pattern(regexp = "^\\d{5}$") String zipCode,
            @NotBlank @Size(max = 255) String baseAddress,
            @Size(max = 100) String detailAddress,
            @Size(max = 255) String requestMessage,
            Boolean isDefault
    ) {
    }

    public record Response(
            String id,
            String recipientName,
            String phoneNumber,
            String secondaryPhoneNumber,
            String zipCode,
            String baseAddress,
            String detailAddress,
            String requestMessage,
            Boolean isDefault,
            LocalDateTime lastUsedAt
    ) {
        public static Response from(UserAddress address) {
            return new Response(
                    address.getId().toString(),
                    address.getRecipientName(),
                    address.getPhoneNumber().getValue(),
                    address.getSecondaryPhoneNumber() != null ? address.getSecondaryPhoneNumber().getValue() : null,
                    address.getZipCode(),
                    address.getBaseAddress(),
                    address.getDetailAddress(),
                    address.getRequestMessage(),
                    address.getIsDefault(),
                    address.getLastUsedAt()
            );
        }
    }
}
