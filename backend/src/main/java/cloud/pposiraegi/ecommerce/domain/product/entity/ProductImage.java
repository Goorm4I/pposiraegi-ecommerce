package cloud.pposiraegi.ecommerce.domain.product.entity;

import cloud.pposiraegi.ecommerce.domain.product.enums.ImageType;
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
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseUpdatedEntity {

    @Id
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "image_type", length = 20)
    private ImageType imageType; // 예: MAIN, DETAIL

    @Column(name = "display_order", columnDefinition = "INT DEFAULT 0")
    private Integer displayOrder;

    @Builder
    public ProductImage(Long id, Long productId, String imageUrl, ImageType imageType, Integer displayOrder) {
        this.id = id;
        this.productId = productId;
        this.imageUrl = imageUrl;
        this.imageType = imageType;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }
}