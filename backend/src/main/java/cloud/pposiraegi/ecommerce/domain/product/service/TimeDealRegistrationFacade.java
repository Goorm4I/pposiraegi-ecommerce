package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.dto.TimeDealDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimeDealRegistrationFacade {
    private final ProductService productService;
    private final TimeDealService timeDealService;

    @Transactional
    public void createTimeDealWithProduct(TimeDealDto.CreateRequestWithProduct request) {
        Long productId = productService.createProduct(request.product());

        TimeDealDto.CreateRequest timeDealRequest = new TimeDealDto.CreateRequest(
                productId,
                request.dealQuantity(),
                request.startTime(),
                request.endTime()
        );

        timeDealService.createTimeDeal(timeDealRequest);
    }
}
