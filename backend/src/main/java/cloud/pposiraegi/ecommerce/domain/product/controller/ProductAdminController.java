package cloud.pposiraegi.ecommerce.domain.product.controller;

import cloud.pposiraegi.ecommerce.domain.product.dto.ProductDto;
import cloud.pposiraegi.ecommerce.domain.product.service.ProductService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
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
    public ApiResponse<Void> createProduct(@RequestBody ProductDto.ProductCreateRequest request) {
        productService.createProduct(request);
        return ApiResponse.success(null);
    }

}
