package com.plog.domain.image.service

import com.plog.domain.image.repository.ImageRepository
import com.plog.global.minio.storage.ObjectStorage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ImageCleanupService(
    private val imageRepository: ImageRepository,
    private val objectStorage: ObjectStorage
) {
    private val log = LoggerFactory.getLogger(ImageCleanupService::class.java)

    /**
     * 고아 이미지를 조회하여 스토리지와 DB에서 모두 삭제합니다.
     * 업로드 후 24시간 이내의 이미지는 삭제 대상에서 제외합니다.
     */
    @Transactional
    fun cleanupOrphanImages() {
        val threshold = LocalDateTime.now().minusHours(24)

        // ID만 조회
        val orphanIds = imageRepository.findPendingOrphanIds(threshold)
        if (orphanIds.isEmpty()) {
            log.info("[ImageCleanupService] 삭제할 고아 이미지가 없습니다.")
            return
        }

        log.info("[ImageCleanupService] 총 ${orphanIds.size}개의 고아 이미지를 삭제합니다.")

        var successCount = 0
        var failCount = 0

        // storedName 조회
        val storedNames = imageRepository.findStoredNamesByIds(orphanIds)

        // 3. 파일 삭제
        for (storedName in storedNames) {
            try {
                objectStorage.delete(storedName)
                successCount++
            } catch (e: Exception) {
                log.error("[ImageCleanupService] 파일 삭제 실패 - storedName: $storedName")
                failCount++
            }
        }

        // DB 배치 삭제
        imageRepository.deleteAllByIdInBatch(orphanIds)

        log.info("[ImageCleanupService] 완료. 파일 성공: $successCount/$storedNames.size, DB: ${orphanIds.size}")
    }
}