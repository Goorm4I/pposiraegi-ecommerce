package cloud.pposiraegi.ecommerce.domain.user.user.repository;

import cloud.pposiraegi.ecommerce.domain.user.user.entity.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddressEntity, Long> {
    long countByUserId(Long userId);

    Optional<UserAddressEntity> findByIdAndUserId(Long id, Long userId);

    Optional<UserAddressEntity> findByUserIdAndIsDefaultTrue(Long userId);

    @Query("SELECT a FROM UserAddressEntity a WHERE a.userId = :userId ORDER BY a.isDefault DESC, a.lastUsedAt DESC")
    List<UserAddressEntity> findAllByUserId(Long userId);
}
