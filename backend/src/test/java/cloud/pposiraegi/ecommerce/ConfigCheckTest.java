package cloud.pposiraegi.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class ConfigCheckTest {
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Test
    void checkConfig() {
        assertThat(dbUrl).isNotBlank();
        assertThat(dbUsername).isNotBlank();
    }
}
