package cloud.pposiraegi.ecommerce.domain.product.repository;

import org.redisson.api.RBatch;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisNoScriptException;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class RedisStockRepository {
    public static final Long OUT_OF_STOCK_CODE = -1L;
    public static final String STOCK_KEY_PREFIX = "stock:sku:";
    public static final String DIRTY_SKU_KEY = "stock:dirty:sku";

    private final RedissonClient redissonClient;
    private final RScript script;

    private String decreaseSha;
    private String increaseSha;

    private static final String DECREASE_SCRIPT = """
            local num_items = (#KEYS - 1)
            local dirty_key = KEYS[#KEYS]
            
            for i = 1, num_items do
                local stock = redis.call('get', KEYS[i])
                if stock == false then return {-2, i} end
                if (tonumber(stock) < tonumber(ARGV[i])) then return {-1, i} end
            end
            
            for i = 1, num_items do
                redis.call('decrby', KEYS[i], tonumber(ARGV[i]))
                redis.call('sadd', dirty_key, ARGV[num_items + i])
            end
            
            return {1, 0}
            """;

    private static final String INCREASE_SCRIPT = """
            local num_items = (#KEYS - 1)
            local dirty_key = KEYS[#KEYS]
            
            for i = 1, num_items do
                local stock = redis.call('get', KEYS[i])
                if stock == false then return {-2, i} end
            end
            
            for i = 1, num_items do
                redis.call('incrby', KEYS[i], tonumber(ARGV[i]))
                redis.call('sadd', dirty_key, ARGV[num_items + i])
            end
            
            return {1, 0}
            """;

    public RedisStockRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.script = redissonClient.getScript(StringCodec.INSTANCE);
        loadScripts();
    }

    private void loadScripts() {
        this.decreaseSha = script.scriptLoad(DECREASE_SCRIPT);
        this.increaseSha = script.scriptLoad(INCREASE_SCRIPT);
    }


    public void setStock(Long skuId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + skuId;
        redissonClient.getAtomicLong(stockKey).set(quantity);
    }

    public void setStockInBatch(Map<Long, Integer> stockQuantityMap) {
        RBatch batch = redissonClient.createBatch();

        stockQuantityMap.forEach((skuId, quantity) -> {
            String stockKey = STOCK_KEY_PREFIX + skuId;
            batch.getBucket(stockKey, StringCodec.INSTANCE).setIfAbsentAsync(String.valueOf(quantity));
        });

        batch.execute();
    }


    public List<Object> executeBulkAtomic(boolean isDecrease, List<Long> skuIds, List<Integer> quantities) {
        int size = skuIds.size();

        List<Object> keys = new ArrayList<>();
        for (Long skuId : skuIds) {
            keys.add(STOCK_KEY_PREFIX + skuId);
        }
        keys.add(DIRTY_SKU_KEY);

        Object[] args = new Object[size * 2];
        for (int i = 0; i < size; i++) {
            args[i] = String.valueOf(quantities.get(i));
            args[size + i] = String.valueOf(skuIds.get(i));
        }

        String targetSha = isDecrease ? decreaseSha : increaseSha;

        try {
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    targetSha,
                    RScript.ReturnType.LIST,
                    keys,
                    args
            );
        } catch (RedisNoScriptException e) {
            loadScripts();
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    targetSha,
                    RScript.ReturnType.LIST,
                    keys,
                    args
            );
        }
    }

    public boolean hasStockKey(Long skuId) {
        return redissonClient.getAtomicLong(STOCK_KEY_PREFIX + skuId).isExists();
    }
}
