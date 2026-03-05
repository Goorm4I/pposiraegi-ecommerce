package cloud.pposiraegi.ecommerce.domain.product.dto;

import cloud.pposiraegi.ecommerce.domain.product.entity.Product;
import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.enums.ImageType;
import cloud.pposiraegi.ecommerce.domain.product.enums.ProductStatus;
import cloud.pposiraegi.ecommerce.domain.product.enums.SkuStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ProductDto {
    public record ProductCreateRequest(
            @NotBlank Long categoryId,
            @NotBlank String name,
            String description,
            String brandName,
            @NotBlank @Min(0) Integer originPrice,
            @Min(0) Integer salePrice,
            ProductStatus status,

            List<ImageRequest> images,
            List<OptionGroupRequest> optionGroups,
            List<SkuRequest> skus
    ) {
    }

    public record ImageRequest(
            @NotBlank String imageUrl,
            ImageType imageType,
            Integer displayOrder
    ) {
    }

    public record OptionGroupRequest(
            @NotBlank String optionName,
            List<String> optionsValues
    ) {
    }

    public record SkuRequest(
            String skuCode,
            SkuStatus status,
            @Min(0) Integer additionalPrice,
            @Min(0) Integer stockQuantity,
            List<String> selectedOptionValues
    ) {
    }

    public record ProductResponse(
            Long id,
            String name,
            String description,
            String brandName,
            Integer originPrice,
            Integer salePrice,
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
                    product.getOriginPrice(),
                    product.getSalePrice(),
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
            Long skuId,
            String skuCode,
            Integer additionalPrice,
            Integer stockQuantity,
            String status,
            List<Long> optionValueIds
    ) {
        public static SkuResponse from(ProductSku sku, List<Long> optionValueIds) {
            return new SkuResponse(
                    sku.getId(),
                    sku.getSkuCode(),
                    sku.getAdditionalPrice(),
                    sku.getStockQuantity(),
                    sku.getStatus().name(),
                    optionValueIds
            );
        }
    }
}
