package cloud.pposiraegi.ecommerce.domain.product.controller;

import cloud.pposiraegi.ecommerce.domain.product.dto.CategoryDto;
import cloud.pposiraegi.ecommerce.domain.product.service.CategoryService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    // TODO: 관리자 전용 기능
    @PostMapping
    public ApiResponse<?> createCategory(@RequestBody CategoryDto.CategoryCreateRequest request) {
        return ApiResponse.success(categoryService.createCategory(request));
    }

    @GetMapping
    public ApiResponse<List<CategoryDto.CategoryResponse>> getCategoryTree() {
        return ApiResponse.success(categoryService.getCategoryTrees());
    }
}
