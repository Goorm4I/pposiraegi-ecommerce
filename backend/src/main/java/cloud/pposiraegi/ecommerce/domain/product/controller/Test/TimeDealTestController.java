package cloud.pposiraegi.ecommerce.domain.product.controller.Test;

import cloud.pposiraegi.ecommerce.domain.product.service.TimeDealService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test/time-deals")
@RequiredArgsConstructor
public class TimeDealTestController {

    private final TimeDealService timeDealService;

    @PostMapping("/{id}/purchase")
    public ApiResponse<String> testDeductStock(@PathVariable Long id) {
        timeDealService.decreaseStock(id, 1);
        return ApiResponse.success(null);
    }
}