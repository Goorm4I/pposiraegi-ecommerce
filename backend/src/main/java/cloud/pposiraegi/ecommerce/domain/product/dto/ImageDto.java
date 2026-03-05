package cloud.pposiraegi.ecommerce.domain.product.dto;

import cloud.pposiraegi.ecommerce.domain.product.enums.ImageType;

public class ImageDto {
    public record ImageResponse(
            String imageUrl,
            ImageType imageType,
            Integer displayOrder
    ) {
    }
}
