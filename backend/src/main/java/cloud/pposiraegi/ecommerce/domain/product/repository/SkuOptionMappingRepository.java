package cloud.pposiraegi.ecommerce.domain.product.repository;

import cloud.pposiraegi.ecommerce.domain.product.entity.SkuOptionMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuOptionMappingRepository extends JpaRepository<SkuOptionMapping, Long> {
}
