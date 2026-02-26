package com.plog.domain.image.service

import com.plog.domain.image.entity.Image
import com.plog.domain.image.repository.ImageRepository
import com.plog.global.minio.storage.ObjectStorage
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImageCleanupServiceTest {

    private lateinit var mockRepo: ImageRepository
    private lateinit var mockStorage: ObjectStorage
    private lateinit var service: ImageCleanupService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockRepo = mockk()
        mockStorage = mockk()
        service = ImageCleanupService(mockRepo, mockStorage)
    }

    @Test
    fun `고아 이미지가 없을 때`() {
        // given
        every { mockRepo.findOrphanImageIds(any()) } returns emptyList()
        every { mockRepo.findPendingOrphanIds(any()) } returns emptyList()

        // when
        service.cleanupOrphanImages()

        // then
        verify(exactly = 1) { mockRepo.findPendingOrphanIds(any()) }
        verify(exactly = 0) { mockRepo.findStoredNamesByIds(any<List<Long>>()) }
        verify(exactly = 0) { mockRepo.deleteAllByIdInBatch(any()) }
    }

    @Test
    fun `고아 이미지가 있을 때 성공적으로 삭제`() {
        // given
        val orphanIds = listOf(1L, 2L)
        val storedNames = listOf("test1.jpg", "test2.jpg")

        every { mockRepo.findOrphanImageIds(any()) } returns orphanIds
        every { mockRepo.findPendingOrphanIds(any()) } returns orphanIds
        every { mockRepo.findStoredNamesByIds(orphanIds) } returns storedNames
        every { mockStorage.delete(any()) } just Runs
        every { mockRepo.deleteAllByIdInBatch(orphanIds) } just Runs

        // when
        service.cleanupOrphanImages()

        // then
        verify(exactly = 1) { mockRepo.findPendingOrphanIds(any()) }
        verify(exactly = 1) { mockRepo.findStoredNamesByIds(orphanIds) }
        verify(exactly = storedNames.size) { mockStorage.delete(any()) }
        verify(exactly = 1) { mockRepo.deleteAllByIdInBatch(orphanIds) }
    }
}
