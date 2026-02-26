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
    @Test
    fun `스토리지 삭제 실패해도 DB 삭제는 반드시 실행된다`() {
        // given
        val orphanIds = listOf(1L, 2L)
        val storedNames = listOf("test1.jpg", "test2.jpg")

        every { mockRepo.findPendingOrphanIds(any()) } returns orphanIds
        every { mockRepo.findStoredNamesByIds(orphanIds) } returns storedNames
        every { mockStorage.delete(any()) } throws RuntimeException("MinIO 연결 실패")
        every { mockRepo.deleteAllByIdInBatch(orphanIds) } just Runs

        // when
        service.cleanupOrphanImages()

        // then - 스토리지가 전부 실패해도 DB 배치 삭제는 반드시 호출
        verify(exactly = 1) { mockRepo.deleteAllByIdInBatch(orphanIds) }
    }

    @Test
    fun `스토리지 일부만 실패해도 나머지는 삭제되고 DB도 실행된다`() {
        // given
        val orphanIds = listOf(1L, 2L, 3L)
        val storedNames = listOf("ok1.jpg", "fail.jpg", "ok2.jpg")

        every { mockRepo.findPendingOrphanIds(any()) } returns orphanIds
        every { mockRepo.findStoredNamesByIds(orphanIds) } returns storedNames
        every { mockStorage.delete("ok1.jpg") } just Runs
        every { mockStorage.delete("fail.jpg") } throws RuntimeException("삭제 실패")
        every { mockStorage.delete("ok2.jpg") } just Runs
        every { mockRepo.deleteAllByIdInBatch(orphanIds) } just Runs

        // when
        service.cleanupOrphanImages()

        // then - 3개 시도, 2개 성공, DB는 전체 배치 삭제
        verify(exactly = 3) { mockStorage.delete(any()) }
        verify(exactly = 1) { mockRepo.deleteAllByIdInBatch(orphanIds) }
    }
}
