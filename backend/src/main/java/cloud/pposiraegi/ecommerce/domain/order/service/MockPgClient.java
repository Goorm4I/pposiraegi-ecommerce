package cloud.pposiraegi.ecommerce.domain.order.service;

import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockPgClient {

    @Value("${mock-pg.url}")
    private String pgUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * imp_uid로 PG 결제 결과 조회 및 검증
     * GET {pgUrl}/payments/{impUid}
     */
    public void verifyPayment(String impUid, BigDecimal expectedAmount) {
        String url = pgUrl + "/payments/" + impUid;

        try {
            String raw = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(raw);

            int code = root.path("code").asInt(-1);
            if (code != 0) {
                log.warn("PG 결제 조회 실패: impUid={}, response={}", impUid, raw);
                throw new BusinessException(ErrorCode.PG_VERIFICATION_FAILED);
            }

            JsonNode response = root.path("response");
            String status = response.path("status").asText();

            if (!"paid".equals(status)) {
                log.warn("PG 결제 미완료 상태: impUid={}, status={}", impUid, status);
                throw new BusinessException(ErrorCode.PG_VERIFICATION_FAILED);
            }

            BigDecimal pgAmount = response.path("amount").decimalValue();
            if (expectedAmount.compareTo(pgAmount) != 0) {
                log.error("PG 결제 금액 불일치: impUid={}, expected={}, actual={}", impUid, expectedAmount, pgAmount);
                throw new BusinessException(ErrorCode.PG_AMOUNT_MISMATCH);
            }

            log.info("PG 결제 검증 완료: impUid={}, amount={}", impUid, pgAmount);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("PG 서버 통신 오류: impUid={}", impUid, e);
            throw new BusinessException(ErrorCode.PG_VERIFICATION_FAILED);
        }
    }
}
