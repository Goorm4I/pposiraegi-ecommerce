package cloud.pposiraegi.product.domain.grpc;

import cloud.pposiraegi.common.exception.BusinessException;
import cloud.pposiraegi.grpc.product.*;
import cloud.pposiraegi.product.domain.dto.ProductInfoDto;
import cloud.pposiraegi.product.domain.entity.ProductSku;
import cloud.pposiraegi.product.domain.repository.ProductSkuRepository;
import cloud.pposiraegi.product.domain.service.ProductQueryService;
import cloud.pposiraegi.product.domain.service.ProductStockService;
import cloud.pposiraegi.product.domain.service.TimeDealService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class ProductGrpcProvider extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase {

    private final ProductQueryService productQueryService;
    private final ProductStockService productStockService;
    private final ProductSkuRepository productSkuRepository;
    private final TimeDealService timeDealService;

    @Override
    public void getSkuInfos(SkuInfoRequest request, StreamObserver<SkuInfoResponse> responseObserver) {
        List<ProductInfoDto.ProductAndSkuInfo> skuInfos = productQueryService.getSkuInfos(request.getSkuIdsList());

        List<SkuInfo> grpcSkuInfos = skuInfos.stream()
                .map(info -> SkuInfo.newBuilder()
                        .setSkuId(info.skuId())
                        .setProductId(info.productId())
                        .setProductName(info.productName())
                        .setThumbnailUrl(info.thumbnailUrl() != null ? info.thumbnailUrl() : "")
                        .setOriginUnitPrice(info.originUnitPrice().toString())
                        .setSaleUnitPrice(info.saleUnitPrice().toString())
                        .setStockQuantity(info.stockQuantity())
                        .setCombinationKey(info.combinationKey() != null ? info.combinationKey() : "")
                        .build())
                .toList();

        SkuInfoResponse response = SkuInfoResponse.newBuilder()
                .addAllSkuInfos(grpcSkuInfos)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getSkuPurchaseLimit(PurchaseLimitRequest request, StreamObserver<PurchaseLimitResponse> responseObserver) {
        Integer limit = productQueryService.getSkuPurchaseLimit(request.getSkuId());

        PurchaseLimitResponse response = PurchaseLimitResponse.newBuilder()
                .setPurchaseLimit(limit)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void decreaseStocks(DecreaseStockRequest request, StreamObserver<DecreaseStockResponse> responseObserver) {
        Map<Long, Integer> stockMap = request.getItemsList().stream()
                .collect(Collectors.toMap(StockItem::getSkuId, StockItem::getQuantity));

        try {
            // Redis SKU 재고 차감
            productStockService.decreaseStocks(stockMap);

            // TimeDeal DB remainQuantity 차감 (skuId → productId → active TimeDeal)
            List<Long> skuIds = List.copyOf(stockMap.keySet());
            List<ProductSku> skus = productSkuRepository.findAllById(skuIds);
            for (ProductSku sku : skus) {
                timeDealService.decreaseStockByProductId(sku.getProductId(), stockMap.get(sku.getId()));
            }

            responseObserver.onNext(DecreaseStockResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (BusinessException e) {
            responseObserver.onError(Status.RESOURCE_EXHAUSTED
                    .withDescription(e.getErrorCode().getCode())
                    .asRuntimeException());
        }
    }
}