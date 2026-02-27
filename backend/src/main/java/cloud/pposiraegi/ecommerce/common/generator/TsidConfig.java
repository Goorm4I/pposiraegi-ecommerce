package cloud.pposiraegi.ecommerce.common.generator;

import com.github.f4b6a3.tsid.TsidFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TsidConfig {
    @Bean
    public TsidFactory tsidFactory() {
        int nodeId = 0;
        return TsidFactory.builder()
                .withNode(nodeId)
                .build();
    }

}
