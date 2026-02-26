package com.plog.domain.post.service

import com.plog.domain.post.repository.PostRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.ValueOperations
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class ViewCountServiceTest {

    @InjectMocks
    private lateinit var viewCountService: ViewCountServiceImpl

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var valueOps: ValueOperations<String, Any>

    @Mock
    private lateinit var setOps: SetOperations<String, Any>

    @Test
    @DisplayName("첫 조회 시 Redis 카운트를 증가시키고 만료 시간을 설정한다")
    fun incrementViewCountFirstView() {
        val postId = 1L
        val userId = "user1"
        val limitKey = "post:view:limit:1:user:user1"
        val countKey = "post:view:count:1"
        val pendingKey = "post:view:pending_posts"

        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        `when`(redisTemplate.opsForSet()).thenReturn(setOps)
        `when`(valueOps.setIfAbsent(eq(limitKey), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true)

        viewCountService.incrementViewCount(postId, userId)

        verify(valueOps).increment(countKey)
        verify(setOps).add(pendingKey, postId.toString())
    }

    @Test
    @DisplayName("이미 조회한 사용자가 다시 조회하면 Redis 카운트를 증가시키지 않는다")
    fun incrementViewCountAlreadyViewed() {
        val postId = 1L
        val userId = "user1"
        val limitKey = "post:view:limit:1:user:user1"

        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        `when`(valueOps.setIfAbsent(eq(limitKey), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false)

        viewCountService.incrementViewCount(postId, userId)

        verify(valueOps, never()).increment(anyString())
        verify(setOps, never()).add(anyString(), anyString())
    }

    @Test
    @DisplayName("동기화 시 Redis의 카운트를 DB에 반영하고 Redis 카운트를 차감한다")
    fun syncViewCountsToDb() {
        val postId = 1L
        val countKey = "post:view:count:1"
        val pendingKey = "post:view:pending_posts"
        val pendingPostIds = setOf("1")

        `when`(redisTemplate.opsForSet()).thenReturn(setOps)
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        `when`(setOps.members(pendingKey)).thenReturn(pendingPostIds)
        `when`(valueOps.get(countKey)).thenReturn(10)

        viewCountService.syncViewCountsToDb()

        verify(postRepository).updateViewCount(postId, 10L)
        verify(valueOps).decrement(countKey, 10L)
        verify(setOps).remove(pendingKey, "1")
    }
}
