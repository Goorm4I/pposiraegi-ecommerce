package cloud.pposiraegi.ecommerce.domain.product.dto;

import cloud.pposiraegi.ecommerce.domain.product.entity.Product;
import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.enums.ImageType;
import cloud.pposiraegi.ecommerce.domain.product.enums.ProductStatus;
import cloud.pposiraegi.ecommerce.domain.product.enums.SkuStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ProductDto {
    public record ProductCreateRequest(
            @NotNull Long categoryId,
            @NotBlank String name,
            String description,
            String brandName,
            @NotNull @Min(0) BigDecimal originPrice,
            // 판매가격이 정가보다 높지 못하게 검증
            @Min(0) BigDecimal salePrice,
            @NotNull ProductStatus status,

            @Valid List<ImageRequest> images,
            @Valid List<OptionGroupRequest> optionGroups,
            @NotEmpty @Valid List<SkuRequest> skus
    ) {
    }

    public record ImageRequest(
            @NotBlank String imageUrl,
            @NotNull ImageType imageType,
            @NotNull Integer displayOrder
    ) {
    }

    public record OptionGroupRequest(
            @NotBlank String optionName,
            @NotEmpty List<String> optionsValues
    ) {
    }

    public record SkuRequest(
            String skuCode,
            @NotNull SkuStatus status,
            @Min(0) BigDecimal additionalPrice,
            @Min(0) Integer stockQuantity,
            @NotNull List<String> selectedOptionValues
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            String description,
            String brandName,
            String originPrice,
            String salePrice,
            String thumbnailUrl,
            String averageRating,
            Integer reviewCount,
            String status
            //sku
    ) {
        public static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getBrandName(),
                    product.getOriginPrice().toString(),
                    product.getSalePrice().toString(),
                    product.getThumbnailUrl(),
                    product.getAverageRating().toString(),
                    product.getReviewCount(),
                    product.getStatus().toString()
            );
        }
    }

    public record ProductDetailResponse(
            Long id,
            String name,
            String description,
            String brandName,
            List<ImageDto.ImageResponse> images,
            List<OptionGroupResponse> optionGroups,
            List<SkuResponse> skus
    ) {
        public static ProductDetailResponse from(Product product, List<ImageDto.ImageResponse> images, List<OptionGroupResponse> optionGroups, List<SkuResponse> skus) {
            return new ProductDetailResponse(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getBrandName(),
                    images,
                    optionGroups,
                    skus
            );
        }
    }

    public record OptionGroupResponse(
            Long optionId,
            String optionName,
            List<OptionValueResponse> optionValues
    ) {
    }

    public record OptionValueResponse(
            Long optionValueId,
            String value
    ) {
    }

    public record SkuResponse(
            String skuId,
            String skuCode,
            BigDecimal additionalPrice,
            Integer stockQuantity,
            String status,
            List<Long> optionValueIds
    ) {
        public static SkuResponse from(ProductSku sku, List<Long> optionValueIds) {
            return new SkuResponse(
                    sku.getId().toString(),
                    sku.getSkuCode(),
                    sku.getAdditionalPrice(),
                    sku.getStockQuantity(),
                    sku.getStatus().name(),
                    optionValueIds
            );
        }
    }
}
