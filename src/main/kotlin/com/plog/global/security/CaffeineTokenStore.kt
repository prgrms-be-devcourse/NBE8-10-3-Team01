// src/main/kotlin/com/plog/global/security/CaffeineTokenStore.kt
package com.plog.global.security

import com.plog.global.config.CacheConfig.CACHE_NAME
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

/**
 * [TokenStore]의 Caffeine Cache 기반 구현체입니다.
 *
 * [CacheManager]를 통해 관리되는 로컬 메모리 캐시에
 * 토큰 정보를 저장하며, 빠른 액세스 속도와 설정된 만료 정책에 따른 자동 관리를 제공합니다.
 *
 * **상속 정보:**
 * [TokenStore] 인터페이스를 구현합니다.
 *
 * **주요 생성자:**
 * `CaffeineTokenStore(CacheManager cacheManager)`
 * 스프링 컨테이너로부터 설정된 CacheManager를 주입받아 초기화합니다.
 *
 * **빈 관리:**
 * `@Component`로 등록되어 있으며, 인증 서비스 레이어에서 주입받아 사용됩니다.
 *
 * **외부 모듈:**
 * Spring Cache Abstraction을 사용합니다.
 *
 * @author minhee
 * @see com.plog.global.config.CacheConfig
 * @since 2026-01-27
 */
@Component
class CaffeineTokenStore(
    private val cacheManager: CacheManager
) : TokenStore {

    override fun save(email: String, refreshToken: String) {
        val refreshTokenCache = cacheManager.getCache(CACHE_NAME)
        refreshTokenCache?.put(email, refreshToken)
    }

    override fun get(email: String): String? {
        val refreshTokenCache = cacheManager.getCache(CACHE_NAME)
        return refreshTokenCache?.get(email, String::class.java)
    }

    override fun delete(email: String) {
        val refreshTokenCache = cacheManager.getCache(CACHE_NAME)
        refreshTokenCache?.evict(email)
    }
}