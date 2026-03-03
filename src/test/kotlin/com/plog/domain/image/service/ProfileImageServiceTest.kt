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
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

@ActiveProfiles("test")
class ProfileImageServiceTest {

    private lateinit var memberRepository: MemberRepository
    private lateinit var imageRepository: ImageRepository
    private lateinit var objectStorage: ObjectStorage
    private lateinit var profileImageService: ProfileImageServiceImpl

    @BeforeEach
    fun setUp() {
        memberRepository = mockk()
        imageRepository = mockk()
        objectStorage = mockk()
        profileImageService = ProfileImageServiceImpl(memberRepository, imageRepository, objectStorage)
    }

    @Test
    @DisplayName("프로필 이미지 업로드 시 기존 이미지가 없으면 바로 저장된다")
    fun uploadProfileImageSuccess_New() {
        // given
        val memberId = 1L
        val member = createMember(memberId)
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", "data".toByteArray())

        every { memberRepository.findById(memberId) } returns Optional.of(member)
        every { objectStorage.upload(any(), any()) } returns "http://minio/profile.jpg"
        every { imageRepository.save(any()) } returns mockk()

        // when
        val result = profileImageService.uploadProfileImage(memberId, file)

        // then
        assertThat(result.memberId).isEqualTo(memberId)
        assertThat(result.profileImageUrl).isEqualTo("http://minio/profile.jpg")
        verify(exactly = 1) { imageRepository.save(any()) }
    }

    @Test
    @DisplayName("프로필 이미지 교체 시 기존 파일과 DB 데이터를 삭제하고 새 이미지를 저장한다")
    fun uploadProfileImageSuccess_Overwrite() {
        // given
        val memberId = 1L
        val member = createMember(memberId)
        val oldImage = Image(
            originalName = "old.jpg",
            storedName = "old/path.jpg",
            accessUrl = "http://old-url",
            uploader = member
        )
        member.updateProfileImage(oldImage)
        val newFile = MockMultipartFile("file", "new.jpg", "image/jpeg", "newdata".toByteArray())

        every { memberRepository.findById(memberId) } returns Optional.of(member)
        every { objectStorage.delete("old/path.jpg") } just Runs
        every { imageRepository.delete(oldImage) } just Runs
        every { objectStorage.upload(any(), any()) } returns "http://new-url"
        every { imageRepository.save(any()) } returns mockk()

        // when
        profileImageService.uploadProfileImage(memberId, newFile)

        // then
        verify { objectStorage.delete("old/path.jpg") }
        verify { imageRepository.delete(oldImage) }
        verify { objectStorage.upload(any(), any()) }
    }

    @Test
    @DisplayName("프로필 이미지 저장 경로에 회원 ID가 포함되어야 한다")
    fun uploadProfileImage_CheckPath() {
        // given
        val memberId = 99L
        val member = createMember(memberId)
        val file = MockMultipartFile("file", "avatar.png", "image/png", "data".toByteArray())
        val pathSlot = slot<String>()

        every { memberRepository.findById(memberId) } returns Optional.of(member)
        every { objectStorage.upload(any(), capture(pathSlot)) } returns "url"
        every { imageRepository.save(any()) } returns mockk()

        // when
        profileImageService.uploadProfileImage(memberId, file)

        // then
        assertThat(pathSlot.captured).contains("profile/image/$memberId/")
        assertThat(pathSlot.captured).endsWith(".png")
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 예외가 발생한다")
    fun uploadProfileImage_InvalidExtension() {
        val file = MockMultipartFile("file", "malware.exe", "application/x-msdownload", "data".toByteArray())

        assertThatThrownBy { profileImageService.uploadProfileImage(1L, file) }
            .isInstanceOf(ImageException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_FILE_EXTENSION)
    }

    @Test
    @DisplayName("프로필 이미지 삭제 시 이미지가 없으면 아무 동작도 하지 않는다 (멱등성)")
    fun deleteProfileImage_Idempotent() {
        // given
        val memberId = 1L
        val member = createMember(memberId)
        every { memberRepository.findById(memberId) } returns Optional.of(member)

        // when
        profileImageService.deleteProfileImage(memberId)

        // then
        verify(exactly = 0) { objectStorage.delete(any()) }
        verify(exactly = 0) { imageRepository.delete(any()) }
    }

    @Test
    @DisplayName("DB 저장 실패 시 롤백 로직이 실행되어 파일을 삭제해야 한다")
    fun shouldDeleteFile_WhenTransactionRollback() {
        // given
        val memberId = 1L
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".toByteArray())
        val member = createMember(memberId)

        every { memberRepository.findById(memberId) } returns Optional.of(member)
        every { objectStorage.upload(any(), any()) } returns "https://minio.url/test.jpg"
        every { imageRepository.save(any()) } throws RuntimeException("DB Error")

        // when
        runCatching { profileImageService.uploadProfileImage(memberId, file) }

        // then
        verify(exactly = 1) { objectStorage.upload(any(), any()) }
    }

    private fun createMember(id: Long): Member {
        val member = Member(email = "test@test.com", nickname = "test", password = "pw")
        ReflectionTestUtils.setField(member, "id", id)
        return member
    }
}
