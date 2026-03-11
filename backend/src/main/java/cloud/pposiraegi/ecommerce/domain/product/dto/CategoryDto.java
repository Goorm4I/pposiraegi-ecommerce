package cloud.pposiraegi.ecommerce.domain.product.dto;

import cloud.pposiraegi.ecommerce.domain.product.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class CategoryDto {
    public record CategoryCreateRequest(
            Long parentId,
            @NotBlank @Size(min = 2, max = 50) String name,
            Integer displayOrder
    ) {
    }

    public record CategoryCreateResponse(
            Long id
    ) {
        public static CategoryCreateResponse from(Category category) {
            return new CategoryCreateResponse(category.getId());
        }
    }

    public record CategoryResponse(
            Long id,
            Long parentId,
            String name,
            Integer depth,
            Integer displayOrder,
            List<CategoryResponse> children
    ) {
        public static CategoryResponse from(Category category) {
            return new CategoryResponse(
                    category.getId(),
                    category.getParentId(),
                    category.getName(),
                    category.getDepth(),
                    category.getDisplayOrder(),
                    new ArrayList<>()
            );
        }
    }
}
