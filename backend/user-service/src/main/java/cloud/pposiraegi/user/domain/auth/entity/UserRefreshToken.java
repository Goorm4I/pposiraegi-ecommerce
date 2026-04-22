package cloud.pposiraegi.user.domain.auth.entity;

import cloud.pposiraegi.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_refresh_tokens")
// 멀티 디바이스의 경우 디바이스 당 하나만 로그인 가능
/*
@Table(name = "user_refresh_tokens", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_user_device",
                columnNames = {"user_id", "device_info"}
        )
})
*/
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRefreshToken extends BaseCreatedEntity {
    @Id
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true) // 단일 기기만 허용
    private Long userId;

    @Column(name = "token_value", nullable = false, unique = true)
    private String tokenValue;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    UserRefreshToken(Long id, Long userId, String tokenValue, String ipAddress, String deviceInfo, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenValue = tokenValue;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void updateToken(String refreshToken, String ipAddress, String deviceInfo, LocalDateTime refreshExpiry) {
        this.tokenValue = refreshToken;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.expiresAt = refreshExpiry;
    }
}
