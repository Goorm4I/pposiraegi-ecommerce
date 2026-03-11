package cloud.pposiraegi.ecommerce.domain.order.entity;

import cloud.pposiraegi.ecommerce.domain.common.PhoneNumber;
import cloud.pposiraegi.ecommerce.domain.order.enums.ShipmentStatus;
import cloud.pposiraegi.ecommerce.global.common.entity.BaseUpdatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Getter
@Entity
@Table(name = "shipments")
public class Shipments extends BaseUpdatedEntity {
    @Id
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false)
    private PhoneNumber receiverPhone;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Column(name = "base_address", nullable = false)
    private String baseAddress;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "request_message")
    private String requestMessage;

    @Column(name = "carrier_name")
    private String carrierName;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "status")
    private ShipmentStatus status;


    @Builder
    public Shipments(Long id, Long orderId, String receiverName, PhoneNumber receiverPhone, String zipCode, String baseAddress, String detailAddress, String requestMessage) {
        this.id = id;
        this.orderId = orderId;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
        this.requestMessage = requestMessage;
        this.status = ShipmentStatus.PENDING;
    }

    public void assignCarrier(String carrierName, String TrackingNumber) {
        this.carrierName = carrierName;
        this.trackingNumber = TrackingNumber;
        this.status = ShipmentStatus.SHIPPING;
    }

}
