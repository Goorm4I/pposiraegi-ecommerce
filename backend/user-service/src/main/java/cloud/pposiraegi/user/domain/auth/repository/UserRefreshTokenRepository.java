package cloud.pposiraegi.user.domain.auth.repository;

import cloud.pposiraegi.user.domain.auth.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    Optional<UserRefreshToken> findByTokenValue(String tokenValue);

    void deleteByUserId(Long userId);

    Optional<UserRefreshToken> findByUserId(Long userId);
}
