package cloud.pposiraegi.ecommerce.global.common.generator;

import com.github.f4b6a3.tsid.TsidFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TsidIdentifierGenerator implements IdentifierGenerator {
    private static final Logger log = LoggerFactory.getLogger(TsidIdentifierGenerator.class);
    private static final TsidFactory customTsidFactory;

    static {
        int nodeId = 0;
        customTsidFactory = TsidFactory.builder()
                .withNode(nodeId)
                .build();
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object obj) {
        return customTsidFactory.create().toLong();
    }
}
