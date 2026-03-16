package cloud.pposiraegi.ecommerce.domain.order.repository;

import cloud.pposiraegi.ecommerce.domain.order.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

}
