package cloud.pposiraegi.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PPosiraegiTimeDealApplication {

    public static void main(String[] args) {
        SpringApplication.run(PPosiraegiTimeDealApplication.class, args);
    }

}
