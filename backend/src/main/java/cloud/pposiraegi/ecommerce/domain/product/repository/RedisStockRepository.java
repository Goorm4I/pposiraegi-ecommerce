package cloud.pposiraegi.ecommerce.domain.product.repository;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisNoScriptException;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

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
            local stock = redis.call('get', KEYS[1])
            if stock == false then return nil end
            local current = tonumber(stock)
            local amount = tonumber(ARGV[1])
            if current >= amount then
                local remain = redis.call('decrby', KEYS[1], amount)
                redis.call('sadd', KEYS[2], ARGV[2])
                return remain
            end
            return -1
            """;

    private static final String INCREASE_SCRIPT = """
            local stock = redis.call('get', KEYS[1])
            if stock == false then return nil end
            local current = redis.call('incrby', KEYS[1], tonumber(ARGV[1]))
            redis.call('sadd', KEYS[2], ARGV[2])
            return current
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

    public Long decreaseAtomic(Long skuId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + skuId;
        List<Object> keys = Arrays.asList(stockKey, DIRTY_SKU_KEY);
        try {
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    decreaseSha,
                    RScript.ReturnType.LONG,
                    keys,
                    String.valueOf(quantity), String.valueOf(skuId)
            );
        } catch (RedisNoScriptException e) {
            loadScripts();
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    decreaseSha,
                    RScript.ReturnType.LONG,
                    keys,
                    String.valueOf(quantity), String.valueOf(skuId)
            );
        }
    }

    public Long increaseAtomic(Long skuId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + skuId;
        List<Object> keys = Arrays.asList(stockKey, DIRTY_SKU_KEY);
        try {
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    increaseSha,
                    RScript.ReturnType.LONG,
                    keys,
                    String.valueOf(quantity), String.valueOf(skuId)
            );
        } catch (RedisNoScriptException e) {
            loadScripts();
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    increaseSha,
                    RScript.ReturnType.LONG,
                    keys,
                    String.valueOf(quantity), String.valueOf(skuId)
            );
        }
    }
}
