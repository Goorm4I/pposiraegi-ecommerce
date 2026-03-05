package cloud.pposiraegi.ecommerce.domain.product.entity;

import cloud.pposiraegi.ecommerce.global.common.entity.BaseUpdatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption extends BaseUpdatedEntity {

    @Id
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Builder
    public ProductOption(Long id, Long productId, String name, Integer displayOrder) {
        this.id = id;
        this.productId = productId;
        this.name = name;
        this.displayOrder = displayOrder != null ? displayOrder : 1;

    }
}