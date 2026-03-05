package cloud.pposiraegi.ecommerce.domain.product.repository;

import cloud.pposiraegi.ecommerce.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
    List<ProductOption> findByProductId(Long productId);
}
