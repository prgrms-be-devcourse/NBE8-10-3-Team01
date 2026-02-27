package com.plog.domain.post.repository

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 게시물 조회수 관련 데이터를 Redis에서 관리하는 저장소 클래스입니다.
 *
 * 이 클래스는 RedisTemplate을 직접 사용하는 대신 추상화된 메서드를 제공하여
 * 서비스 계층과 데이터 저장소 간의 결합도를 낮춥니다.
 */
@Repository
class ViewCountRedisRepository(private val redisTemplate: RedisTemplate<String, Any>) {

    companion object {
        private const val LIMIT_KEY_PREFIX = "post:view:limit:"
        private const val COUNT_KEY_PREFIX = "post:view:count:"
        private const val PENDING_POSTS_KEY = "post:view:pending_posts"
        private const val SYNC_FAILED_KEY = "post:view:sync_failed"
    }

    /**
     * 특정 사용자의 게시물 조회 제한 여부를 확인하고, 첫 조회인 경우 기록합니다.
     *
     * @param postId 게시물 ID
     * @param userId 사용자 식별자 (ID 또는 IP)
     * @param ttl 만료 시간 (단위: 초)
     * @return 첫 조회인 경우 true, 이미 조회한 경우 false
     */
    fun setIfAbsentWithTtl(postId: Long, userId: String, ttl: Long): Boolean {
        val key = "$LIMIT_KEY_PREFIX$postId:user:$userId"
        val now = LocalDateTime.now().toString()
        return redisTemplate.opsForValue().setIfAbsent(key, now, ttl, TimeUnit.SECONDS) ?: false
    }

    /**
     * Redis에 저장된 특정 게시물의 조회수를 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     */
    fun incrementCount(postId: Long) {
        val key = "$COUNT_KEY_PREFIX$postId"
        redisTemplate.opsForValue().increment(key)
    }

    /**
     * DB 동기화가 필요한 게시물 목록(Pending Set)에 게시물 ID를 추가합니다.
     *
     * @param postId 게시물 ID
     */
    fun addToPending(postId: Long) {
        redisTemplate.opsForSet().add(PENDING_POSTS_KEY, postId.toString())
    }

    /**
     * DB 동기화가 필요한 모든 게시물 ID 목록을 조회합니다.
     *
     * @return 게시물 ID 목록
     */
    fun getPendingPostIds(): Set<String> {
        val members = redisTemplate.opsForSet().members(PENDING_POSTS_KEY) ?: return emptySet()
        return members.map { it.toString() }.toSet()
    }

    /**
     * 특정 게시물의 누적 조회수를 원자적으로 조회하고 0으로 초기화합니다.
     * Lua Script를 사용하여 조회와 초기화 사이의 데이터 불일치를 방지합니다.
     *
     * @param postId 게시물 ID
     * @return 누적 조회수
     */
    fun getAndResetCount(postId: Long): Long {
        val key = "$COUNT_KEY_PREFIX$postId"
        val script = """
            local count = redis.call('GET', KEYS[1])
            if count then
                redis.call('SET', KEYS[1], '0')
                return tonumber(count)
            else
                return 0
            end
        """.trimIndent()
        
        return redisTemplate.execute(
            DefaultRedisScript(script, Long::class.java),
            listOf(key)
        ) ?: 0L
    }

    /**
     * DB 동기화 대상 목록에서 여러 게시물 ID를 한 번에 제거합니다.
     *
     * @param postIds 게시물 ID 목록
     */
    fun removeAllFromPending(postIds: List<Long>) {
        if (postIds.isEmpty()) return
        val postIdStrings = postIds.map { it.toString() }.toTypedArray()
        redisTemplate.opsForSet().remove(PENDING_POSTS_KEY, *postIdStrings)
    }

    /**
     * DB 동기화 대상 목록에서 특정 게시물 ID를 제거합니다.
     *
     * @param postId 게시물 ID
     */
    fun removeFromPending(postId: Long) {
        redisTemplate.opsForSet().remove(PENDING_POSTS_KEY, postId.toString())
    }
}
