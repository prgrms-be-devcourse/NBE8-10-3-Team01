package com.plog.domain.image.service

import com.plog.domain.image.repository.ImageRepository
import com.plog.domain.image.verifier.ImageUsageVerifier
import com.plog.global.minio.storage.ObjectStorage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.plog.global.util.TimeUtil

@Service
class ImageCleanupService(
    private val imageRepository: ImageRepository,
    private val objectStorage: ObjectStorage,
    private val timeUtil: TimeUtil,
    private val verifiers: List<ImageUsageVerifier>,
    private val imageChunkDeleteService: ImageChunkDeleteService
) {
    private val log = LoggerFactory.getLogger(ImageCleanupService::class.java)

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
        } else {
            log.info("[ImageCleanupService] 총 ${orphanIds.size}개 → 청크 처리 시작 (500개)")

            orphanIds.chunked(500).forEach { chunk ->
                imageChunkDeleteService.deleteOrphanChunk(chunk)
            }

            log.info("[ImageCleanupService] 전체 청크 처리 완료")
        }

        cleanupStaleUsedImages()
    }

    private fun cleanupStaleUsedImages() {
        val usedImages = imageRepository.findAllUsedImages()
        if (usedImages.isEmpty()) return

        val staleIds = usedImages
            .filter { image ->
                val domain = image.domain?.name ?: return@filter false
                val verifier = verifiers.find { it.supports(domain) } ?: return@filter false
                !verifier.isInUse(image)
            }
            .mapNotNull { it.id }

        if (staleIds.isEmpty()) {
            log.info("[ImageCleanupService] 실사용되지 않는 USED 이미지가 없습니다.")
            return
        }

        log.info("[ImageCleanupService] 실사용되지 않는 USED 이미지 ${staleIds.size}개 삭제 시작")

        val staleStoredNames = imageRepository.findStoredNamesByIds(staleIds)
        for (storedName in staleStoredNames) {
            try {
                objectStorage.delete(storedName)
            } catch (e: Exception) {
                log.error("[ImageCleanupService] USED 이미지 파일 삭제 실패 - storedName: $storedName")
            }
        }

        imageRepository.deleteAllByIdInBatch(staleIds)
        log.info("[ImageCleanupService] USED 이미지 정리 완료. DB: ${staleIds.size}")
    }
}
