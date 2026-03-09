package cloud.pposiraegi.ecommerce.domain.product.controller;

import cloud.pposiraegi.ecommerce.domain.product.dto.TimeDealDto;
import cloud.pposiraegi.ecommerce.domain.product.enums.TimeDealStatus;
import cloud.pposiraegi.ecommerce.domain.product.service.TimeDealService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/time-deals")
@RequiredArgsConstructor
public class TimeDealController {
    private final TimeDealService timeDealService;

    @GetMapping
    public ApiResponse<List<TimeDealDto.TimeDealResponse>> getPublicTimeDeals(@RequestParam(required = false) TimeDealStatus status) {
        return ApiResponse.success(timeDealService.getPublicTimeDeals(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<TimeDealDto.TimeDealDetailResponse> getTimeDeal(@PathVariable Long id) {
        return ApiResponse.success(timeDealService.getTimeDeal(id));
    }
}
