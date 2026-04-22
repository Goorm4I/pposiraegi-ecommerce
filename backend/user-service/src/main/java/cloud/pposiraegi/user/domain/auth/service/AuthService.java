package cloud.pposiraegi.user.domain.auth.service;

import cloud.pposiraegi.common.exception.BusinessException;
import cloud.pposiraegi.common.exception.ErrorCode;
import cloud.pposiraegi.user.domain.auth.dto.AuthDto;
import cloud.pposiraegi.user.domain.auth.entity.UserRefreshToken;
import cloud.pposiraegi.user.domain.auth.repository.UserRefreshTokenRepository;
import cloud.pposiraegi.user.domain.user.entity.User;
import cloud.pposiraegi.user.domain.user.enums.UserStatus;
import cloud.pposiraegi.user.domain.user.repository.UserRepository;
import cloud.pposiraegi.user.global.auth.jwt.TokenProvider;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final TsidFactory tsidFactory;

    @Value("${jwt.access-token-validity-in-milliseconds}")
    private long accessTokenValidityTime;

    @Value("${jwt.refresh-token-validity-in-milliseconds}")
    private long refreshTokenValidityTime;

    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request, String ipAddress, String deviceInfo) {
        // 1. 유저 존재 여부 확인
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        // 2. 유저 상태 확인
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // TODO: 이메일 미인증 계정 체크

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime accessExpiry = now.plus(accessTokenValidityTime, ChronoUnit.MILLIS);
        LocalDateTime refreshExpiry = now.plus(refreshTokenValidityTime, ChronoUnit.MILLIS);

        // 4. 새 토큰 발급
        String accessToken = tokenProvider.createAccessToken(user.getId(), now, accessExpiry);
        String refreshToken = tokenProvider.createRefreshToken(user.getId(), now, refreshExpiry);

        // 5. 단일 접속 보장: 기존 토큰 덮어쓰기 or 새 토큰 저장 (Upsert)
        userRefreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        // 5-A. 기존 접속이 있다면 새 기기 정보와 토큰으로 덮어씁니다. (기존 접속자는 로그아웃됨)
                        existingToken -> {
                            existingToken.updateToken(refreshToken, ipAddress, deviceInfo, refreshExpiry);
                        },
                        // 5-B. 기존 접속이 없다면 완전히 새로 생성합니다.
                        () -> {
                            UserRefreshToken newToken = UserRefreshToken.builder()
                                    .id(tsidFactory.create().toLong()) // 신규 생성 시에만 TSID 발급
                                    .userId(user.getId())
                                    .tokenValue(refreshToken)
                                    .ipAddress(ipAddress)
                                    .deviceInfo(deviceInfo)
                                    .createdAt(now)
                                    .expiresAt(refreshExpiry)
                                    .build();
                            userRefreshTokenRepository.save(newToken);
                        }
                );

        // TODO: 마지막 로그인 시간(user_status_info 테이블) 변경

        return new AuthDto.LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(Long userId, AuthDto.LogoutRequest logoutRequest) {
        UserRefreshToken rtEntity = userRefreshTokenRepository.findByTokenValue(logoutRequest.refreshToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!rtEntity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.TOKEN_USER_MISMATCH);
        }

        userRefreshTokenRepository.delete(rtEntity);
    }

    @Transactional
    public AuthDto.RefreshResponse reissue(String refreshTokenValue) {
        if (!tokenProvider.validateToken(refreshTokenValue)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        UserRefreshToken rtEntity = userRefreshTokenRepository.findByTokenValue(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN));

        if (rtEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            userRefreshTokenRepository.delete(rtEntity);
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime accessExpiry = now.plus(accessTokenValidityTime, ChronoUnit.MILLIS);
        String accessToken = tokenProvider.createAccessToken(rtEntity.getUserId(), now, accessExpiry);

        return new AuthDto.RefreshResponse(accessToken);
    }
}
