package cloud.pposiraegi.ecommerce.domain.product.repository;

import cloud.pposiraegi.ecommerce.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    public List<ProductImage> findByProductId(Long productId);

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);
}
