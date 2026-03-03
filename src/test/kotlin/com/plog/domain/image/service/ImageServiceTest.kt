package com.plog.domain.image.service

import com.plog.domain.image.dto.ImageUploadRes
import com.plog.domain.image.entity.Image
import com.plog.domain.image.repository.ImageRepository
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.ImageErrorCode
import com.plog.global.exception.exceptions.ImageException
import com.plog.global.minio.storage.ObjectStorage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.multipart.MultipartFile
import java.util.*

@ExtendWith(MockitoExtension::class)
@ActiveProfiles("test")
class ImageServiceTest {

    @InjectMocks
    lateinit var imageService: ImageServiceImpl

    @Mock
    lateinit var objectStorage: ObjectStorage

    @Mock
    lateinit var imageRepository: ImageRepository

    @Mock
    lateinit var memberRepository: MemberRepository

    @Test
    @DisplayName("이미지 업로드 시 UUID가 적용된 고유한 파일명으로 저장소에 전달된다")
    fun uploadImageSuccess() {
        // [Given]
        val memberId = 1L
        val originalFilename = "test-image.jpg"
        val file = MockMultipartFile(
            "file", originalFilename, "image/jpeg", "content".toByteArray()
        )
        val mockUrl = "http://minio-url/bucket/uuid-filename.jpg"

        val mockMember = Member()
        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)

        whenever(objectStorage.upload(any(), any()))
            .thenReturn(mockUrl)

        // [When]
        val result = imageService.uploadImage(file, memberId)

        // [Then]
        assertThat(result.successUrls).hasSize(1)
        assertThat(result.successUrls[0]).isEqualTo(mockUrl)
        assertThat(result.failedFilenames).isEmpty()

        // 파일명 변환 검증
        val filenameCaptor = argumentCaptor<String>()
        verify(objectStorage).upload(any(), filenameCaptor.capture())
        val savedFilename = filenameCaptor.firstValue
        assertThat(savedFilename).isNotEqualTo(originalFilename)
        assertThat(savedFilename).endsWith(".jpg")

        verify(imageRepository).save(any<Image>())
    }

    @Test
    @DisplayName("다중 이미지 업로드 성공 시 모든 파일의 URL을 반환한다")
    fun uploadImagesSuccess() {
        // [Given]
        val memberId = 1L
        val files = listOf<MultipartFile>(
            MockMultipartFile("f1", "a.png", "image/png", "d1".toByteArray()),
            MockMultipartFile("f2", "b.jpg", "image/jpeg", "d2".toByteArray())
        )
        val mockUrl = "http://mock-url/img"

        val mockMember = Member()
        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)

        whenever(objectStorage.upload(any(), any()))
            .thenReturn(mockUrl)

        // [When]
        val result = imageService.uploadImages(files, memberId)

        // [Then]
        assertThat(result.successUrls).hasSize(2)
        assertThat(result.failedFilenames).isEmpty()

        verify(objectStorage, times(2)).upload(any(), any())
        verify(imageRepository, times(2)).save(any<Image>())
    }

    @Test
    @DisplayName("다중 이미지 업로드 부분 실패 시 성공/실패 파일을 구분하여 반환한다")
    fun uploadImagesPartialFailure() {
        // [Given]
        val memberId = 1L
        val validFile = MockMultipartFile("f1", "ok.jpg", "image/jpeg", "data".toByteArray())
        val invalidFile = MockMultipartFile("f2", "bad.exe", "app/exe", "bad".toByteArray())

        val mockMember = Member()
        whenever(memberRepository.getReferenceById(memberId)).thenReturn(mockMember)

        whenever(objectStorage.upload(any(), argThat { name: String -> name.endsWith(".jpg") }))
            .thenReturn("http://mock.jpg")

        // [When]
        val result = imageService.uploadImages(listOf(validFile, invalidFile), memberId)

        // [Then]
        assertThat(result.successUrls).hasSize(1)
        assertThat(result.failedFilenames).containsExactly("bad.exe")
        verify(objectStorage, times(1)).upload(any(), any())
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 예외가 발생한다")
    fun uploadImageInvalidExtension() {
        // [Given]
        val memberId = 1L
        val txtFile = MockMultipartFile(
            "file", "danger.exe", "application/x-msdownload", "content".toByteArray()
        )

        // [When & Then]
        assertThatThrownBy { imageService.uploadImage(txtFile, memberId) }
            .isInstanceOf(ImageException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_FILE_EXTENSION)
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외가 발생한다")
    fun uploadImageEmptyFile() {
        val emptyFile = MockMultipartFile(
            "file", "empty.jpg", "image/jpeg", ByteArray(0)
        )

        assertThatThrownBy { imageService.uploadImage(emptyFile, 1L) }
            .isInstanceOf(ImageException::class.java)
    }

    @Test
    @DisplayName("이미지 단일 삭제 성공 시 스토리지와 DB에서 모두 삭제된다")
    fun deleteImageSuccess() {
        // [Given]
        val imageUrl = "http://minio/bucket/uuid-image.jpg"
        val storedName = "uuid-image.jpg"
        val memberId = 1L

        val mockMember = Member()
        ReflectionTestUtils.setField(mockMember, "id", memberId)

        val mockImage = Image(
            originalName = "test.jpg",
            accessUrl = imageUrl,
            storedName = storedName,
            uploader = mockMember
        )

        whenever(objectStorage.parsePath(imageUrl)).thenReturn(storedName)
        whenever(imageRepository.findByAccessUrl(imageUrl)).thenReturn(mockImage)

        // [When]
        imageService.deleteImage(imageUrl, memberId)

        // [Then]
        verify(objectStorage, times(1)).delete(storedName)
        verify(imageRepository, times(1)).delete(mockImage)
    }

    @Test
    @DisplayName("존재하지 않는 이미지 삭제 시 예외가 발생한다")
    fun deleteImageNotFound() {
        // [Given]
        val wrongUrl = "http://minio/bucket/ghost.jpg"
        val memberId = 1L

        whenever(imageRepository.findByAccessUrl(wrongUrl)).thenReturn(null)

        // [When & Then]
        assertThatThrownBy { imageService.deleteImage(wrongUrl, memberId) }
            .isInstanceOf(ImageException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_FOUND)

        verify(objectStorage, times(0)).delete(any())
    }

    @Test
    @DisplayName("다중 이미지 삭제 시 리스트 개수만큼 반복하여 삭제한다")
    fun deleteImagesSuccess() {
        // [Given]
        val memberId = 1L
        val mockMember = Member()
        ReflectionTestUtils.setField(mockMember, "id", memberId)

        val url1 = "http://minio/bucket/1.jpg"
        val url2 = "http://minio/bucket/2.jpg"
        val urls = listOf(url1, url2)

        whenever(objectStorage.parsePath(url1)).thenReturn("1.jpg")
        whenever(objectStorage.parsePath(url2)).thenReturn("2.jpg")

        val img1 = Image(originalName = "1.jpg", accessUrl = url1, storedName = "1.jpg", uploader = mockMember)
        val img2 = Image(originalName = "2.jpg", accessUrl = url2, storedName = "2.jpg", uploader = mockMember)

        whenever(imageRepository.findByAccessUrl(url1)).thenReturn(img1)
        whenever(imageRepository.findByAccessUrl(url2)).thenReturn(img2)

        // [When]
        imageService.deleteImages(urls, memberId)

        // [Then]
        verify(objectStorage, times(2)).delete(any())
        verify(imageRepository, times(2)).delete(any<Image>())
    }
}
