package com.plog.domain.image.scheduler

import com.plog.domain.image.service.ImageCleanupService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 고아 이미지 정리 작업을 주기적으로 실행하는 스케줄러입니다.
 * 매일 새벽 3시에 ImageCleanupService를 자동 호출합니다.
 */
@Component
class ImageCleanupScheduler(
    private val imageCleanupService: ImageCleanupService
) {
    private val log = LoggerFactory.getLogger(ImageCleanupScheduler::class.java)

    /**
     * 매일 새벽 3시에 고아 이미지를 자동으로 정리합니다.
     * cron = "0 0 3 * * *" → 초(0) 분(0) 시(3) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 0 3 * * *")
    fun runOrphanImageCleanup() {
        log.info("🔄 [스케줄러] 고아 이미지 자동 삭제 작업 시작")

        try {
            imageCleanupService.cleanupOrphanImages()
        } catch (e: Exception) {
            log.error("❌ [스케줄러] 고아 이미지 삭제 작업 실패: ${e.message}", e)
        }

        log.info("✅ [스케줄러] 고아 이미지 자동 삭제 작업 완료")
    }
}