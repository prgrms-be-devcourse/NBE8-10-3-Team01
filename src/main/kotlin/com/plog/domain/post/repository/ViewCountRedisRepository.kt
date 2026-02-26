package com.plog.domain.post.repository

import org.springframework.data.redis.core.RedisTemplate
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
     * 특정 게시물의 누적 조회수를 조회합니다.
     *
     * @param postId 게시물 ID
     * @return 누적 조회수
     */
    fun getCount(postId: Long): Long {
        val key = "$COUNT_KEY_PREFIX$postId"
        return (redisTemplate.opsForValue().get(key) as? Number)?.toLong() ?: 0L
    }

    /**
     * DB 반영이 완료된 조회수만큼 Redis 카운트에서 차감합니다.
     *
     * @param postId 게시물 ID
     * @param count 차감할 수치
     */
    fun decrementCount(postId: Long, count: Long) {
        val key = "$COUNT_KEY_PREFIX$postId"
        redisTemplate.opsForValue().decrement(key, count)
    }

    /**
     * DB 동기화 대상 목록에서 특정 게시물 ID를 제거합니다.
     *
     * @param postId 게시물 ID
     */
    fun removeFromPending(postId: Long) {
        redisTemplate.opsForSet().remove(PENDING_POSTS_KEY, postId.toString())
    }

    /**
     * 최종적으로 동기화에 실패한 게시물 ID를 Dead Letter Queue(DLQ)에 저장합니다.
     *
     * @param postId 게시물 ID
     */
    fun addToDlq(postId: Long) {
        redisTemplate.opsForSet().add(SYNC_FAILED_KEY, postId.toString())
    }
}
