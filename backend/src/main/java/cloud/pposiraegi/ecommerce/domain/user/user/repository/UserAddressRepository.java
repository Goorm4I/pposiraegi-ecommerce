package cloud.pposiraegi.ecommerce.domain.user.user.repository;

import cloud.pposiraegi.ecommerce.domain.user.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    long countByUserId(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<UserAddress> findFirstByUserIdOrderByLastUsedAtDesc(Long userId);

    @Query("SELECT a FROM UserAddress a WHERE a.userId = :userId ORDER BY a.isDefault DESC, a.lastUsedAt DESC")
    List<UserAddress> findAllByUserId(Long userId);
}
