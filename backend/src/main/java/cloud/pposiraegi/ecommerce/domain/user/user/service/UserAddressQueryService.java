package cloud.pposiraegi.ecommerce.domain.user.user.service;

import cloud.pposiraegi.ecommerce.domain.user.user.entity.UserAddressEntity;
import cloud.pposiraegi.ecommerce.domain.user.user.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAddressQueryService {
    private final UserAddressRepository userAddressRepository;

    public record AddressInfo(
            Long addressId,
            String recipientName,
            String phoneNumber,
            String secondaryPhoneNumber,
            String zipCode,
            String baseAddress,
            String detailAddress,
            String requestMessage
    ) {
        public static AddressInfo from(UserAddressEntity address) {
            return new AddressInfo(
                    address.getId(),
                    address.getRecipientName(),
                    address.getPhoneNumber().getValue(),
                    address.getSecondaryPhoneNumber() != null ? address.getSecondaryPhoneNumber().getValue() : null,
                    address.getZipCode(),
                    address.getBaseAddress(),
                    address.getDetailAddress(),
                    address.getRequestMessage()
            );
        }
    }

    public AddressInfo getDefaultAddress(Long userId) {
        return userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(AddressInfo::from) // 값이 있을 때만 실행됨
                .orElse(null);
    }
}
