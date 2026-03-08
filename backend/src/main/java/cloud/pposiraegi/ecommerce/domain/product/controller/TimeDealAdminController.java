package cloud.pposiraegi.ecommerce.domain.product.controller;

import cloud.pposiraegi.ecommerce.domain.product.dto.TimeDealDto;
import cloud.pposiraegi.ecommerce.domain.product.enums.TimeDealStatus;
import cloud.pposiraegi.ecommerce.domain.product.service.TimeDealRegistrationFacade;
import cloud.pposiraegi.ecommerce.domain.product.service.TimeDealService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/time-deals")
@RequiredArgsConstructor
public class TimeDealAdminController {
    private final TimeDealService timeDealService;
    private final TimeDealRegistrationFacade timeDealRegistrationFacade;

    @PostMapping
    public ApiResponse<?> createTimeDealForExistingProduct(@Valid @RequestBody TimeDealDto.CreateRequest request) {
        timeDealService.createTimeDeal(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/with-product")
    public ApiResponse<?> createTimeDealWithNewProduct(@Valid @RequestBody TimeDealDto.CreateRequestWithProduct request) {
        timeDealRegistrationFacade.createTimeDealWithProduct(request);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<?> getTimeDeals(@RequestParam(required = false) TimeDealStatus status) {
        return ApiResponse.success(timeDealService.getAdminTimeDeals(status));
    }
}
