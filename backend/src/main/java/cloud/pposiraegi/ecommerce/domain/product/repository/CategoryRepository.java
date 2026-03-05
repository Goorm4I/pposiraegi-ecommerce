package cloud.pposiraegi.ecommerce.domain.product.repository;

import cloud.pposiraegi.ecommerce.domain.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    public Optional<Category> findByParentId(Long parentId);

    public Optional<Category> findById(Long id);

    List<Category> findAllByOrderByDisplayOrderAsc();
}
