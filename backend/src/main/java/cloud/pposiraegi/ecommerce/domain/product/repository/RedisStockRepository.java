package cloud.pposiraegi.ecommerce.domain.product.repository;

import io.lettuce.core.RedisNoScriptException;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.Collections;

@Repository
public class RedisStockRepository {
    public static final Long OUT_OF_STOCK_CODE = 1L;
    
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
                redis.call('decrby', KEYS[1], amount)
                return current - amount
            end
            return -1
            """;

    private static final String INCREASE_SCRIPT = """
            local stock = redis.call('get', KEYS[1])
            if stock == false then return nil end
            return redis.call('incrby', KEYS[1], tonumber(ARGV[1]))
            """;

    public RedisStockRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.script = redissonClient.getScript();
        loadScripts();
    }

    private void loadScripts() {
        this.decreaseSha = script.scriptLoad(DECREASE_SCRIPT);
        this.increaseSha = script.scriptLoad(INCREASE_SCRIPT);
    }


    public void setStock(String key, int quantity) {
        redissonClient.getAtomicLong(key).set(quantity);
    }

    public Long decreaseAtomic(String key, int quantity) {
        try {
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    decreaseSha,
                    RScript.ReturnType.LONG,
                    Collections.singletonList(key),
                    String.valueOf(quantity)
            );
        } catch (org.redisson.client.RedisNoScriptException e) {
            loadScripts();
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    decreaseSha,
                    RScript.ReturnType.LONG,
                    Collections.singletonList(key),
                    String.valueOf(quantity)
            );
        }
    }

    public Long increaseAtomic(String key, int quantity) {
        try {
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    increaseSha,
                    RScript.ReturnType.LONG,
                    Collections.singletonList(key),
                    String.valueOf(quantity)
            );
        } catch (RedisNoScriptException e) {
            loadScripts();
            return script.evalSha(
                    RScript.Mode.READ_WRITE,
                    increaseSha,
                    RScript.ReturnType.LONG,
                    Collections.singletonList(key),
                    String.valueOf(quantity)
            );
        }
    }
}
