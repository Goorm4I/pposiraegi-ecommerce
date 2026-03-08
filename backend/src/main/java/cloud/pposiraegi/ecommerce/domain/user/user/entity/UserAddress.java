package cloud.pposiraegi.ecommerce.domain.user.user.entity;

import cloud.pposiraegi.ecommerce.domain.common.PhoneNumber;
import cloud.pposiraegi.ecommerce.global.common.entity.BaseUpdatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_addresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAddress extends BaseUpdatedEntity {
    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /* 배송지 라벨 사용시
    @Column(name = "address_name", nullable = false)
    private String addressName;
     */

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "phone_number", nullable = false))
    private PhoneNumber phoneNumber;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "secondary_phone_number"))
    private PhoneNumber secondaryPhoneNumber;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Column(name = "base_address", nullable = false)
    private String baseAddress;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "request_message")
    private String requestMessage;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Builder
    public UserAddress(Long id, Long userId, String recipientName, PhoneNumber phoneNumber, PhoneNumber secondaryPhoneNumber, String zipCode, String baseAddress, String detailAddress, String requestMessage, Boolean isDefault) {
        this.id = id;
        this.userId = userId;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.secondaryPhoneNumber = secondaryPhoneNumber;
        this.zipCode = zipCode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
        this.requestMessage = requestMessage;
        this.isDefault = isDefault;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void UpdateAddress(String recipientName, PhoneNumber phoneNumber, PhoneNumber secondaryPhoneNumber, String zipCode, String baseAddress, String detailAddress, String requestMessage, Boolean isDefault) {
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.secondaryPhoneNumber = secondaryPhoneNumber;
        this.zipCode = zipCode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
        this.requestMessage = requestMessage;
        this.isDefault = isDefault;

        updateLastUsedAt();
    }

    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void changeDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
