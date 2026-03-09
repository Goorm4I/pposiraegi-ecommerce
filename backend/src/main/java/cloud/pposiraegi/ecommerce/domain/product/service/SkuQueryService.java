package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.entity.Product;
import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.enums.ProductStatus;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductRepository;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductSkuRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkuQueryService {
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;

    public record SkuInfo(
            Long skuId,
            String optionCombination,
            String imageUrl,
            Integer stockQuantity,
            BigDecimal unitPrice
    ) {
        public static SkuInfo from(Product product, ProductSku sku) {
            return new SkuInfo(
                    sku.getId(),
                    sku.getCombinationKey(),
                    product.getThumbnailUrl(),
                    sku.getStockQuantity(),
                    product.getSalePrice().add(sku.getAdditionalPrice())
            );
        }
    }

    public SkuInfo getSkuDetailForOrder(Long skuId) {

        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));

        Product product = productRepository.findById(sku.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!ProductStatus.FOR_SALE.equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }

        return SkuInfo.from(product, sku);
    }
}
