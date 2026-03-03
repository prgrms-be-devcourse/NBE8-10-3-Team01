package com.plog.domain.image.controller

import com.ninjasquad.springmockk.MockkBean
import com.plog.domain.image.dto.ProfileImageUploadRes
import com.plog.domain.image.service.ProfileImageService
import com.plog.global.security.SecurityUser
import com.plog.testUtil.SecurityTestConfig
import com.plog.testUtil.WebMvcTestSupport
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProfileImageController::class)
@ActiveProfiles("test")
@Import(SecurityTestConfig::class)
class ProfileImageControllerTest : WebMvcTestSupport() {

    @MockkBean
    private lateinit var profileImageService: ProfileImageService

    @BeforeEach
    fun setUp() {
        val mockUser = mockk<SecurityUser>(relaxed = true)
        every { mockUser.id } returns 1L
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(mockUser, null, emptyList())
    }

    @Test
    @DisplayName("프로필 이미지 업로드 성공 시 변경된 URL을 반환한다")
    fun uploadProfileImageSuccess() {
        val memberId = 1L
        val file = MockMultipartFile("file", "profile.jpg", "image/jpeg", "data".toByteArray())
        every { profileImageService.uploadProfileImage(eq(memberId), any()) } returns
                ProfileImageUploadRes(memberId, "http://minio/new-profile.jpg")

        mockMvc.perform(
            multipart("/api/members/{memberId}/profile-image", memberId)
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA)
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.memberId").value(memberId))
            .andExpect(jsonPath("$.data.profileImageUrl").value("http://minio/new-profile.jpg"))
    }

    @Test
    @DisplayName("프로필 이미지 조회 성공 시 URL을 반환한다")
    fun getProfileImageSuccess() {
        val memberId = 1L
        every { profileImageService.getProfileImage(memberId) } returns
                ProfileImageUploadRes(memberId, "http://minio/profile.jpg")

        mockMvc.perform(get("/api/members/{memberId}/profile-image", memberId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profileImageUrl").value("http://minio/profile.jpg"))
    }

    @Test
    @DisplayName("프로필 이미지 삭제 요청이 성공하면 200 OK를 반환한다")
    fun deleteProfileImageSuccess() {
        val memberId = 1L
        every { profileImageService.deleteProfileImage(memberId) } returns Unit

        mockMvc.perform(delete("/api/members/{memberId}/profile-image", memberId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("프로필 이미지가 삭제되었습니다."))
    }
}
