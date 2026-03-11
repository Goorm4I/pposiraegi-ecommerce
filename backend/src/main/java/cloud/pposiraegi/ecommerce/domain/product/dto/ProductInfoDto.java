package cloud.pposiraegi.ecommerce.domain.product.dto;

import cloud.pposiraegi.ecommerce.domain.product.entity.Product;
import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;

import java.math.BigDecimal;

public class ProductInfoDto {
    public record ProductAndSkuInfo(
            Long skuId,
            Long productId,
            String productName,
            String thumbnailUrl,
            BigDecimal originUnitPrice,
            BigDecimal saleUnitPrice,
            Integer stockQuantity,
            String combinationKey
    ) {
        public static ProductAndSkuInfo from(Product product, ProductSku sku) {
            return new ProductAndSkuInfo(
                    sku.getId(),
                    product.getId(),
                    product.getName(),
                    product.getThumbnailUrl(),
                    product.getOriginPrice().add(sku.getAdditionalPrice()),
                    product.getSalePrice().add(sku.getAdditionalPrice()),
                    sku.getStockQuantity(),
                    sku.getCombinationKey()
            );
        }
    }
}
