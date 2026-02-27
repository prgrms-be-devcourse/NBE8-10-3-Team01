package com.plog.domain.image.service

import com.plog.domain.image.entity.Image
import com.plog.domain.image.repository.ImageRepository
import com.plog.domain.image.verifier.ImageUsageVerifier
import com.plog.global.minio.storage.ObjectStorage
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.plog.global.util.TimeUtil
import java.time.LocalDateTime
import org.springframework.test.util.ReflectionTestUtils


class ImageCleanupServiceTest {

    private lateinit var mockRepo: ImageRepository
    private lateinit var mockStorage: ObjectStorage
    private lateinit var mockVerifier: ImageUsageVerifier
    private lateinit var mockTimeUtil: TimeUtil
    private lateinit var mockChunkDeleteService: ImageChunkDeleteService
    private lateinit var service: ImageCleanupService

    @BeforeEach
    fun setUp() {
        mockRepo = mockk()
        mockStorage = mockk()
        mockTimeUtil = mockk()
        mockVerifier = mockk()
        mockChunkDeleteService = mockk(relaxUnitFun = true)

        val fixedTime = LocalDateTime.of(2026, 2, 27, 12, 0)
        every { mockTimeUtil.getNowKST() } returns fixedTime

        service = ImageCleanupService(mockRepo, mockStorage, mockTimeUtil, listOf(mockVerifier), mockChunkDeleteService)
    }

    // ===== PENDING 이미지 정리 테스트 =====

    @Test
    fun `고아 이미지가 없을 때`() {
        // given
        every { mockRepo.findPendingOrphanIds(any()) } returns emptyList()
        every { mockRepo.findAllUsedImages() } returns emptyList()

        // when
        service.cleanupOrphanImages()

        // then
        verify(exactly = 1) { mockRepo.findPendingOrphanIds(any()) }
        verify(exactly = 0) { mockChunkDeleteService.deleteOrphanChunk(any()) }
    }

    @Test
    fun `고아 이미지가 있을 때 청크 서비스가 호출된다`() {
        // given
        val orphanIds = (1L..600L).toList()

        every { mockRepo.findPendingOrphanIds(any()) } returns orphanIds
        every { mockRepo.findAllUsedImages() } returns emptyList()

        // when
        service.cleanupOrphanImages()

        // then - 600개 → 500개 청크 + 100개 청크 = 2번 호출
        verify(exactly = 2) { mockChunkDeleteService.deleteOrphanChunk(any()) }
    }

    @Test
    fun `고아 이미지가 500개 이하면 청크 서비스가 1번 호출된다`() {
        // given
        val orphanIds = listOf(1L, 2L)

        every { mockRepo.findPendingOrphanIds(any()) } returns orphanIds
        every { mockRepo.findAllUsedImages() } returns emptyList()

        // when
        service.cleanupOrphanImages()

        // then
        verify(exactly = 1) { mockChunkDeleteService.deleteOrphanChunk(orphanIds) }
    }

    // ===== USED 이미지 재검증 테스트 =====

    @Test
    fun `USED 이미지가 실제로 사용 중이면 삭제되지 않는다`() {
        // given
        val image = makeImage(id = 10L, domain = "POST", accessUrl = "http://example.com/img.jpg")

        every { mockRepo.findPendingOrphanIds(any()) } returns emptyList()
        every { mockRepo.findAllUsedImages() } returns listOf(image)
        every { mockVerifier.supports("POST") } returns true
        every { mockVerifier.isInUse(image) } returns true

        // when
        service.cleanupOrphanImages()

        // then
        verify(exactly = 0) { mockRepo.deleteAllByIdInBatch(any()) }
    }

    @Test
    fun `USED 이미지가 실제로 사용되지 않으면 삭제된다`() {
        // given
        val image = makeImage(id = 10L, domain = "POST", accessUrl = "http://example.com/img.jpg")
        val storedNames = listOf("post/image/10/uuid.jpg")

        every { mockRepo.findPendingOrphanIds(any()) } returns emptyList()
        every { mockRepo.findAllUsedImages() } returns listOf(image)
        every { mockVerifier.supports("POST") } returns true
        every { mockVerifier.isInUse(image) } returns false
        every { mockRepo.findStoredNamesByIds(listOf(10L)) } returns storedNames
        every { mockStorage.delete(any()) } just Runs
        every { mockRepo.deleteAllByIdInBatch(listOf(10L)) } just Runs

        // when
        service.cleanupOrphanImages()

        // then
        verify(exactly = 1) { mockRepo.deleteAllByIdInBatch(listOf(10L)) }
        verify(exactly = 1) { mockStorage.delete("post/image/10/uuid.jpg") }
    }

    @Test
    fun `domain이 null인 USED 이미지는 삭제되지 않는다`() {
        // given
        val image = makeImage(id = 10L, domain = null, accessUrl = "http://example.com/img.jpg")

        every { mockRepo.findPendingOrphanIds(any()) } returns emptyList()
        every { mockRepo.findAllUsedImages() } returns listOf(image)

        // when
        service.cleanupOrphanImages()

        // then
        verify(exactly = 0) { mockRepo.deleteAllByIdInBatch(any()) }
    }

    @Test
    fun `매칭되는 verifier가 없는 USED 이미지는 삭제되지 않는다`() {
        // given
        val image = makeImage(id = 10L, domain = "UNKNOWN", accessUrl = "http://example.com/img.jpg")

        every { mockRepo.findPendingOrphanIds(any()) } returns emptyList()
        every { mockRepo.findAllUsedImages() } returns listOf(image)
        every { mockVerifier.supports("UNKNOWN") } returns false

        // when
        service.cleanupOrphanImages()

        // then
        verify(exactly = 0) { mockRepo.deleteAllByIdInBatch(any()) }
    }

    private fun makeImage(id: Long, domain: String?, accessUrl: String): Image {
        val image = Image(
            originalName = "test.jpg",
            storedName = "post/image/$id/uuid.jpg",
            accessUrl = accessUrl,
            uploader = null,
            domain = domain,
            status = "USED",
            domainId = id
        )
        ReflectionTestUtils.setField(image, "id", id)
        return image
    }
}
