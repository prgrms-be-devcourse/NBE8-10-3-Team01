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
        every { mockRepo.findOrphanImages(any()) } returns emptyList()

        service.cleanupOrphanImages()

        verify(exactly = 1) { mockRepo.findOrphanImages(any()) }
        verify(exactly = 0) { mockStorage.delete(any()) }
        verify(exactly = 0) { mockRepo.delete(any()) }
    }

    @Test
    fun `고아 이미지가 있을 때 성공적으로 삭제`() {
        val orphanImage = mockk<Image>(relaxed = true)
        every { orphanImage.storedName } returns "test.jpg"

        every { mockRepo.findOrphanImages(any()) } returns listOf(orphanImage)
        every { mockStorage.delete("test.jpg") } just Runs  // just Runs로 예외 방지!
        every { mockRepo.delete(orphanImage) } just Runs

        service.cleanupOrphanImages()

        verify(exactly = 1) { mockStorage.delete("test.jpg") }
        verify(exactly = 1) { mockRepo.delete(orphanImage) }
    }
}
