package cloud.pposiraegi.ecommerce.domain.order.service;

import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutFacade {
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderService orderService;
    private final TsidFactory tsidFactory;


}
