package cloud.pposiraegi.ecommerce.domain.product.repository;

import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    List<ProductSku> findByProductId(Long productId);
}
