package cloud.pposiraegi.ecommerce.domain.product.entity;

import cloud.pposiraegi.ecommerce.domain.product.enums.SkuStatus;
import cloud.pposiraegi.ecommerce.global.common.entity.BaseUpdatedEntity;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_skus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSku extends BaseUpdatedEntity {

    @Id
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", length = 100)
    private String skuCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkuStatus status;

    @Column(name = "additional_price", columnDefinition = "INT DEFAULT 0")
    private Integer additionalPrice;

    @Column(name = "stock_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer stockQuantity;

    @Column(name = "combination_key")
    private String combinationKey;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 논리 삭제(Soft Delete) 용도

    @Builder
    public ProductSku(Long id, Long productId, String skuCode, String combinationKey, SkuStatus status, Integer additionalPrice, Integer stockQuantity) {
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode;
        this.combinationKey = combinationKey;
        this.status = status != null ? status : SkuStatus.OUT_OF_STOCK;
        this.additionalPrice = additionalPrice != null ? additionalPrice : 0;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("보충할 재고는 0보다 커야합니다.");
        }
        this.stockQuantity += quantity;

        // 기존에 품절이었다면 숨김으로 변경
        if (this.status == SkuStatus.OUT_OF_STOCK) {
            this.status = SkuStatus.HIDDEN;
        }
    }

    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        this.stockQuantity = restStock;

        if (this.stockQuantity == 0) {
            this.status = SkuStatus.OUT_OF_STOCK;
        }
    }

    public void updateInfo(Integer additionalPrice, SkuStatus status) {
        if (additionalPrice != null) {
            this.additionalPrice = additionalPrice;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = SkuStatus.DISCONTINUED;
    }

}