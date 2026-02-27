package com.plog.domain.post.service

import com.plog.domain.post.repository.PostRepository
import com.plog.domain.post.repository.ViewCountRedisRepository
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@ExtendWith(MockKExtension::class)
class ViewCountSyncServiceTest {

    @InjectMockKs
    private lateinit var viewCountSyncService: ViewCountSyncService

    @MockK
    private lateinit var viewCountSyncTask: ViewCountSyncTask

    @MockK
    private lateinit var viewCountRedisRepository: ViewCountRedisRepository

    @Test
    @DisplayName("DB 동기화 시 Redis의 Pending 목록을 Chunk 단위로 Task에 전달한다")
    fun syncViewCountsToDbInChunks() {
        // [Given]
        val pendingPostIds = (1..150).map { it.toString() }.toSet()
        every { viewCountRedisRepository.getPendingPostIds() } returns pendingPostIds
        every { viewCountSyncTask.processChunkWithRetry(any()) } just Runs

        // [When]
        viewCountSyncService.syncViewCountsToDb()

        // [Then]
        verify(exactly = 2) { viewCountSyncTask.processChunkWithRetry(any()) }
    }
}

@ExtendWith(MockKExtension::class)
class ViewCountSyncTaskTest {

    @InjectMockKs
    private lateinit var viewCountSyncTask: ViewCountSyncTask

    @MockK
    private lateinit var viewCountRedisRepository: ViewCountRedisRepository

    @MockK
    private lateinit var postRepository: PostRepository

    @BeforeEach
    fun setUp() {
        mockkStatic(TransactionSynchronizationManager::class)
    }

    @Test
    @DisplayName("Task 실행 시 개별 포스트의 조회수를 DB에 반영하고 Redis 상태를 갱신한다")
    fun processChunk() {
        // [Given]
        val chunk = listOf("1", "2")
        val longChunk = listOf(1L, 2L)
        
        every { viewCountRedisRepository.getAndResetCount(1L) } returns 10L
        every { viewCountRedisRepository.getAndResetCount(2L) } returns 5L
        every { postRepository.updateViewCount(any(), any()) } just Runs
        
        val syncSlot = slot<TransactionSynchronization>()
        every { TransactionSynchronizationManager.registerSynchronization(capture(syncSlot)) } just Runs
        every { viewCountRedisRepository.removeAllFromPending(longChunk) } just Runs

        // [When]
        viewCountSyncTask.processChunkWithRetry(chunk)

        // [Then]
        verify { viewCountRedisRepository.getAndResetCount(1L) }
        verify { viewCountRedisRepository.getAndResetCount(2L) }
        verify { postRepository.updateViewCount(1L, 10L) }
        verify { postRepository.updateViewCount(2L, 5L) }
        
        // Manual trigger of afterCommit to verify bulk removal
        syncSlot.captured.afterCommit()
        verify { viewCountRedisRepository.removeAllFromPending(longChunk) }
    }
}
