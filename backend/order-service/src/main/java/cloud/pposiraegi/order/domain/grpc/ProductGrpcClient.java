package cloud.pposiraegi.order.domain.grpc;

import cloud.pposiraegi.common.exception.BusinessException;
import cloud.pposiraegi.common.exception.ErrorCode;
import cloud.pposiraegi.grpc.product.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductGrpcClient {

    private final ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productStub;

    public List<SkuInfo> getSkuInfos(List<Long> skuIds) {
        SkuInfoRequest request = SkuInfoRequest.newBuilder()
                .addAllSkuIds(skuIds)
                .build();

        SkuInfoResponse response = productStub.getSkuInfos(request);
        return response.getSkuInfosList();
    }

    public Integer getSkuPurchaseLimit(Long skuId) {
        PurchaseLimitRequest request = PurchaseLimitRequest.newBuilder()
                .setSkuId(skuId)
                .build();

        PurchaseLimitResponse response = productStub.getSkuPurchaseLimit(request);
        return response.getPurchaseLimit();
    }

    public void decreaseStocks(Map<Long, Integer> stockRequests) {
        List<StockItem> items = stockRequests.entrySet().stream()
                .map(e -> StockItem.newBuilder()
                        .setSkuId(e.getKey())
                        .setQuantity(e.getValue())
                        .build())
                .collect(Collectors.toList());

        DecreaseStockRequest request = DecreaseStockRequest.newBuilder()
                .addAllItems(items)
                .build();

        try {
            productStub.decreaseStocks(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.RESOURCE_EXHAUSTED) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}