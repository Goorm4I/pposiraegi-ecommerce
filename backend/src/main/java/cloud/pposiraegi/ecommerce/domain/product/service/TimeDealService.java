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
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeDealService {
    private final TimeDealRepository timeDealRepository;
    private final TsidFactory tsidFactory;
    private final ProductService productService;
    private final RedissonClient redissonClient;
    private final ProductStockService productStockService;


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
    public List<TimeDealDto.TimeDealResponse> getAdminTimeDeals(TimeDealStatus status) {
        List<Object[]> results = timeDealRepository.findTimeDealsWithProducts(status);

        return results.stream().map(result -> {
            TimeDeal timeDeal = (TimeDeal) result[0];
            Product product = (Product) result[1];

            return TimeDealDto.TimeDealResponse.from(timeDeal, ProductDto.ProductResponse.from(product));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<TimeDealDto.TimeDealResponse> getPublicTimeDeals(TimeDealStatus status) {
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

            return TimeDealDto.TimeDealResponse.from(timeDeal, ProductDto.ProductResponse.from(product));
        }).toList();
    }

    @Transactional(readOnly = true)
    public TimeDealDto.TimeDealDetailResponse getTimeDeal(Long id) {
        TimeDeal timeDeal = timeDealRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIMEDEAL_NOT_FOUND));

        if (TimeDealStatus.SUSPENDED.equals(timeDeal.getStatus())) {
            throw new BusinessException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        return TimeDealDto.TimeDealDetailResponse.from(timeDeal, productService.getProductDetail(timeDeal.getProductId()));
    }

    @Transactional
    public void updateTimeDealStatus() {
        LocalDateTime now = LocalDateTime.now();

        timeDealRepository.findByStatusAndStartTimeLessThanEqual(TimeDealStatus.PENDING, now)
                .forEach(TimeDeal::startTimeDeal);
        timeDealRepository.findByStatusAndEndTimeLessThanEqual(TimeDealStatus.ACTIVE, now)
                .forEach(TimeDeal::endTimeDeal);
    }

    @Transactional(readOnly = true)
    public void warmupUpcomingTimeDeals() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesLater = now.plusMinutes(5);

        List<TimeDeal> upcomingDeals = timeDealRepository.findByStatusAndStartTimeBetween(TimeDealStatus.PENDING, now, fiveMinutesLater);

        for (TimeDeal upcomingDeal : upcomingDeals) {
            String warmupFlagKey = "warmup:product:" + upcomingDeal.getProductId();
            if (redissonClient.getBucket(warmupFlagKey).isExists()) {
                continue;
            }

            try {
                productStockService.warmupProductStock(upcomingDeal.getProductId());
                redissonClient.getBucket(warmupFlagKey).set(true, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.error("Redis: 타임딜 워밍업 중 오류 발생 - Product ID: {}", upcomingDeal.getProductId(), e);
            }
        }
    }


    @Transactional
    public void decreaseStock(Long id, Integer amount) {
        TimeDeal timeDeal = timeDealRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIMEDEAL_NOT_FOUND));

        timeDeal.decreaseQuantity(amount);
    }


}
