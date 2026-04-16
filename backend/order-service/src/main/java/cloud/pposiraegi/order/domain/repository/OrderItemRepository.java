package cloud.pposiraegi.order.domain.repository;

import cloud.pposiraegi.order.domain.entity.OrderItem;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi JOIN Order o ON oi.orderId = o.id " +
            "WHERE o.userId = :userId AND oi.timeDealId = :timeDealId AND o.status != 'CANCELED'")
    Integer sumTimeDealPurchaseQuantity(@Param("userId") Long userId, @Param("timeDealId") Long timeDealId);
}
