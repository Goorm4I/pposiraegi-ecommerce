package cloud.pposiraegi.ecommerce.domain.order.repository;

import cloud.pposiraegi.ecommerce.domain.order.entity.Order;
import cloud.pposiraegi.ecommerce.domain.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByCheckoutIdAndStatus(Long checkoutId, OrderStatus status);
}
