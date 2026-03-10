package cloud.pposiraegi.ecommerce.domain.product.repository;

import cloud.pposiraegi.ecommerce.domain.product.entity.TimeDeal;
import cloud.pposiraegi.ecommerce.domain.product.enums.TimeDealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeDealRepository extends JpaRepository<TimeDeal, Long> {
    List<TimeDeal> findByStatusAndStartTimeLessThanEqual(TimeDealStatus status, LocalDateTime time);

    List<TimeDeal> findByStatusAndEndTimeLessThanEqual(TimeDealStatus status, LocalDateTime time);

    List<TimeDeal> findByStatus(TimeDealStatus status);

    List<TimeDeal> findByStatusAndStartTimeBetween(TimeDealStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t, p FROM TimeDeal t JOIN Product p ON t.productId = p.id WHERE (:status IS NULL OR t.status = :status)")
    List<Object[]> findTimeDealsWithProducts(@Param("status") TimeDealStatus status);

    @Query("SELECT t, p FROM TimeDeal t JOIN Product p ON t.productId = p.id WHERE t.status != :excludeStatus")
    List<Object[]> findTimeDealsWithProductsExcludingStatus(@Param("excludeStatus") TimeDealStatus excludeStatus);
}
