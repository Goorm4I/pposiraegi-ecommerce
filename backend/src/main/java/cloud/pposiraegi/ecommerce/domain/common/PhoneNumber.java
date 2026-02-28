package cloud.pposiraegi.ecommerce.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class PhoneNumber {
    public static final String REGEX = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$";

    @Column(name = "phone_number", nullable = false)
    private String value;

    public PhoneNumber(String value) {
        Assert.hasText(value, "전화번호는 필수입니다.");
        if (!value.matches(REGEX)) {
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다.");
        }
        this.value = value;
    }
}
