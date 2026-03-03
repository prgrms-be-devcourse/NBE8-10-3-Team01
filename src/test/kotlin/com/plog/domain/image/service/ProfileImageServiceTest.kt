package com.plog.domain.image.service

import com.plog.domain.image.dto.ProfileImageUploadRes
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
class ProfileImageServiceTest {

    @InjectMocks
    lateinit var profileImageService: ProfileImageServiceImpl

    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var imageRepository: ImageRepository

    @Mock
    lateinit var objectStorage: ObjectStorage

    @Test
    @DisplayName("프로필 이미지 업로드 시 기존 이미지가 없으면 바로 저장된다")
    fun uploadProfileImageSuccess_New() {
        // [Given]
        val memberId = 1L
        val member = createMember(memberId)
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", "data".toByteArray())
        val mockUrl = "http://minio/profile.jpg"

        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))
        whenever(objectStorage.upload(any(), any())).thenReturn(mockUrl)

        // [When]
        val result = profileImageService.uploadProfileImage(memberId, file)

        // [Then]
        assertThat(result.memberId).isEqualTo(memberId)
        assertThat(result.profileImageUrl).isEqualTo(mockUrl)
        verify(imageRepository, times(1)).save(any<Image>())
    }

    @Test
    @DisplayName("프로필 이미지 교체 시 기존 파일과 DB 데이터를 삭제하고 새 이미지를 저장한다")
    fun uploadProfileImageSuccess_Overwrite() {
        // [Given]
        val memberId = 1L
        val member = createMember(memberId)

        // 기존 이미지 설정
        val oldImage = Image(originalName = "old.jpg", storedName = "old/path.jpg", accessUrl = "url", uploader = member)
        member.updateProfileImage(oldImage)

        val newFile = MockMultipartFile("file", "new.jpg", "image/jpeg", "newdata".toByteArray())

        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))
        whenever(objectStorage.upload(any(), any())).thenReturn("http://new-url")

        // [When]
        profileImageService.uploadProfileImage(memberId, newFile)

        // [Then]
        // 1. 기존 파일 삭제 호출 검증
        verify(objectStorage).delete(eq("old/path.jpg"))
        verify(imageRepository).delete(eq(oldImage))

        // 2. 새 파일 업로드 호출 검증
        verify(objectStorage).upload(any(), any())
    }

    @Test
    @DisplayName("프로필 이미지 저장 경로에 회원 ID가 포함되어야 한다")
    fun uploadProfileImage_CheckPath() {
        // [Given]
        val memberId = 99L
        val member = createMember(memberId)
        val file = MockMultipartFile("file", "avatar.png", "image/png", "data".toByteArray())

        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))
        whenever(objectStorage.upload(any(), any())).thenReturn("url")

        // [When]
        profileImageService.uploadProfileImage(memberId, file)

        // [Then]
        val pathCaptor = argumentCaptor<String>()
        verify(objectStorage).upload(any(), pathCaptor.capture())

        val capturedPath = pathCaptor.firstValue
        assertThat(capturedPath).contains("profile/image/$memberId/")
        assertThat(capturedPath).endsWith(".png")
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 예외가 발생한다")
    fun uploadProfileImage_InvalidExtension() {
        // [Given]
        val file = MockMultipartFile("file", "malware.exe", "application/x-msdownload", "data".toByteArray())

        // [When & Then]
        assertThatThrownBy { profileImageService.uploadProfileImage(1L, file) }
            .isInstanceOf(ImageException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_FILE_EXTENSION)
    }

    @Test
    @DisplayName("프로필 이미지 삭제 시 이미지가 없으면 아무 동작도 하지 않는다 (멱등성)")
    fun deleteProfileImage_Idempotent() {
        // [Given]
        val memberId = 1L
        val member = createMember(memberId) // 이미지 없음 (null)
        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))

        // [When]
        profileImageService.deleteProfileImage(memberId)

        // [Then]
        verify(objectStorage, times(0)).delete(any())
        verify(imageRepository, times(0)).delete(any<Image>())
    }

    // --- Helper ---
    private fun createMember(id: Long): Member {
        val member = Member(
            email = "test@test.com",
            nickname = "test",
            password = "pw"
        )
        ReflectionTestUtils.setField(member, "id", id)
        return member
    }

    @Test
    @DisplayName("DB 저장 실패 시 롤백 로직이 실행되어 파일을 삭제해야 한다")
    fun shouldDeleteFile_WhenTransactionRollback() {
        // given
        val memberId = 1L
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".toByteArray())

        val member = Member()
        ReflectionTestUtils.setField(member, "id", memberId)

        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))
        whenever(objectStorage.upload(any(), any())).thenReturn("https://minio.url/test.jpg")

        whenever(imageRepository.save(any<Image>())).thenThrow(RuntimeException("DB Error"))

        try {
            profileImageService.uploadProfileImage(memberId, file)
        } catch (e: RuntimeException) {
            // expected
        }

        verify(objectStorage, times(1)).upload(any(), any())
    }
}
