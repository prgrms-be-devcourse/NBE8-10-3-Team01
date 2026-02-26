package com.plog.domain.post.service

import com.plog.domain.post.repository.PostRepository
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class ViewCountServiceImpl(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val postRepository: PostRepository
) : ViewCountService {

    companion object {
        private const val LIMIT_KEY_PREFIX = "post:view:limit:"
        private const val COUNT_KEY_PREFIX = "post:view:count:"
        private const val PENDING_POSTS_KEY = "post:view:pending_posts"
    }

    override fun incrementViewCount(postId: Long, userId: String) {
        val limitKey = "$LIMIT_KEY_PREFIX$postId:user:$userId"
        val now = LocalDateTime.now().toString()
        val durationUntil0400 = getDurationUntilNext0400()

        // Use atomic set-if-absent with expiration
        val isFirstView = redisTemplate.opsForValue().setIfAbsent(
            limitKey,
            now,
            durationUntil0400.toMillis(),
            TimeUnit.MILLISECONDS
        ) ?: false

        if (isFirstView) {
            val countKey = "$COUNT_KEY_PREFIX$postId"
            redisTemplate.opsForValue().increment(countKey)
            redisTemplate.opsForSet().add(PENDING_POSTS_KEY, postId.toString())
        }
    }

    @Transactional
    override fun syncViewCountsToDb() {
        val pendingPostIds = redisTemplate.opsForSet().members(PENDING_POSTS_KEY) ?: return
        if (pendingPostIds.isEmpty()) return

        for (postIdStr in pendingPostIds) {
            val postId = postIdStr.toString().toLong()
            
            // Remove from pending set first
            redisTemplate.opsForSet().remove(PENDING_POSTS_KEY, postIdStr)
            
            val countKey = "$COUNT_KEY_PREFIX$postId"
            val count = (redisTemplate.opsForValue().get(countKey) as? Number)?.toLong() ?: 0L

            if (count > 0) {
                postRepository.updateViewCount(postId, count)
                // Concurrency Safety: subtract the updated count from Redis
                redisTemplate.opsForValue().decrement(countKey, count)
            }
        }
    }

    private fun getDurationUntilNext0400(): Duration {
        val now = LocalDateTime.now()
        var target = now.withHour(4).withMinute(0).withSecond(0).withNano(0)
        if (now.isAfter(target)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target)
    }
}
