package com.plog.domain.member.service;

import com.plog.domain.member.entity.Member;
import com.plog.domain.member.repository.MemberRepository;
import com.plog.global.exception.errorCode.AuthErrorCode;
import com.plog.global.exception.exceptions.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() { // @Value 값을 받을 수 없어 생성자를 직접 호출
        String testKey = "testSecretKeytestSecretKeytestSecretKey";
        long testExp = 3600L;
        authService = new AuthServiceImpl(testKey, testExp, memberRepository, passwordEncoder);
    }

    @Test
    @DisplayName("회원가입 요청 시 비밀번호를 암호화하여 저장하고 ID를 반환한다.")
    void signUp_success() {
        // given
        String email = "test@plog.com";
        String rawPassword = "password123!";
        String encodedPassword = "encoded_password";
        String nickname = "plogger";

        Member mockMember = mock(Member.class);
        given(mockMember.getId()).willReturn(1L);

        given(memberRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
        given(memberRepository.save(any(Member.class))).willReturn(mockMember);

        // when
        Long savedId = authService.signUp(email, rawPassword, nickname);

        // then
        assertThat(savedId).isEqualTo(1L);
        then(memberRepository).should(times(1)).findByEmail(email);
        then(passwordEncoder).should(times(1)).encode(rawPassword);
        then(memberRepository).should(times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 가입 시 USER_ALREADY_EXIST 예외가 발생한다.")
    void signUp_fail_alreadyExist() {
        // given
        String email = "exist@plog.com";
        given(memberRepository.findByEmail(email)).willReturn(Optional.of(mock(Member.class)));

        // when
        AuthException ex = assertThrows(AuthException.class,
                () -> authService.signUp(email, "pw", "nick"));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.USER_ALREADY_EXIST);
        then(memberRepository).should(times(1)).findByEmail(email);
        then(memberRepository).should(times(0)).save(any(Member.class));
    }
}