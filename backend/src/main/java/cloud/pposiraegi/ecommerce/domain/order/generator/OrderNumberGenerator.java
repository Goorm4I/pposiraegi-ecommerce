package cloud.pposiraegi.ecommerce.domain.order.generator;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {
    private final RedissonClient redissonClient;
    private static final String REDIS_ORDER_NUMBER_KEY_PREFIX = "order:number:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String redisKey = REDIS_ORDER_NUMBER_KEY_PREFIX + today;

        RAtomicLong sequence = redissonClient.getAtomicLong(redisKey);

        if (!sequence.isExists()) {
            sequence.expire(java.time.Duration.ofDays(1));
        }

        Long nextVal = sequence.incrementAndGet();
        return String.format("%s-%08d", today, nextVal);
    }
}
