package cloud.pposiraegi.ecommerce.domain.product.entity;

import cloud.pposiraegi.ecommerce.domain.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "average_rating", precision = 2, scale = 1, nullable = false)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "brand_name", length = 50)
    private String brandName;

    @Column(name = "origin_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal originPrice;

    @Column(name = "sale_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal salePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.PREPARING;

    @Builder
    public Product(Long id, Long categoryId, String name, String description, String brandName, BigDecimal originPrice, BigDecimal salePrice, ProductStatus status) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.brandName = brandName;
        this.originPrice = originPrice;
        this.salePrice = salePrice != null ? salePrice : originPrice;
        this.status = status != null ? status : ProductStatus.PREPARING;
    }
}