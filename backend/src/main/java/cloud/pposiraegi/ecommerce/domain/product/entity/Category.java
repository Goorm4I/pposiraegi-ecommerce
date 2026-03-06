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

@Getter
@Entity
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseUpdatedEntity {
    @Id
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer depth;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder
    public Category(Long id, Long parentId, String name, Integer depth, Integer displayOrder) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.depth = depth;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }
}
