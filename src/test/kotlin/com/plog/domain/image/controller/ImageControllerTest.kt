package com.plog.domain.image.controller

import com.ninjasquad.springmockk.MockkBean
import com.plog.domain.image.dto.ImageUploadRes
import com.plog.domain.image.service.ImageService
import com.plog.global.exception.errorCode.ImageErrorCode
import com.plog.global.exception.exceptions.ImageException
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.multipart.MultipartFile

@WebMvcTest(ImageController::class)
@ActiveProfiles("test")
@Import(SecurityTestConfig::class)
class ImageControllerTest : WebMvcTestSupport() {

    @MockkBean
    private lateinit var imageService: ImageService

    @BeforeEach
    fun setUp() {
        val mockUser = mockk<SecurityUser>(relaxed = true)
        every { mockUser.id } returns 1L
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(mockUser, null, emptyList())

        every { imageService.uploadImage(any<MultipartFile>(), any<Long>()) } returns
                ImageUploadRes(listOf("http://minio/uuid.jpg"), emptyList())
        every { imageService.uploadImages(any<List<MultipartFile>>(), any<Long>()) } returns
                ImageUploadRes(listOf("http://minio/bulk1.jpg", "http://minio/bulk2.jpg"), emptyList())
    }

    @Test
    @DisplayName("이미지 업로드 성공 시 successUrls를 포함한 응답을 반환한다")
    fun uploadImageSuccess() {
        val file = MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".toByteArray())

        mockMvc.perform(multipart("/api/images").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.successUrls[0]").value("http://minio/uuid.jpg"))
            .andExpect(jsonPath("$.message").value("이미지 업로드 성공"))
    }

    @Test
    @DisplayName("다중 이미지 업로드 성공 시 successUrls 리스트를 반환한다")
    fun uploadImagesSuccess() {
        val file1 = MockMultipartFile("files", "test1.jpg", "image/jpeg", "data1".toByteArray())
        val file2 = MockMultipartFile("files", "test2.jpg", "image/jpeg", "data2".toByteArray())

        mockMvc.perform(
            multipart("/api/images/bulk").file(file1).file(file2).contentType(MediaType.MULTIPART_FORM_DATA)
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.successUrls").isArray)
            .andExpect(jsonPath("$.data.successUrls.length()").value(2))
            .andExpect(jsonPath("$.message").value("다중 이미지 업로드 성공"))
    }

    @Test
    @DisplayName("다중 이미지 업로드 부분 실패 시 실패 파일명도 포함하여 반환한다")
    fun uploadImagesPartialFailure() {
        every { imageService.uploadImages(any<List<MultipartFile>>(), any()) } returns
                ImageUploadRes(listOf("http://minio/uuid1.jpg"), listOf("invalid.txt"))

        val file1 = MockMultipartFile("files", "test1.jpg", "image/jpeg", "data1".toByteArray())

        mockMvc.perform(multipart("/api/images/bulk").file(file1).contentType(MediaType.MULTIPART_FORM_DATA))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.failedFilenames[0]").value("invalid.txt"))
            .andExpect(jsonPath("$.message").value("다건 업로드 완료 (성공: 1건, 실패: 1건)"))
    }

    @Test
    @DisplayName("지원하지 않는 파일 형식이면 서비스 예외를 400으로 처리한다")
    fun uploadImageInvalidExtension() {
        every { imageService.uploadImage(any(), any()) } throws
                ImageException(ImageErrorCode.INVALID_FILE_EXTENSION, "지원하지 않는 파일 형식입니다.")

        val txtFile = MockMultipartFile("file", "test.txt", "text/plain", "content".toByteArray())
        mockMvc.perform(multipart("/api/images").file(txtFile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.message").value("지원하지 않는 파일 형식입니다."))
    }

    @Test
    @DisplayName("파일 없이 요청하면 400 Bad Request가 발생한다")
    fun uploadImageWithoutFile() {
        mockMvc.perform(multipart("/api/images"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Missing request part"))
    }
}
