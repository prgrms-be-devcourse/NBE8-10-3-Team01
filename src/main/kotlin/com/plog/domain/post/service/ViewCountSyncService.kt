package com.plog.domain.post.service

import com.plog.domain.post.repository.PostRepository
import com.plog.domain.post.repository.ViewCountRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Redis에 축적된 조회수를 DB에 동기화하는 비즈니스 로직을 담당하는 서비스 클래스입니다.
 */
@Service
@EnableRetry
class ViewCountSyncService(
    private val viewCountRedisRepository: ViewCountRedisRepository,
    private val viewCountSyncTask: ViewCountSyncTask
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CHUNK_SIZE = 100
    }

    /**
     * Redis의 Pending Set에 등록된 모든 게시물의 조회수를 DB에 반영합니다.
     * Chunk 단위로 트랜잭션을 관리하여 성능을 최적화합니다.
     */
    fun syncViewCountsToDb() {
        val pendingPostIds = viewCountRedisRepository.getPendingPostIds()
        if (pendingPostIds.isEmpty()) return

        log.info("[ViewCountSyncService] Starting sync for {} posts", pendingPostIds.size)

        pendingPostIds.chunked(CHUNK_SIZE).forEach { chunk ->
            viewCountSyncTask.processChunkWithRetry(chunk)
        }

        log.info("[ViewCountSyncService] Sync completed")
    }
}

/**
 * 실제 동기화 작업을 수행하는 컴포넌트입니다.
 * Spring Retry와 Transactional 관리를 위해 별도 컴포넌트로 분리하였습니다.
 */
@Component
class ViewCountSyncTask(
    private val viewCountRedisRepository: ViewCountRedisRepository,
    private val postRepository: PostRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Chunk 단위로 DB 업데이트를 수행합니다. 
     * 실패 시 지수 백오프(Exponential Backoff)와 지터(Jitter)를 적용하여 재시도합니다.
     */
    @Transactional
    @Retryable(
        retryFor = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0, random = true)
    )
    fun processChunkWithRetry(postIds: List<String>) {
        log.debug("[ViewCountSyncTask] Processing chunk of size {}", postIds.size)
        for (postIdStr in postIds) {
            val postId = postIdStr.toLong()
            val count = viewCountRedisRepository.getCount(postId)
            if (count > 0) {
                postRepository.updateViewCount(postId, count)
                viewCountRedisRepository.decrementCount(postId, count)
            }
            viewCountRedisRepository.removeFromPending(postId)
        }
    }

    /**
     * 모든 재시도가 실패했을 때 호출되는 복구 메서드입니다.
     * 실패한 게시물 ID들을 Dead Letter Queue(DLQ)로 이동시켜 추후 검사가 가능하게 합니다.
     */
    @Recover
    fun recover(e: Exception, postIds: List<String>) {
        log.error("[ViewCountSyncTask] Sync failed after max retries. Moving {} posts to DLQ", postIds.size, e)
        postIds.forEach { postIdStr ->
            viewCountRedisRepository.addToDlq(postIdStr.toLong())
            viewCountRedisRepository.removeFromPending(postIdStr.toLong())
        }
    }
}
