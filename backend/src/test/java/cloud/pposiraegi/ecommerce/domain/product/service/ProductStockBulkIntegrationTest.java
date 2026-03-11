package cloud.pposiraegi.ecommerce.domain.product.service;

import cloud.pposiraegi.ecommerce.domain.product.entity.ProductSku;
import cloud.pposiraegi.ecommerce.domain.product.enums.SkuStatus;
import cloud.pposiraegi.ecommerce.domain.product.repository.ProductSkuRepository;
import cloud.pposiraegi.ecommerce.domain.product.repository.RedisStockRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test") // 실제 Redis 설정을 사용하는 프로필
class ProductStockBulkIntegrationTest {

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisStockRepository redisStockRepository;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis와 DB 초기화
        redissonClient.getKeys().flushall();
        productSkuRepository.deleteAll();

        // 테스트 데이터 세팅 (SKU 101: 재고 10개, SKU 102: 재고 20개)
        setupSku(101L, 10);
        setupSku(102L, 20);
    }

    private void setupSku(Long id, int stock) {
        ProductSku sku = ProductSku.builder()
                .id(id)
                .productId(id)
                .skuCode("CODE-" + id)
                .status(SkuStatus.AVAILABLE)
                .stockQuantity(stock)
                .build();
        productSkuRepository.saveAndFlush(sku);
        redisStockRepository.setStock(id, stock);
    }

    @Test
    @DisplayName("실제 Redis 연동: 여러 상품의 재고를 한 번에 차감 성공해야 한다.")
    void bulkDecrease_Success() {
        // given
        Map<Long, Integer> request = new LinkedHashMap<>();
        request.put(101L, 5); // 10 -> 5
        request.put(102L, 5); // 20 -> 15

        // when
        productStockService.decreaseStocks(request);

        // then
        assertThat(getRedisStock(101L)).isEqualTo(5);
        assertThat(getRedisStock(102L)).isEqualTo(15);
    }

    @Test
    @DisplayName("실제 Redis 연동: 하나라도 재고가 부족하면 전체가 차감되지 않아야 한다(원자성).")
    void bulkDecrease_Fail_Atomic() {
        // given
        Map<Long, Integer> request = new LinkedHashMap<>();
        request.put(101L, 5);  // 재고 충분 (10개 중 5개)
        request.put(102L, 25); // 재고 부족 (20개 중 25개 요청)

        // when & then
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            productStockService.decreaseStocks(request);
        });

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK);

        // 🌟 중요: 원자성이 보장된다면 101L의 재고도 10개 그대로여야 함
        assertThat(getRedisStock(101L)).isEqualTo(10);
        assertThat(getRedisStock(102L)).isEqualTo(20);
    }

    @Test
    @DisplayName("실제 Redis 연동: Redis에 키가 없는 상품이 포함되면 DB에서 로드 후 성공해야 한다.")
    void bulkDecrease_WithCacheMiss() {
        // given: 102L의 Redis 키를 강제로 삭제
        redissonClient.getAtomicLong("stock:sku:102").delete();

        Map<Long, Integer> request = new LinkedHashMap<>();
        request.put(101L, 3);
        request.put(102L, 3);

        // when
        productStockService.decreaseStocks(request);

        // then: DB에서 20개를 읽어와서 3개를 깎았으므로 17개가 남아야 함
        assertThat(getRedisStock(101L)).isEqualTo(7);
        assertThat(getRedisStock(102L)).isEqualTo(17);
    }

    private long getRedisStock(Long skuId) {
        return redissonClient.getAtomicLong("stock:sku:" + skuId).get();
    }
}