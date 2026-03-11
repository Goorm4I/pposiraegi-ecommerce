package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductSkuRepository;
import cloud.pposiraegi.ecommerce.domain.product.repository.RedisStockRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductStockBulkServiceTest {

    @InjectMocks
    private ProductStockService productStockService;

    @Mock
    private RedisStockRepository redisStockRepository;

    @Mock
    private ProductSkuRepository productSkuRepository;

    @Mock
    private RedissonClient redissonClient;

    @Test
    @DisplayName("다건 재고 차감 성공 - 모든 상품의 재고가 충분할 때")
    void decreaseStocks_Success() {
        Map<Long, Integer> request = new LinkedHashMap<>();
        request.put(1L, 2);
        request.put(2L, 3);

        List<Object> mockSuccessResult = List.of(1L, 0L);

        when(redisStockRepository.executeBulkAtomic(eq(true), anyList(), anyList()))
                .thenReturn(mockSuccessResult);

        assertDoesNotThrow(() -> productStockService.decreaseStocks(request));

        verify(redisStockRepository, times(1)).executeBulkAtomic(eq(true), anyList(), anyList());
        verify(productSkuRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("다건 재고 차감 실패 - 일부 상품의 재고가 부족할 때 예외 발생")
    void decreaseStocks_OutOfStock_ThrowsException() {
        Map<Long, Integer> request = Map.of(1L, 2);

        // 🌟 수정: Lua 스크립트 1-based 인덱스를 고려하여 첫 번째 요소인 1L을 반환하도록 설정
        List<Object> mockFailResult = List.of(-1L, 1L);

        when(redisStockRepository.executeBulkAtomic(eq(true), anyList(), anyList()))
                .thenReturn(mockFailResult);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productStockService.decreaseStocks(request));

        assertEquals(ErrorCode.OUT_OF_STOCK, exception.getErrorCode());
    }

    @Test
    @DisplayName("다건 재고 차감 중 캐시 미스 발생 - DB 조회 후 재시도하여 성공")
    void decreaseStocks_CacheMiss_LoadsFromDBAndRetries() throws Exception {
        Long missingSkuId = 2L;
        Map<Long, Integer> request = Map.of(missingSkuId, 3);

        // 🌟 수정: Lua 스크립트 1-based 인덱스를 고려하여 첫 번째 요소인 1L을 반환하도록 설정
        List<Object> mockMissResult = List.of(-2L, 1L);
        List<Object> mockSuccessResult = List.of(1L, 0L);

        RLock mockLock = mock(RLock.class);
        when(redissonClient.getLock("lock:sku:" + missingSkuId)).thenReturn(mockLock);
        when(mockLock.tryLock(5, TimeUnit.SECONDS)).thenReturn(true);
        when(mockLock.isLocked()).thenReturn(true);
        when(mockLock.isHeldByCurrentThread()).thenReturn(true);

        when(redisStockRepository.executeBulkAtomic(eq(true), anyList(), anyList()))
                .thenReturn(mockMissResult)
                .thenReturn(mockSuccessResult);

        when(redisStockRepository.hasStockKey(missingSkuId)).thenReturn(false);
        ProductSku mockSku = mock(ProductSku.class);
        when(mockSku.getStockQuantity()).thenReturn(10);
        when(productSkuRepository.findById(missingSkuId)).thenReturn(Optional.of(mockSku));

        assertDoesNotThrow(() -> productStockService.decreaseStocks(request));

        verify(productSkuRepository, times(1)).findById(missingSkuId);
        verify(redisStockRepository, times(1)).setStock(missingSkuId, 10);
        verify(redisStockRepository, times(2)).executeBulkAtomic(eq(true), anyList(), anyList());
        verify(mockLock, times(1)).unlock();
    }

    @Test
    @DisplayName("다건 재고 차감 무시 - 빈 요청이나 null이 들어올 때")
    void decreaseStocks_EmptyOrNullRequest_DoesNothing() {
        assertDoesNotThrow(() -> productStockService.decreaseStocks(null));
        assertDoesNotThrow(() -> productStockService.decreaseStocks(Map.of()));

        verify(redisStockRepository, never()).executeBulkAtomic(anyBoolean(), anyList(), anyList());
    }
}