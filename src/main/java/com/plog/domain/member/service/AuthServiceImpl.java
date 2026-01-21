package com.plog.domain.member.service;


import com.plog.domain.member.entity.Member;
import com.plog.domain.member.repository.MemberRepository;
import com.plog.global.exception.errorCode.AuthErrorCode;
import com.plog.global.exception.exceptions.AuthException;
import com.plog.global.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 코드에 대한 전체적인 역할을 적습니다.
 * <p>
 * 코드에 대한 작동 원리 등을 적습니다.
 *
 * <p><b>상속 정보:</b><br>
 * 상속 정보를 적습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code ExampleClass(String example)}  <br>
 * 주요 생성자와 그 매개변수에 대한 설명을 적습니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * 필요 시 빈 관리에 대한 내용을 적습니다.
 *
 * <p><b>외부 모듈:</b><br>
 * 필요 시 외부 모듈에 대한 내용을 적습니다.
 *
 * @author minhee
 * @see
 * @since 2026-01-15
 */

// TODO: service 바로 쓰지 말고 implement로 만들기

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public String genAccessToken(Member member) {
        return jwtUtils.createAccessToken(Map.of(
                "id", member.getId(),
                "email", member.getEmail(),
                "nickname", member.getNickname()
        ));
    }

    @Override
    public String genRefreshToken(Member member) {
        return jwtUtils.createRefreshToken(member.getId());
    }

    @Override
    @Transactional
    public Long signUp(String email, String password, String nickname) {
        memberRepository.findByEmail(email)
                .ifPresent(_member -> {
                            throw new AuthException(AuthErrorCode.USER_ALREADY_EXIST,
                                    "[AuthServiceImpl#signUp] user already exists.",
                                    "이미 가입 완료된 이메일입니다."
                                    );
                });

        password = passwordEncoder.encode(password);
        Member member = Member.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();
        return memberRepository.save(member).getId();
    }

    @Override
    public Member signIn(String email, String password) {
        Member member = findByEmail(email);
        checkPassword(member, password);
        return member;
    }

    private Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));
    }

    private void checkPassword(Member member, String password) {
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }
}