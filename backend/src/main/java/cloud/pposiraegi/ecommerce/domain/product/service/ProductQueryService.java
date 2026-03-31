package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.dto.ProductInfoDto;
import cloud.pposiraegi.ecommerce.domain.product.entity.Product;
import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.enums.ProductStatus;
import cloud.pposiraegi.ecommerce.domain.product.enums.SkuStatus;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductRepository;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductSkuRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;

    public List<ProductInfoDto.ProductAndSkuInfo> getSkuInfos(List<Long> skuIds) {
        List<ProductSku> skus = productSkuRepository.findAllById(skuIds);

        if (skus.size() != skuIds.size()) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
        }

        Set<Long> productIds = skus.stream()
                .filter(sku -> SkuStatus.AVAILABLE.equals(sku.getStatus()))
                .map(ProductSku::getProductId)
                .collect(Collectors.toSet());

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .filter(product -> ProductStatus.FOR_SALE.equals(product.getStatus()))
                .collect(Collectors.toMap(Product::getId, product -> product));

        return skus.stream()
                .map(sku -> {
                    if (!SkuStatus.AVAILABLE.equals(sku.getStatus())) {
                        throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
                    }
                    Product product = productMap.get(sku.getProductId());
                    if (product == null) {
                        throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
                    }
                    return ProductInfoDto.ProductAndSkuInfo.from(product, sku);
                }).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "skuPurchaseLimit", key = "#skuId")
    public Integer getSkuPurchaseLimit(Long skuId) {
        return productSkuRepository.findById(skuId)
                .map(ProductSku::getPurchaseLimit)
                .orElse(0);
    }
}
