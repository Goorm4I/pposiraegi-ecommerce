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
@Table(name = "product_option_values")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOptionValue extends BaseUpdatedEntity {

    @Id
    private Long id;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(nullable = false, length = 50)
    private String value;

    @Column(name = "display_order")
    private Integer displayOrder = 1;

    @Builder
    public ProductOptionValue(Long id, Long optionId, String value, Integer displayOrder) {
        this.id = id;
        this.optionId = optionId;
        this.value = value;
        this.displayOrder = displayOrder != null ? displayOrder : 1;

    }
}