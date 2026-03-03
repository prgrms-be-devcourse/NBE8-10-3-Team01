package com.plog.domain.image.service

import com.plog.domain.image.repository.ImageRepository
import com.plog.global.minio.storage.ObjectStorage
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImageChunkDeleteServiceTest {

    private lateinit var mockRepo: ImageRepository
    private lateinit var mockStorage: ObjectStorage
    private lateinit var service: ImageChunkDeleteService

    @BeforeEach
    fun setUp() {
        mockRepo = mockk()
        mockStorage = mockk()
        service = ImageChunkDeleteService(mockRepo, mockStorage)
    }

    @Test
    fun `스토리지 삭제 실패해도 DB 삭제는 반드시 실행된다`() {
        // given
        val chunkIds = listOf(1L, 2L)
        val storedNames = listOf("test1.jpg", "test2.jpg")

        every { mockRepo.findStoredNamesByIds(chunkIds) } returns storedNames
        every { mockStorage.delete(any()) } throws RuntimeException("MinIO 연결 실패")
        every { mockRepo.deleteAllByIdInBatch(chunkIds) } just Runs

        // when
        service.deleteOrphanChunk(chunkIds)

        // then - 스토리지가 전부 실패해도 DB 배치 삭제는 반드시 호출
        verify(exactly = 1) { mockRepo.deleteAllByIdInBatch(chunkIds) }
    }

    @Test
    fun `스토리지 일부만 실패해도 나머지는 삭제되고 DB도 실행된다`() {
        // given
        val chunkIds = listOf(1L, 2L, 3L)
        val storedNames = listOf("ok1.jpg", "fail.jpg", "ok2.jpg")

        every { mockRepo.findStoredNamesByIds(chunkIds) } returns storedNames
        every { mockStorage.delete("ok1.jpg") } just Runs
        every { mockStorage.delete("fail.jpg") } throws RuntimeException("삭제 실패")
        every { mockStorage.delete("ok2.jpg") } just Runs
        every { mockRepo.deleteAllByIdInBatch(chunkIds) } just Runs

        // when
        service.deleteOrphanChunk(chunkIds)

        // then - 3개 시도, 2개 성공, DB는 전체 배치 삭제
        verify(exactly = 3) { mockStorage.delete(any()) }
        verify(exactly = 1) { mockRepo.deleteAllByIdInBatch(chunkIds) }
    }

    @Test
    fun `정상 케이스에서 스토리지와 DB 모두 삭제된다`() {
        // given
        val chunkIds = listOf(1L, 2L)
        val storedNames = listOf("test1.jpg", "test2.jpg")

        every { mockRepo.findStoredNamesByIds(chunkIds) } returns storedNames
        every { mockStorage.delete(any()) } just Runs
        every { mockRepo.deleteAllByIdInBatch(chunkIds) } just Runs

        // when
        service.deleteOrphanChunk(chunkIds)

        // then
        verify(exactly = storedNames.size) { mockStorage.delete(any()) }
        verify(exactly = 1) { mockRepo.deleteAllByIdInBatch(chunkIds) }
    }
}
