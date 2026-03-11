package cloud.pposiraegi.ecommerce.domain.order.generator;

import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class OrderNumberGenerator {
    @Getter
    private static final OrderNumberGenerator instance = new OrderNumberGenerator();

    private final AtomicLong counter;
    private volatile String currentDateStr;

    private OrderNumberGenerator() {
        this.counter = new AtomicLong(0);
        this.currentDateStr = getCurrentDateStr();
    }

    public Long generate() {
        String today = getCurrentDateStr();

        if (!today.equals(this.currentDateStr)) {
            synchronized (this) {
                if (!today.equals(this.currentDateStr)) {
                    this.counter.set(0);
                    this.currentDateStr = today;
                }
            }
        }

        long sequence = counter.incrementAndGet();
        return Long.parseLong(String.format("%s%08d", this.currentDateStr, sequence));
    }

    private String getCurrentDateStr() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

}
