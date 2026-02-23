package com.plog.global.security;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Spring Security의 인증 인터페이스를 구현한 사용자 정의 인증 객체입니다.
 * <p>
 * 시스템 내에서 인증된 사용자의 정보를 세션이나 SecurityContext에 보관하며,
 * 사용자의 식별자(id), 이메일, 닉네임 등의 기본 정보를 제공합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link User} 클래스를 상속받아 구현되었습니다.
 *
 * <p><b>주요 생성자:</b><br>
 * 빌더 패턴을 통해 생성되며, 부모 클래스인 User에 인증 정보를 전달합니다.
 *
 * <p><b>빈 관리:</b><br>
 * UserDetailsService 구현체에서 사용자 정보를 조회하여 인스턴스를 생성합니다.
 *
 * <p><b>외부 모듈:</b><br>
 * Spring Security Core 모듈을 활용합니다.
 *
 * @author minhee
 * @see org.springframework.security.core.userdetails.UserDetails
 * @since 2026-01-16
 */

public class SecurityUser extends User {
    private final Long id;
    private final String nickname;

    public SecurityUser(
            Long id,
            String email,
            String password,
            String nickname,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(email, password, authorities); // email이 부모의 username에 저장됨
        this.id = id;
        this.nickname = nickname;
    }

    public static SecurityUserBuilder securityUserBuilder() {
        return new SecurityUserBuilder();
    }

    public static class SecurityUserBuilder {
        private Long id;
        private String email;
        private String password;
        private String nickname;
        private Collection<? extends GrantedAuthority> authorities;

        SecurityUserBuilder() {
        }

        public SecurityUserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SecurityUserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public SecurityUserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public SecurityUserBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public SecurityUserBuilder authorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public SecurityUser build() {
            return new SecurityUser(id, email, password, nickname, authorities);
        }
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    /**
     * 부모 클래스(User)의 username 필드(이메일)를 반환하는 getter
     */
    public String getEmail() {
        return super.getUsername();
    }
}