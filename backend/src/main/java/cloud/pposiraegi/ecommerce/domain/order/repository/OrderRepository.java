package cloud.pposiraegi.ecommerce.domain.order.repository;

import cloud.pposiraegi.ecommerce.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
