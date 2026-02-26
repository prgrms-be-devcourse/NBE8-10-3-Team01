// src/test/kotlin/com/plog/global/security/CaffeineTokenStoreTest.kt
package com.plog.global.security

import com.github.benmanes.caffeine.cache.Caffeine
import com.plog.global.config.CacheConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.cache.caffeine.CaffeineCacheManager
import java.util.concurrent.TimeUnit

/**
 * [CaffeineTokenStore]의 핵심 기능을 검증하기 위한 단위 테스트 클래스입니다.
 *
 * 실제 스프링 컨텍스트를 로드하지 않고, Caffeine Cache 객체를 직접 구성하여
 * 로컬 메모리 상에서의 데이터 저장, 조회, 삭제 정합성을 빠르게 검증합니다.
 *
 * **작동 원리:**
 * [CaffeineCacheManager]를 수동으로 생성하고 테스트 대상인 `TokenStore`에 주입하여
 * 추상화된 캐시 인터페이스가 실제 Caffeine 라이브러리와 올바르게 상호작용하는지 확인합니다.
 *
 * **외부 모듈:**
 * - Caffeine Cache: 인메모리 캐시 엔진
 * - Spring Cache: 캐시 추상화 레이어
 *
 * @author minhee
 * @since 2026-01-27
 */
class CaffeineTokenStoreTest {

    private lateinit var tokenStore: CaffeineTokenStore
    private lateinit var cacheManager: CaffeineCacheManager

    @BeforeEach
    fun setUp() {
        // 실제 Caffeine 설정과 유사하게 CacheManager를 수동 생성합니다.
        cacheManager = CaffeineCacheManager(CacheConfig.CACHE_NAME)
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(7, TimeUnit.DAYS)
                .maximumSize(1000)
        )

        tokenStore = CaffeineTokenStore(cacheManager)
    }

    @Test
    @DisplayName("토큰 저장 및 조회 성공")
    fun saveAndGetToken_Success() {
        // given
        val email = "test@plog.com"
        val refreshToken = "sample-refresh-token"

        // when
        tokenStore.save(email, refreshToken)
        val savedToken = tokenStore.get(email)

        // then
        assertThat(savedToken).isEqualTo(refreshToken)
    }

    @Test
    @DisplayName("기존 토큰이 존재할 때 새로운 토큰을 저장하면 값이 갱신된다")
    fun updateToken_Success() {
        // given
        val email = "test@plog.com"
        val oldToken = "old-token"
        val newToken = "new-token"
        tokenStore.save(email, oldToken)

        // when
        tokenStore.save(email, newToken)
        val savedToken = tokenStore.get(email)

        // then
        assertThat(savedToken).isEqualTo(newToken)
        assertThat(savedToken).isNotEqualTo(oldToken)
    }

    @Test
    @DisplayName("토큰 삭제 후 조회하면 null을 반환한다")
    fun deleteToken_Success() {
        // given
        val email = "test@plog.com"
        tokenStore.save(email, "some-token")

        // when
        tokenStore.delete(email)
        val savedToken = tokenStore.get(email)

        // then
        assertThat(savedToken).isNull()
    }

    @Test
    @DisplayName("저장되지 않은 이메일로 조회하면 null을 반환한다")
    fun getNonExistentToken_ReturnsNull() {
        // when
        val savedToken = tokenStore.get("unknown@plog.com")

        // then
        assertThat(savedToken).isNull()
    }
}