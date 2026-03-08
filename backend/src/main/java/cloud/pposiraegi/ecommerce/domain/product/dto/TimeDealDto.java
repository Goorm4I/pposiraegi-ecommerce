package cloud.pposiraegi.ecommerce.domain.product.dto;

import cloud.pposiraegi.ecommerce.domain.product.entity.TimeDeal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class TimeDealDto {
    public record CreateRequest(
            @NotNull Long productId,
            @NotNull @Min(1) Integer dealQuantity,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime
    ) {
    }

    public record CreateRequestWithProduct(
            @Valid @NotNull ProductDto.ProductCreateRequest product,
            @NotNull @Min(1) Integer dealQuantity,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime
    ) {
    }

    public record Response(
            String id,
            String productId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status,
            ProductDto.ProductResponse product
    ) {
        public static Response from(TimeDeal timeDeal, ProductDto.ProductResponse product) {
            return new Response(
                    timeDeal.getId().toString(),
                    timeDeal.getProductId().toString(),
                    timeDeal.getStartTime(),
                    timeDeal.getEndTime(),
                    timeDeal.getStatus().toString(),
                    product
            );
        }
    }
}
