package com.plog.domain.post.service

import com.plog.domain.post.repository.PostRepository
import com.plog.domain.post.repository.ViewCountRedisRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ViewCountSyncServiceTest {

    @InjectMocks
    private lateinit var viewCountSyncService: ViewCountSyncService

    @Mock
    private lateinit var viewCountSyncTask: ViewCountSyncTask

    @Mock
    private lateinit var viewCountRedisRepository: ViewCountRedisRepository

    @Test
    @DisplayName("DB 동기화 시 Redis의 Pending 목록을 Chunk 단위로 Task에 전달한다")
    fun syncViewCountsToDbInChunks() {
        // [Given]
        val pendingPostIds = (1..150).map { it.toString() }.toSet()
        `when`(viewCountRedisRepository.getPendingPostIds()).thenReturn(pendingPostIds)

        // [When]
        viewCountSyncService.syncViewCountsToDb()

        // [Then]
        // 100개씩 Chunking 하므로 2번 호출되어야 함 (100개, 50개)
        verify(viewCountSyncTask, times(2)).processChunkWithRetry(anyList())
    }
}

@ExtendWith(MockitoExtension::class)
class ViewCountSyncTaskTest {

    @InjectMocks
    private lateinit var viewCountSyncTask: ViewCountSyncTask

    @Mock
    private lateinit var viewCountRedisRepository: ViewCountRedisRepository

    @Mock
    private lateinit var postRepository: PostRepository

    @Test
    @DisplayName("Task 실행 시 개별 포스트의 조회수를 DB에 반영하고 Redis 상태를 갱신한다")
    fun processChunk() {
        // [Given]
        val chunk = listOf("1", "2")
        `when`(viewCountRedisRepository.getCount(1L)).thenReturn(10)
        `when`(viewCountRedisRepository.getCount(2L)).thenReturn(5)

        // [When]
        viewCountSyncTask.processChunkWithRetry(chunk)

        // [Then]
        verify(postRepository).updateViewCount(1L, 10L)
        verify(postRepository).updateViewCount(2L, 5L)
        verify(viewCountRedisRepository).decrementCount(1L, 10L)
        verify(viewCountRedisRepository).decrementCount(2L, 5L)
        verify(viewCountRedisRepository).removeFromPending(1L)
        verify(viewCountRedisRepository).removeFromPending(2L)
    }

    @Test
    @DisplayName("복구(Recover) 시 실패한 포스트들을 DLQ로 이동시킨다")
    fun recoverFailedPosts() {
        // [Given]
        val chunk = listOf("1", "2")
        val exception = RuntimeException("Sync error")

        // [When]
        viewCountSyncTask.recover(exception, chunk)

        // [Then]
        verify(viewCountRedisRepository).addToDlq(1L)
        verify(viewCountRedisRepository).addToDlq(2L)
        verify(viewCountRedisRepository).removeFromPending(1L)
        verify(viewCountRedisRepository).removeFromPending(2L)
    }
}
