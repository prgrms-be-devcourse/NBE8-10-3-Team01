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
     * SSCAN을 사용하여 대량의 데이터를 비차단(non-blocking) 방식으로 가져옵니다.
     *
     * @return 게시물 ID 목록
     */
    fun getPendingPostIds(): Set<String> {
        val postIds = mutableSetOf<String>()
        redisTemplate.opsForSet().scan(PENDING_POSTS_KEY, org.springframework.data.redis.core.ScanOptions.NONE).use { cursor ->
            while (cursor.hasNext()) {
                postIds.add(cursor.next().toString())
            }
        }
        return postIds
    }

    /**
     * 특정 게시물의 현재 누적 조회수를 조회합니다. (초기화하지 않음)
     *
     * @param postId 게시물 ID
     * @return 누적 조회수
     */
    fun getCount(postId: Long): Long {
        val key = "$COUNT_KEY_PREFIX$postId"
        val value = redisTemplate.opsForValue().get(key)
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    /**
     * Redis에 저장된 특정 게시물의 조회수를 특정 수치만큼 감소시킵니다.
     * DB 동기화 완료 후 호출되어 동기화된 만큼만 차감합니다.
     *
     * @param postId 게시물 ID
     * @param count 차감할 수치
     */
    fun decrementCount(postId: Long, count: Long) {
        if (count <= 0) return
        val key = "$COUNT_KEY_PREFIX$postId"
        redisTemplate.opsForValue().decrement(key, count)
    }

    /**
     * 여러 게시물의 조회수를 한 번에 차감하고, Pending Set에서도 한 번에 제거합니다.
     * Lua Script를 사용하여 단일 네트워크 라운드트립으로 처리합니다.
     *
     * @param counts 차감할 게시물 ID와 수치의 맵
     * @param allPostIds Pending Set에서 제거할 전체 게시물 ID 목록 (조회수가 0인 경우 포함)
     */
    fun decrementCountsAndRemoveFromPending(counts: Map<Long, Long>, allPostIds: List<Long>) {
        if (allPostIds.isEmpty()) return

        val script = """
            -- ARGV[1]: Pending Set Key
            -- ARGV[2]: Count Key Prefix
            -- ARGV[3...]: Pairs of [postId, decrementAmount] then [remaining postIds to remove from pending]
            
            local pendingSetKey = ARGV[1]
            local countKeyPrefix = ARGV[2]
            local pairCount = tonumber(ARGV[3])
            
            -- 1. Decrement counts
            for i = 1, pairCount do
                local postId = ARGV[3 + (i-1)*2 + 1]
                local amount = tonumber(ARGV[3 + (i-1)*2 + 2])
                local key = countKeyPrefix .. postId
                redis.call('DECRBY', key, amount)
            end
            
            -- 2. Remove all from pending set
            local startIdx = 3 + pairCount * 2 + 1
            for i = startIdx, #ARGV do
                redis.call('SREM', pendingSetKey, ARGV[i])
            end
            
            return true
        """.trimIndent()

        val args = mutableListOf<String>()
        args.add(PENDING_POSTS_KEY)
        args.add(COUNT_KEY_PREFIX)
        args.add(counts.size.toString())
        
        // Add pairs for decrement
        counts.forEach { (id, count) ->
            args.add(id.toString())
            args.add(count.toString())
        }
        
        // Add all IDs for removal from pending
        allPostIds.forEach { args.add(it.toString()) }

        redisTemplate.execute(
            DefaultRedisScript(script, Boolean::class.java),
            emptyList<String>(), // No KEYS used, all passed via ARGV to handle dynamic list
            *args.toTypedArray()
        )
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
