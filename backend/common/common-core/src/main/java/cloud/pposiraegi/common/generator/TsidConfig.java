package cloud.pposiraegi.common.generator;

import com.github.f4b6a3.tsid.TsidFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

@Configuration
public class TsidConfig {
    @Value("{NODE_ID:0.0.0.0}")
    private String podIP;

    @Bean
    public TsidFactory tsidFactory() {
        int nodeId;
        try {
            InetAddress ip = InetAddress.getByName(podIP);
            byte[] bytes = ip.getAddress();
            nodeId = ((bytes[2] & 0x03) << 8) | (bytes[3] & 0xFF);
        } catch (Exception e) {
            nodeId = 0;
        }
        return TsidFactory.builder()
                .withNode(nodeId)
                .build();
    }

}
