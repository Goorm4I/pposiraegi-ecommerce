package cloud.pposiraegi.ecommerce.domain.user.user.service;

import cloud.pposiraegi.ecommerce.domain.common.PhoneNumber;
import cloud.pposiraegi.ecommerce.domain.user.user.dto.UserAddressDto;
import cloud.pposiraegi.ecommerce.domain.user.user.entity.UserAddressEntity;
import cloud.pposiraegi.ecommerce.domain.user.user.repository.UserAddressRepository;
import cloud.pposiraegi.ecommerce.global.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.global.common.exception.ErrorCode;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressService {
    private final UserAddressRepository userAddressRepository;
    private final TsidFactory tsidFactory;

    @Transactional
    public void registerAddress(Long userId, UserAddressDto.Request request) {
        long Tsid = tsidFactory.create().toLong();

        boolean isFirstAddress = userAddressRepository.countByUserId(userId) == 0;
        boolean isNewDefault = isFirstAddress || Boolean.TRUE.equals(request.isDefault());

        if (isNewDefault) {
            handleDefaultAddress(userId);
        }

        PhoneNumber secondaryPhone = StringUtils.hasText(request.secondaryPhoneNumber())
                ? new PhoneNumber(request.secondaryPhoneNumber()) : null;

        UserAddressEntity userAddress = UserAddressEntity.builder()
                .id(Tsid)
                .userId(userId)
                .recipientName(request.recipientName())
                .phoneNumber(new PhoneNumber(request.phoneNumber()))
                .secondaryPhoneNumber(secondaryPhone)
                .zipCode(request.zipCode())
                .baseAddress(request.baseAddress())
                .detailAddress(request.detailAddress())
                .requestMessage(request.requestMessage())
                .isDefault(isNewDefault)
                .build();

        userAddressRepository.save(userAddress);
    }

    public List<UserAddressDto.Response> getAllAddresses(Long userId) {
        List<UserAddressEntity> userAddresses = userAddressRepository.findAllByUserId(userId);

        return userAddresses.stream()
                .map(UserAddressDto.Response::from)
                .toList();
    }

    @Transactional
    public void updateAddress(Long userId, Long addressId, UserAddressDto.Request request) {
        UserAddressEntity userAddress = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        boolean isRequestDefault = Boolean.TRUE.equals(request.isDefault());

        if (userAddress.getIsDefault()) {
            isRequestDefault = true;
        } else if (isRequestDefault) {
            handleDefaultAddress(userId);
        }

        PhoneNumber secondaryPhone = StringUtils.hasText(request.secondaryPhoneNumber())
                ? new PhoneNumber(request.secondaryPhoneNumber()) : null;

        userAddress.UpdateAddress(
                request.recipientName(),
                new PhoneNumber(request.phoneNumber()),
                secondaryPhone,
                request.zipCode(),
                request.baseAddress(),
                request.detailAddress(),
                request.requestMessage(),
                isRequestDefault
        );

        userAddressRepository.save(userAddress);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddressEntity userAddress = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if (userAddress.getIsDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_ADDRESS_DELETE_NOT_ALLOWED);
        }

        userAddressRepository.delete(userAddress);
    }

    private void handleDefaultAddress(Long userId) {
        userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existingDefault -> existingDefault.changeDefault(false));
    }
}
