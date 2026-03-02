package cloud.pposiraegi.ecommerce.domain.user.user.repository;

import cloud.pposiraegi.ecommerce.domain.user.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
