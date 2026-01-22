package com.plog.domain.member.service;

import com.plog.domain.member.dto.AuthLoginResult;
import com.plog.domain.member.dto.AuthSignInReq;
import com.plog.domain.member.dto.AuthSignUpReq;
import com.plog.domain.member.dto.MemberInfoRes;
import com.plog.domain.member.entity.Member;
import com.plog.domain.member.repository.MemberRepository;
import com.plog.global.exception.errorCode.AuthErrorCode;
import com.plog.global.exception.exceptions.AuthException;
import com.plog.global.security.JwtUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * {@link AuthServiceImpl} 에 대한 단위 테스트 입니다.
 *
 * @author minhee
 * @since 2026-01-21
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberService memberService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("회원가입 성공 - 이메일/닉네임 중복 없음, 비밀번호를 암호화, ID 반환")
    void signUp_success() {
        // given
        AuthSignUpReq req = new AuthSignUpReq("test@plog.com", "password123!", "plogger");
        String encodedPassword = "encoded_password";

        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(1L);

        given(memberService.isDuplicateEmail(req.email())).willReturn(false);
        given(memberService.isDuplicateNickname(req.nickname())).willReturn(false);
        given(passwordEncoder.encode(req.password())).willReturn(encodedPassword);
        given(memberRepository.save(any(Member.class))).willReturn(mockMember);

        // when
        Long savedId = authService.signUp(req);

        // then
        assertThat(savedId).isEqualTo(1L);
        then(memberService).should().isDuplicateEmail(req.email());
        then(passwordEncoder).should().encode(req.password());
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    void signUp_fail_alreadyExist() {
        // given
        String email = "exist@plog.com";
        AuthSignUpReq req = new AuthSignUpReq(email, "pw", "nick");
        given(memberService.isDuplicateEmail(email)).willReturn(true);

        // when
        AuthException ex = assertThrows(AuthException.class,
                () -> authService.signUp(req));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.USER_ALREADY_EXIST);
        assertThat(ex.getMessage()).isEqualTo("이미 가입된 이메일입니다.");
        then(memberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 사용 중인 닉네임")
    void signUp_fail_alreadyExistNickname() {
        // given
        String email = "test@plog.com";
        String nickname = "duplicateNick";
        AuthSignUpReq req = new AuthSignUpReq(email, "pw", nickname);

        given(memberService.isDuplicateEmail(email)).willReturn(false);
        given(memberService.isDuplicateNickname(nickname)).willReturn(true);

        // when
        AuthException ex = assertThrows(AuthException.class,
                () -> authService.signUp(req));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.USER_ALREADY_EXIST);
        assertThat(ex.getMessage()).isEqualTo("이미 사용 중인 닉네임입니다.");
        then(passwordEncoder).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("로그인 성공 - 회원 정보 반환")
    void signIn_success() {
        // given
        String email = "test@plog.com";
        String password = "password123!";
        String encodedPassword = "encoded_password";
        AuthSignInReq req = new AuthSignInReq(email, password);
        Member mockMember = Member.builder()
                .email(email)
                .nickname("plogger")
                .password(encodedPassword)
                .build();
        ReflectionTestUtils.setField(mockMember, "id", 1L);

        given(memberRepository.findByEmail(email)).willReturn(Optional.of(mockMember));
        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);
        given(jwtUtils.createAccessToken(anyMap())).willReturn("mock-access");
        given(jwtUtils.createRefreshToken(anyLong())).willReturn("mock-refresh");

        // when
        AuthLoginResult result = authService.signIn(req);

        // then
        assertThat(result.nickname()).isEqualTo("plogger");
        assertThat(result.accessToken()).isEqualTo("mock-access");
        assertThat(result.refreshToken()).isEqualTo("mock-refresh");
        then(passwordEncoder).should(times(1)).matches(password, encodedPassword);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치, INVALID_CREDENTIALS 예외 발생")
    void signIn_fail_invalidPassword() {
        // given
        String email = "test@plog.com";
        String password = "wrong_password";
        AuthSignInReq req = new AuthSignInReq(email, password);
        Member mockMember = Member.builder()
                .email(email)
                .password("encoded_password")
                .build();

        given(memberRepository.findByEmail(email)).willReturn(Optional.of(mockMember));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        // when
        AuthException ex = assertThrows(AuthException.class, () -> authService.signIn(req));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("AccessToken 생성 시 JwtUtils를 호출")
    void genAccessToken_success() {
        // given
        MemberInfoRes mockMember = mock(MemberInfoRes.class);
        given(mockMember.id()).willReturn(1L);
        given(mockMember.email()).willReturn("test@plog.com");
        given(mockMember.nickname()).willReturn("nick");

        given(jwtUtils.createAccessToken(anyMap())).willReturn("mock-token");

        // when
        String token = authService.genAccessToken(mockMember);

        // then
        assertThat(token).isEqualTo("mock-token");
        then(jwtUtils).should(times(1)).createAccessToken(anyMap());
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 리프레시 토큰이 유효하면 새 Access/Refresh Token 발급")
    void tokenReissue_success() {
        // given
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        String nickname = "nick";
        Long memberId = 1L;
        MemberInfoRes mockMember = mock(MemberInfoRes.class);
        Claims mockClaims = mock(Claims.class);

        given(jwtUtils.parseToken(refreshToken)).willReturn(mockClaims);
        given(mockClaims.get("id", Long.class)).willReturn(memberId);
        given(memberService.findMemberWithId(memberId)).willReturn(mockMember);

        // Mock 멤버 정보 설정
        given(mockMember.id()).willReturn(memberId);
        given(mockMember.email()).willReturn("test@plog.com");
        given(mockMember.nickname()).willReturn(nickname);

        given(jwtUtils.createAccessToken(anyMap())).willReturn(newAccessToken);
        given(jwtUtils.createRefreshToken(anyLong())).willReturn(newRefreshToken);

        // when
        AuthLoginResult result = authService.tokenReissue(refreshToken);

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        then(jwtUtils).should().createRefreshToken(anyLong());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 리프레시 토큰 만료 시 LOGIN_REQUIRED 예외 발생")
    void tokenReissue_fail_expired() {
        // given
        String expiredToken = "expired-token";
        given(jwtUtils.parseToken(expiredToken)).willThrow(
                new io.jsonwebtoken.ExpiredJwtException(null, null, "token expired")
        );

        // when
        AuthException ex = assertThrows(AuthException.class,
                () -> authService.tokenReissue(expiredToken));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.LOGIN_REQUIRED);
        assertThat(ex.getMessage()).isEqualTo("세션이 만료되었습니다. 다시 로그인해 주세요.");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰이 null이면 TOKEN_INVALID 예외 발생")
    void token() {
        // when
        AuthException ex = assertThrows(AuthException.class,
                () -> authService.tokenReissue(null));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.TOKEN_INVALID);
    }
}