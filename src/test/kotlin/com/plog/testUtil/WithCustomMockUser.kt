package com.plog.testUtil

import org.springframework.security.test.context.support.WithSecurityContext
import kotlin.annotation.Retention

/**
 * 추후 spring security 가 작성되면 테스트 중 유저 정보를 가져올 때 사용하는 어노테이션
 *
 * @author jack8
 * @since 2026-01-20
 */
@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithSecurityMockUserContextFactory::class)
annotation class WithCustomMockUser(
    val userId: Long = 1L,
    val email: String = "test@test.com",
    val nickname: String = "tester"
)
