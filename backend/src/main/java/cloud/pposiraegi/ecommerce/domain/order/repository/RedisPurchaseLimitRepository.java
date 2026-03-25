package cloud.pposiraegi.ecommerce.domain.order.repository;

import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisNoScriptException;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Repository;

import java.util.Collections;

@Repository
public class RedisPurchaseLimitRepository {
    private static final String LIMIT_KEY_PREFIX = "limit:timedeal:%d:user:%d";

    private final RedissonClient redissonClient;
    private final RScript script;

    private String checkAndIncreaseSha;
    private String decreaseSha;

    private static final String CHECK_AND_INCREASE_SCRIPT = """
            local count_key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local purchase_qty = tonumber(ARGV[2])
            local ttl_seconds = tonumber(ARGV[3])
            
            local current_count = redis.call('get', count_key)
            if current_count == false then
                current_count = 0
            else
                current_count = tonumber(current_count)
            end
            
            if (current_count + purchase_qty) > limit then
                return -1
            end
            
            redis.call('incrby', count_key, purchase_qty)
            
            if current_count == 0 and ttl_seconds > 0 then
                redis.call('expire', count_key, ttl_seconds)
            end
            
            return 1
            """;

    private static final String DECREASE_SCRIPT = """
            local count_key = KEYS[1]
            local cancel_qty = tonumber(ARGV[1])
            
            local current_count = redis.call('get', count_key)
            if current_count == false then
                return -1
            end
            
            current_count = tonumber(current_count)
            
            if (current_count - cancel_qty) < 0 then
                return -2
            end
            
            redis.call('decrby', count_key, cancel_qty)
            return 1
            """;

    public RedisPurchaseLimitRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.script = redissonClient.getScript(StringCodec.INSTANCE);
        loadScripts();
    }

    private void loadScripts() {
        this.checkAndIncreaseSha = script.scriptLoad(CHECK_AND_INCREASE_SCRIPT);
        this.decreaseSha = script.scriptLoad(DECREASE_SCRIPT);
    }


    public boolean checkAndIncreasePurchaseCount(Long timeDealId, Long userId, int limit, int requestQty, int ttlSeconds) {
        String key = String.format(LIMIT_KEY_PREFIX, timeDealId, userId);

        try {
            return executeCheckAndIncrease(key, limit, requestQty, ttlSeconds);
        } catch (RedisNoScriptException e) {
            loadScripts();
            return executeCheckAndIncrease(key, limit, requestQty, ttlSeconds);
        }
    }

    private boolean executeCheckAndIncrease(String key, int limit, int requestQty, int ttlSeconds) {
        Long result = script.evalSha(
                RScript.Mode.READ_WRITE,
                checkAndIncreaseSha,
                RScript.ReturnType.LONG,
                Collections.singletonList(key),
                String.valueOf(limit),
                String.valueOf(requestQty),
                String.valueOf(ttlSeconds)
        );
        return result != null && result == 1L;
    }

    public void decreasePurchaseCount(Long timeDealId, Long userId, int requestQty) {
        String key = String.format(LIMIT_KEY_PREFIX, timeDealId, userId);

        try {
            executeDecrease(key, requestQty);
        } catch (RedisNoScriptException e) {
            loadScripts();
            executeDecrease(key, requestQty);
        }
    }

    private void executeDecrease(String key, int requestQty) {
        Long result = script.evalSha(
                RScript.Mode.READ_WRITE,
                decreaseSha,
                RScript.ReturnType.LONG,
                Collections.singletonList(key),
                String.valueOf(requestQty)
        );

        if (result != null) {
            if (result == -1L) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            if (result == -2L) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }
}