package com.plog.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 애플리케이션 전역에서 사용되는 공통 인프라 빈을 설정하는 클래스입니다.
 *
 * **설계 의도:**
 * 특정 도메인에 종속되지 않는 범용 빈을 중앙 관리합니다.
 * 설정 클래스 간의 복잡한 의존성 관계를 끊기 위해 공통 컴포넌트를 독립적인 설정 계층으로 분리합니다.
 * 향후 별도의 공통 Util 빈 등이 추가될 수 있습니다.
 *
 * **빈 관리:**
 * - [ObjectMapper]: JSON 데이터 파싱 및 Java 객체 매핑 담당
 *
 * @author minhee
 * @see com.plog.global.security.SecurityConfig
 * @since 2026-01-22
 */
@Configuration
class AppConfig {

    /**
     * 전역적으로 사용할 ObjectMapper 설정입니다.
     *
     * **주요 사용처:**
     * - SecurityConfig 내 예외 핸들러의 JSON 응답 생성
     */
    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
    }

    /**
     * 사용자의 비밀번호를 암호화하기 위한 PasswordEncoder를 Bean으로 등록합니다.
     *
     * @return BCrypt 알고리즘이 적용된 PasswordEncoder 객체
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
