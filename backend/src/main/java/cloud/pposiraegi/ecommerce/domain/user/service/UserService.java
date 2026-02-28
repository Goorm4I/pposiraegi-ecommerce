package cloud.pposiraegi.ecommerce.domain.user.service;

import cloud.pposiraegi.ecommerce.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.common.exception.ErrorCode;
import cloud.pposiraegi.ecommerce.domain.common.PhoneNumber;
import cloud.pposiraegi.ecommerce.domain.user.dto.UserDto;
import cloud.pposiraegi.ecommerce.domain.user.entity.User;
import cloud.pposiraegi.ecommerce.domain.user.repository.UserRepository;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TsidFactory tsidFactory;


    @Transactional
    public UserDto.SimpleResponse registerUser(UserDto.RegisterRequest request) {
        // 1. ID(이메일) 중복 검증
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }

        // 2. 비밀번호 암호화, TSID 생성, PhoneNumber 객체 생성
        String encodedPassword = passwordEncoder.encode(request.password());
        Long tsid = tsidFactory.create().toLong();


        // 3. 엔티티 생성 및 저장
        User user = User.builder()
                .id(tsid)
                .email(request.email())
                .passwordHash(encodedPassword)
                .name(request.name())
                .nickname(applyNicknameMasking(request.nickname(), request.name()))
                .phoneNumber(new PhoneNumber(request.phoneNumber()))
                .build();

        User savedUser = userRepository.save(user);

        return UserDto.SimpleResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDto.SimpleResponse getUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserDto.SimpleResponse.from(user);
    }

    private String applyNicknameMasking(String nickname, String name) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }

        int length = name.length();

        if (length == 2) {
            return name.charAt(0) + "*";
        }

        return name.charAt(0) + "*" + name.charAt(length - 1);
    }
}
