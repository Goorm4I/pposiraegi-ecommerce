package cloud.pposiraegi.ecommerce.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseUpdatedEntity extends BaseCreatedEntity {
    @LastModifiedDate
    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;
}