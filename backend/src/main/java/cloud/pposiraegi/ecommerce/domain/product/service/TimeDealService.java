package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.dto.ProductDto;
import cloud.pposiraegi.ecommerce.domain.product.dto.TimeDealDto;
import cloud.pposiraegi.ecommerce.domain.product.entity.Product;
import cloud.pposiraegi.ecommerce.domain.product.entity.TimeDeal;
import cloud.pposiraegi.ecommerce.domain.product.enums.TimeDealStatus;
import cloud.pposiraegi.ecommerce.domain.product.repository.TimeDealRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeDealService {
    private final TimeDealRepository timeDealRepository;
    private final TsidFactory tsidFactory;
    private final ProductService productService;


    @Transactional
    public void createTimeDeal(TimeDealDto.CreateRequest request) {
        if (request.startTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_TIMEDEAL_START_TIME);
        }
        if (request.endTime().isBefore(request.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_TIMEDEAL_TIME_RANGE);
        }

        TimeDeal timeDeal = TimeDeal.builder()
                .id(tsidFactory.create().toLong())
                .productId(request.productId())
                .totalQuantity(request.dealQuantity())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();

        timeDealRepository.save(timeDeal);
    }

    @Transactional(readOnly = true)
    public List<TimeDealDto.Response> getAdminTimeDeals(TimeDealStatus status) {
        List<Object[]> results = timeDealRepository.findTimeDealsWithProducts(status);

        return results.stream().map(result -> {
            TimeDeal timeDeal = (TimeDeal) result[0];
            Product product = (Product) result[1];

            return TimeDealDto.Response.from(timeDeal, ProductDto.ProductResponse.from(product));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<TimeDealDto.Response> getPublicTimeDeals(TimeDealStatus status) {
        if (status == TimeDealStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Object[]> results;

        if (status == null) {
            results = timeDealRepository.findTimeDealsWithProductsExcludingStatus(TimeDealStatus.SUSPENDED);
        } else {
            results = timeDealRepository.findTimeDealsWithProducts(status);
        }

        return results.stream().map(result -> {
            TimeDeal timeDeal = (TimeDeal) result[0];
            Product product = (Product) result[1];

            return TimeDealDto.Response.from(timeDeal, ProductDto.ProductResponse.from(product));
        }).toList();
    }

    @Transactional
    public void decreaseStock(Long id) {
        TimeDeal timeDeal = timeDealRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIMEDEAL_NOT_FOUND));

        timeDeal.decreaseQuantity(1);
    }
}
