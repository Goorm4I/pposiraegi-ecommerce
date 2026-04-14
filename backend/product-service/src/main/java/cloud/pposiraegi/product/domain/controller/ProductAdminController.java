package cloud.pposiraegi.product.domain.controller;

import cloud.pposiraegi.common.dto.ApiResponse;
import cloud.pposiraegi.product.domain.dto.ProductDto;
import cloud.pposiraegi.product.domain.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {
    private final ProductService productService;

    @PostMapping
    public ApiResponse<Void> createProduct(@Valid @RequestBody ProductDto.ProductCreateRequest request) {
        productService.createProduct(request);
        return ApiResponse.success(null);
    }
}
