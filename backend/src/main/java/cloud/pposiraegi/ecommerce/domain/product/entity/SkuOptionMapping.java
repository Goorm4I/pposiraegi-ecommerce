package cloud.pposiraegi.ecommerce.domain.product.entity;

import cloud.pposiraegi.ecommerce.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "sku_option_mappings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SkuOptionMapping extends BaseCreatedEntity {

    @Id
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "option_value_id", nullable = false)
    private Long optionValueId;

    @Builder
    public SkuOptionMapping(Long id, Long skuId, Long optionValueId) {
        this.id = id;
        this.skuId = skuId;
        this.optionValueId = optionValueId;
    }
}