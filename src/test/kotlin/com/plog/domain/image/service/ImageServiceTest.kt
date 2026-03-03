package com.plog.domain.image.service

import com.plog.domain.image.entity.Image
import com.plog.domain.image.repository.ImageRepository
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.ImageErrorCode
import com.plog.global.exception.exceptions.ImageException
import com.plog.global.minio.storage.ObjectStorage
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class ImageServiceTest {

    private lateinit var objectStorage: ObjectStorage
    private lateinit var imageRepository: ImageRepository
    private lateinit var memberRepository: MemberRepository
    private lateinit var imageService: ImageServiceImpl

    @BeforeEach
    fun setUp() {
        objectStorage = mockk()
        imageRepository = mockk()
        memberRepository = mockk()
        imageService = ImageServiceImpl(objectStorage, imageRepository, memberRepository)
    }

    @Test
    @DisplayName("이미지 업로드 시 UUID가 적용된 고유한 파일명으로 저장소에 전달된다")
    fun uploadImageSuccess() {
        // given
        val memberId = 1L
        val originalFilename = "test-image.jpg"
        val file = MockMultipartFile("file", originalFilename, "image/jpeg", "content".toByteArray())
        val mockUrl = "http://minio-url/bucket/uuid-filename.jpg"
        val mockMember = mockk<Member> { every { id } returns memberId }
        val filenameSlot = slot<String>()

        every { memberRepository.getReferenceById(memberId) } returns mockMember
        every { objectStorage.upload(any(), capture(filenameSlot)) } returns mockUrl
        every { imageRepository.save(any()) } returns mockk()

        // when
        val result = imageService.uploadImage(file, memberId)

        // then
        assertThat(result.successUrls).hasSize(1)
        assertThat(result.successUrls[0]).isEqualTo(mockUrl)
        assertThat(filenameSlot.captured).isNotEqualTo(originalFilename)
        assertThat(filenameSlot.captured).endsWith(".jpg")
        verify(exactly = 1) { imageRepository.save(any()) }
    }

    @Test
    @DisplayName("다중 이미지 업로드 성공 시 모든 파일의 URL을 반환한다")
    fun uploadImagesSuccess() {
        // given
        val memberId = 1L
        val files = listOf(
            MockMultipartFile("f1", "a.png", "image/png", "d1".toByteArray()),
            MockMultipartFile("f2", "b.jpg", "image/jpeg", "d2".toByteArray())
        )
        val mockMember = mockk<Member> { every { id } returns memberId }

        every { memberRepository.getReferenceById(memberId) } returns mockMember
        every { objectStorage.upload(any(), any()) } returns "http://mock-url/img"
        every { imageRepository.save(any()) } returns mockk()

        // when
        val result = imageService.uploadImages(files, memberId)

        // then
        assertThat(result.successUrls).hasSize(2)
        assertThat(result.failedFilenames).isEmpty()
        verify(exactly = 2) { objectStorage.upload(any(), any()) }
        verify(exactly = 2) { imageRepository.save(any()) }
    }

    @Test
    @DisplayName("다중 이미지 업로드 부분 실패 시 성공/실패 파일을 구분하여 반환한다")
    fun uploadImagesPartialFailure() {
        // given
        val memberId = 1L
        val validFile = MockMultipartFile("f1", "ok.jpg", "image/jpeg", "data".toByteArray())
        val invalidFile = MockMultipartFile("f2", "bad.exe", "app/exe", "bad".toByteArray())
        val mockMember = mockk<Member> { every { id } returns memberId }

        every { memberRepository.getReferenceById(memberId) } returns mockMember
        every { objectStorage.upload(any(), any()) } returns "http://mock.jpg"
        every { imageRepository.save(any()) } returns mockk()

        // when
        val result = imageService.uploadImages(listOf(validFile, invalidFile), memberId)

        // then
        assertThat(result.successUrls).hasSize(1)
        assertThat(result.failedFilenames).containsExactly("bad.exe")
        verify(exactly = 1) { objectStorage.upload(any(), any()) }
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 예외가 발생한다")
    fun uploadImageInvalidExtension() {
        // given
        val txtFile = MockMultipartFile("file", "danger.exe", "application/x-msdownload", "content".toByteArray())

        // when & then
        assertThatThrownBy { imageService.uploadImage(txtFile, 1L) }
            .isInstanceOf(ImageException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_FILE_EXTENSION)
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외가 발생한다")
    fun uploadImageEmptyFile() {
        val emptyFile = MockMultipartFile("file", "empty.jpg", "image/jpeg", ByteArray(0))

        assertThatThrownBy { imageService.uploadImage(emptyFile, 1L) }
            .isInstanceOf(ImageException::class.java)
    }

    @Test
    @DisplayName("이미지 단일 삭제 성공 시 스토리지와 DB에서 모두 삭제된다")
    fun deleteImageSuccess() {
        // given
        val imageUrl = "http://minio/bucket/uuid-image.jpg"
        val storedName = "uuid-image.jpg"
        val memberId = 1L
        val mockMember = mockk<Member> { every { id } returns memberId }
        val mockImage = Image(
            originalName = "test.jpg",
            storedName = storedName,
            accessUrl = imageUrl,
            uploader = mockMember
        )

        every { objectStorage.parsePath(imageUrl) } returns storedName
        every { imageRepository.findByAccessUrl(imageUrl) } returns mockImage
        every { objectStorage.delete(storedName) } just Runs
        every { imageRepository.delete(mockImage) } just Runs

        // when
        imageService.deleteImage(imageUrl, memberId)

        // then
        verify(exactly = 1) { objectStorage.delete(storedName) }
        verify(exactly = 1) { imageRepository.delete(mockImage) }
    }

    @Test
    @DisplayName("존재하지 않는 이미지 삭제 시 예외가 발생한다")
    fun deleteImageNotFound() {
        // given
        val wrongUrl = "http://minio/bucket/ghost.jpg"

        every { imageRepository.findByAccessUrl(wrongUrl) } returns null

        // when & then
        assertThatThrownBy { imageService.deleteImage(wrongUrl, 1L) }
            .isInstanceOf(ImageException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_FOUND)
        verify(exactly = 0) { objectStorage.delete(any()) }
    }

    @Test
    @DisplayName("다중 이미지 삭제 시 리스트 개수만큼 반복하여 삭제한다")
    fun deleteImagesSuccess() {
        // given
        val memberId = 1L
        val mockMember = mockk<Member> { every { id } returns memberId }
        val url1 = "http://minio/bucket/1.jpg"
        val url2 = "http://minio/bucket/2.jpg"

        every { objectStorage.parsePath(url1) } returns "1.jpg"
        every { objectStorage.parsePath(url2) } returns "2.jpg"

        val img1 = Image(originalName = "test1.jpg", storedName = "1.jpg", accessUrl = url1, uploader = mockMember)
        val img2 = Image(originalName = "test2.jpg", storedName = "2.jpg", accessUrl = url2, uploader = mockMember)

        every { imageRepository.findByAccessUrl(url1) } returns img1
        every { imageRepository.findByAccessUrl(url2) } returns img2
        every { objectStorage.delete(any()) } just Runs
        every { imageRepository.delete(any()) } just Runs

        // when
        imageService.deleteImages(listOf(url1, url2), memberId)

        // then
        verify(exactly = 2) { objectStorage.delete(any()) }
        verify(exactly = 2) { imageRepository.delete(any()) }
    }
}
