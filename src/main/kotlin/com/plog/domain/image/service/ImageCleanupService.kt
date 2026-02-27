package com.plog.domain.image.service

import com.plog.domain.image.repository.ImageRepository
import com.plog.global.minio.storage.ObjectStorage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.plog.global.util.TimeUtil
import org.springframework.retry.annotation.Retryable
import org.springframework.retry.annotation.Backoff

@Service
class ImageCleanupService(
    private val imageRepository: ImageRepository,
    private val objectStorage: ObjectStorage,
    private val timeUtil: TimeUtil
) {
    private val log = LoggerFactory.getLogger(ImageCleanupService::class.java)

    /**
     * 청크 단위 DB 삭제 + 재시도
     */
    @Transactional
    @Retryable(retryFor = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 1000, multiplier = 2.0))
    fun deleteOrphanChunk(chunkIds: List<Long>) {
        log.info("[ImageCleanupService] 청크 삭제 시작: {}개", chunkIds.size)

        // 1. 스토리지 삭제 (개별 실패 무시! 로그만)
        val storedNames = imageRepository.findStoredNamesByIds(chunkIds)
        var successCount = 0
        storedNames.forEach { storedName ->
            try {
                objectStorage.delete(storedName)
                successCount++
                log.info("스토리지 삭제 성공: {}", storedName)
            } catch (e: Exception) {
                log.error("스토리지 삭제 실패 (무시): {} - {}", storedName, e.message)  // throw 제거!
            }
        }

        // 2. DB 무조건 배치 삭제 (스토리지 실패와 무관)
        imageRepository.deleteAllByIdInBatch(chunkIds)
        log.info("[ImageCleanupService] 청크 완료: DB {}개, 스토리지 성공 {}개", chunkIds.size, successCount)
    }

        /**
     * 고아 이미지를 조회하여 스토리지와 DB에서 모두 삭제합니다.
     * 업로드 후 3일 이내의 이미지는 삭제 대상에서 제외합니다.
     */
        @Transactional(readOnly = true)
        fun cleanupOrphanImages() {
            val threshold = timeUtil.getNowKST().minusDays(3)
            val orphanIds = imageRepository.findPendingOrphanIds(threshold)

            if (orphanIds.isEmpty()) {
                log.info("[ImageCleanupService] 삭제할 고아 이미지가 없습니다.")
                return
            }

            log.info("[ImageCleanupService] 총 ${orphanIds.size}개 → 청크 처리 시작 (500개)")

            orphanIds.chunked(500).forEach { chunk ->
                deleteOrphanChunk(chunk)
            }

            log.info("[ImageCleanupService] 전체 청크 처리 완료")
        }
}