package cloud.pposiraegi.ecommerce.domain.user.user.enums;

public enum UserStatus {
    // 이메일 인증 전 (임시 계정)
    PENDING,
    // 정상 사용 가능
    ACTIVE,
    // 휴면 계정
    DORMANT,
    // 정지 계정
    SUSPENDED,
    // 탈퇴 계정
    WITHDRAWN,
    // 보안 위험 계정
    LOCKED
}
