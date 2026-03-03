package cloud.pposiraegi.ecommerce.domain.user.user.controller;

import cloud.pposiraegi.ecommerce.domain.user.user.dto.UserAddressDto;
import cloud.pposiraegi.ecommerce.domain.user.user.service.UserAddressService;
import cloud.pposiraegi.ecommerce.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {
    private final UserAddressService userAddressService;

    @PostMapping
    public ApiResponse<?> registerAddress(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UserAddressDto.Request request) {
        userAddressService.registerAddress(Long.parseLong(userId), request);

        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<UserAddressDto.Response>> getAllAddresses(
            @AuthenticationPrincipal String userId) {
        List<UserAddressDto.Response> addresses = userAddressService.getAllAddresses(Long.parseLong(userId));

        return ApiResponse.success(addresses);
    }

    @PutMapping("{addressId}")
    public ApiResponse<?> updateAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressDto.Request request) {
        userAddressService.updateAddress(Long.parseLong(userId), addressId, request);

        return ApiResponse.success(null);
    }

    @DeleteMapping("{addressId}")
    public ApiResponse<?> deleteAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable Long addressId) {
        userAddressService.deleteAddress(Long.parseLong(userId), addressId);

        return ApiResponse.success(null);
    }

}
