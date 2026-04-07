package cloud.pposiraegi.ecommerce.domain.order.entity;

import cloud.pposiraegi.ecommerce.domain.order.enums.IdempotencyStatus;
import cloud.pposiraegi.ecommerce.global.common.entity.BaseUpdatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "idempotency_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord extends BaseUpdatedEntity {
    @Id
    private String id;

    @Column(name = "handler_name", nullable = false)
    private String handlerName;

    @Column(name = "request_hash")
    private String requestHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IdempotencyStatus status;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    public IdempotencyRecord(String id, String handlerName, String requestHash) {
        this.id = id;
        this.handlerName = handlerName;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.PENDING;
        this.expiredAt = LocalDateTime.now().plusDays(1);
    }

    public void updateResponse(IdempotencyStatus status, String responsePayload) {
        this.status = status;
        this.responsePayload = responsePayload;
    }
}