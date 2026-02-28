package cloud.pposiraegi.ecommerce.domain.user.entity;

import cloud.pposiraegi.ecommerce.common.entity.BaseUpdatedEntity;
import cloud.pposiraegi.ecommerce.domain.common.PhoneNumber;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatedEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Embedded
    private PhoneNumber phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(Long id, String email, String passwordHash, String name, String nickname, PhoneNumber phoneNumber) {
        Assert.notNull(id, "ID는 비어있을 수 없습니다.");
        Assert.hasText(email, "이메일 주소는 비어있을 수 없습니다.");
        Assert.hasText(passwordHash, "비밀번호 해쉬는 비어있을 수 없습니다.");
        Assert.hasText(name, "이름은 비어있을 수 없습니다.");
        Assert.hasText(nickname, "닉네임은 비어있을 수 없습니다.");

        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;

        this.status = UserStatus.ACTIVE;
    }

    public void updateNickname(String nickname) {
        Assert.hasText(nickname, "변경할 닉네임은 비어있을 수 없습니다.");
        this.nickname = nickname;
    }

    public void updatePhoneNumber(PhoneNumber phoneNumber) {
        Assert.notNull(phoneNumber, "변경할 전화번호는 비어있을 수 없습니다.");
        this.phoneNumber = phoneNumber;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void withDraw() {
        if (this.status == UserStatus.DELETED) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
