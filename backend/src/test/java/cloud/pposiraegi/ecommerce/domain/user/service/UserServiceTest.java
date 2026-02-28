package cloud.pposiraegi.ecommerce.domain.user.service;

import cloud.pposiraegi.ecommerce.common.exception.BusinessException;
import cloud.pposiraegi.ecommerce.common.exception.ErrorCode;
import cloud.pposiraegi.ecommerce.domain.user.dto.UserDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest // 스프링 컨테이너를 실제로 띄워서 빈을 주입받음
@Transactional  // 테스트가 끝나면 DB를 자동으로 롤백함 (매우 중요!)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("정상적인 회원가입")
    void registerUserSuccess() {
        // 1. 회원가입 정보 입력(정상 정보)
        UserDto.RegisterRequest request = new UserDto.RegisterRequest(
                "test@test.com", "password123!", "홍길동", "길동이", "010-1234-5678"
        );

        // 2. 회원 등록 서비스
        UserDto.SimpleResponse response = userService.registerUser(request);

        // 3. 결과 확인
        assertThat(response.id()).isNotNull(); // TSID가 잘 생성되었는지
        assertThat(response.nickname()).isEqualTo(request.nickname());

    }

    @Test
    @DisplayName("중복이메일 회원가입")
    void registerUserWithDuplicateEmail() {
        // 1. 회원가입 정보 입력(정상)
        UserDto.RegisterRequest firstRequest = new UserDto.RegisterRequest(
                "test@test.com", "password123!", "홍길동", "길동이", "010-1234-5678"
        );
        // 1-2. 중복된 이메일로 가입 요청
        UserDto.RegisterRequest secondRequest = new UserDto.RegisterRequest(
                "test@test.com", "password456!", "홍길동2", "길동이2", "010-8765-4321"
        );

        // 2. 회원 등록 서비스
        UserDto.SimpleResponse firstResponse = userService.registerUser(firstRequest);

        em.flush();
        em.clear();

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.registerUser(secondRequest);
        });

        // 3. 결과 확인
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_DUPLICATION);
    }
}