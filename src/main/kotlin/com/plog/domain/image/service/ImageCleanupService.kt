package com.plog.domain.image.service

import com.plog.domain.image.repository.ImageRepository
import com.plog.global.minio.storage.ObjectStorage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 사용되지 않는 고아 이미지(Orphan Image)를 정리하는 서비스입니다.
 *
 * 게시글 썸네일이나 본문에 포함되지 않은 이미지 데이터를
 * 주기적으로 탐지하여 MinIO 스토리지와 DB에서 삭제합니다.
 * 업로드 후 24시간이 지나지 않은 이미지는 작성 중인 게시글을 보호하기 위해 제외합니다.
 */
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
        // 현재 시각 기준 24시간 이전을 기준점으로 설정
        val threshold = LocalDateTime.now().minusHours(24)

        val orphanImages = imageRepository.findOrphanImages(threshold)

        if (orphanImages.isEmpty()) {
            log.info("[ImageCleanupService] 삭제할 고아 이미지가 없습니다.")
            return
        }

        log.info("[ImageCleanupService] 총 ${orphanImages.size}개의 고아 이미지를 삭제합니다.")

        var successCount = 0
        var failCount = 0

        for (image in orphanImages) {
            try {
                // 1. MinIO에서 물리적 파일 삭제
                objectStorage.delete(image.storedName)
                // 2. DB에서 메타데이터 삭제
                imageRepository.delete(image)
                successCount++
            } catch (e: Exception) {
                log.error("[ImageCleanupService] 고아 이미지 삭제 실패 - url: ${image.accessUrl}, cause: ${e.message}")
                failCount++
            }
        }

        log.info("[ImageCleanupService] 고아 이미지 정리 완료. 성공: $successCount, 실패: $failCount")
    }
}
