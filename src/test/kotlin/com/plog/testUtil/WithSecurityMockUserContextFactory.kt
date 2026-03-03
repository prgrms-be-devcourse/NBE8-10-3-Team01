package com.plog.testUtil

import com.plog.global.security.SecurityUser
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.test.context.support.WithSecurityContextFactory

/**
 * 테스트 환경에서 [WithCustomMockUser] 어노테이션을 통해
 * Spring Security [SecurityContext]를 구성하는 Factory 클래스입니다.
 */
class WithSecurityMockUserContextFactory : WithSecurityContextFactory<WithCustomMockUser> {
    override fun createSecurityContext(annotation: WithCustomMockUser): SecurityContext {
        val id = annotation.userId
        val email = annotation.email
        val nickname = annotation.nickname

        val authorities = AuthorityUtils.NO_AUTHORITIES

        val user = SecurityUser.securityUserBuilder()
            .id(id)
            .email(email)
            .password("test-password")
            .nickname(nickname)
            .authorities(authorities)
            .build()

        val auth = UsernamePasswordAuthenticationToken(user, "test-password", user.authorities)

        val context: SecurityContext = SecurityContextImpl()
        context.authentication = auth
        return context
    }
}
