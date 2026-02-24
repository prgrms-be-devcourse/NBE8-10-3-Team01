package com.plog.domain.image.controller

import com.plog.domain.image.dto.ImageUploadRes
import com.plog.domain.image.service.ImageService
import com.plog.global.response.CommonResponse
import com.plog.global.security.SecurityUser
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * 이미지 리소스와 관련된 HTTP 요청을 처리하는 컨트롤러입니다.
 *
 * 클라이언트로부터 이미지 파일을 전송받아 서비스 계층으로 전달하고,
 * 처리 결과를 표준 응답 포맷([CommonResponse])으로 반환합니다.
 *
 * `@RestController`가 적용되어 모든 메서드의 반환값이 Response Body로 직렬화됩니다.
 *
 * @author Jaewon Ryu
 * @since 2026-01-20
 */
@RestController
@RequestMapping(value = ["/api/images"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ImageController(
    private val imageService: ImageService
) {

    /**
     * 단일 이미지를 업로드합니다.
     *
     * **API:** [POST] /api/images
     * **Content-Type:** multipart/form-data
     *
     * @param file 업로드할 이미지 파일 (key: "file")
     * @return 200 OK 상태 코드와 함께 업로드된 이미지 URL을 반환
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
        @AuthenticationPrincipal securityUser: SecurityUser
    ): ResponseEntity<CommonResponse<ImageUploadRes>> {
        val memberId = securityUser.id
        val result = imageService.uploadImage(file, memberId)
        return ResponseEntity.ok(CommonResponse.success(result, "이미지 업로드 성공"))
    }

    /**
     * 다중 이미지를 업로드합니다.
     *
     * **API:** [POST] /api/images/bulk
     * **Content-Type:** multipart/form-data
     *
     * @param files 업로드할 이미지 파일 리스트 (key: "files")
     * @return 200 OK 상태 코드와 함께 성공 URL 및 실패 파일명 목록을 반환
     */
    @PostMapping(value = ["/bulk"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImages(
        @RequestParam("files") files: List<MultipartFile>,
        @AuthenticationPrincipal securityUser: SecurityUser
    ): ResponseEntity<CommonResponse<ImageUploadRes>> {
        val memberId = securityUser.id
        val result = imageService.uploadImages(files, memberId)
        val message = if (result.failedFilenames.isEmpty()) {
            "다중 이미지 업로드 성공"
        } else {
            "다건 업로드 완료 (성공: ${result.successUrls.size}건, 실패: ${result.failedFilenames.size}건)"
        }
        return ResponseEntity.ok(CommonResponse.success(result, message))
    }
}
