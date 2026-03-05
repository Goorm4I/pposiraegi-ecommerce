package cloud.pposiraegi.ecommerce.domain.product.controller;

import cloud.pposiraegi.ecommerce.domain.product.dto.ProductDto;
import cloud.pposiraegi.ecommerce.domain.product.service.ProductService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<ProductDto.ProductResponse>> getProducts() {
        return ApiResponse.success(productService.getProducts());
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductDto.ProductDetailResponse> getProductDetail(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProductDetail(productId));
    }

}
