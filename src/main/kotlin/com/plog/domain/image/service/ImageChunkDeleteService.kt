package com.plog.domain.image.service

import com.plog.domain.image.repository.ImageRepository
import com.plog.global.minio.storage.ObjectStorage
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ImageChunkDeleteService(
    private val imageRepository: ImageRepository,
    private val objectStorage: ObjectStorage
) {
    private val log = LoggerFactory.getLogger(ImageChunkDeleteService::class.java)

    /**
     * 청크 단위 DB 삭제 + 재시도
     * 외부 빈으로 분리하여 Spring AOP 프록시를 통해 @Transactional과 @Retryable이 정상 동작합니다.
     */
    @Transactional
    @Retryable(retryFor = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 1000, multiplier = 2.0))
    fun deleteOrphanChunk(chunkIds: List<Long>) {
        log.info("[ImageChunkDeleteService] 청크 삭제 시작: {}개", chunkIds.size)

        val storedNames = imageRepository.findStoredNamesByIds(chunkIds)
        var successCount = 0
        storedNames.forEach { storedName ->
            try {
                objectStorage.delete(storedName)
                successCount++
                log.info("스토리지 삭제 성공: {}", storedName)
            } catch (e: Exception) {
                log.error("스토리지 삭제 실패 (무시): {} - {}", storedName, e.message)
            }
        }

        imageRepository.deleteAllByIdInBatch(chunkIds)
        log.info("[ImageChunkDeleteService] 청크 완료: DB {}개, 스토리지 성공 {}개", chunkIds.size, successCount)
    }
}
