package com.plog.domain.member.dto;


/**
 * 로그인과 토큰 재발급 시 서비스에서 컨트롤러에게 전달하는 데이터입니다.
 * <p>
 * accessToken은 헤더 설정에, refreshToken은 쿠키 설정에 사용됩니다.
 *
 * @param nickname 인증된 사용자의 닉네임
 * @param accessToken 이후 요청에 사용될 Bearer 인증 토큰
 * @param refreshToken 쿠키에 새로 저장될 토큰
 */
public record AuthLoginResult(
        Long id,
        String nickname,
        String accessToken,
        String refreshToken
) {

    public static AuthLoginResultBuilder builder() {
        return new AuthLoginResultBuilder();
    }

    public static class AuthLoginResultBuilder {
        private Long id;
        private String nickname;
        private String accessToken;
        private String refreshToken;

        AuthLoginResultBuilder() {
        }

        public AuthLoginResultBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuthLoginResultBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public AuthLoginResultBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public AuthLoginResultBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public AuthLoginResult build() {
            return new AuthLoginResult(id, nickname, accessToken, refreshToken);
        }
    }
}