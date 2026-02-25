package com.plog.domain.image.controller

import com.plog.domain.image.dto.ProfileImageUploadRes
import com.plog.domain.image.service.ProfileImageService
import com.plog.global.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * 프로필 이미지 리소스와 관련된 HTTP 요청을 처리하는 컨트롤러입니다.
 *
 * @author Jaewon Ryu
 * @since 2026-01-23
 */
@RestController
@RequestMapping(value = ["/api/members"])
@Tag(name = "Profile Image", description = "프로필 이미지 관련 API")
class ProfileImageController(
    private val profileImageService: ProfileImageService
) {

    /**
     * 프로필 이미지 업로드 (수정)
     * [POST] /api/members/{memberId}/profile-image
     */
    @Operation(summary = "프로필 이미지 업로드", description = "사용자의 프로필 이미지를 업로드하거나 교체합니다.")
    @PostMapping(value = ["/{memberId}/profile-image"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfileImage(
        @PathVariable memberId: Long,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<CommonResponse<ProfileImageUploadRes>> {
        val response = profileImageService.uploadProfileImage(memberId, file)
        return ResponseEntity.ok(CommonResponse.success(response, "프로필 이미지가 성공적으로 변경되었습니다."))
    }

    /**
     * 프로필 이미지 조회
     * [GET] /api/members/{memberId}/profile-image
     */
    @Operation(summary = "프로필 이미지 조회", description = "사용자의 현재 프로필 이미지 URL을 조회합니다.")
    @GetMapping(value = ["/{memberId}/profile-image"])
    fun getProfileImage(
        @PathVariable memberId: Long
    ): ResponseEntity<CommonResponse<ProfileImageUploadRes>> {
        val response = profileImageService.getProfileImage(memberId)
        return ResponseEntity.ok(CommonResponse.success(response, "프로필 이미지를 조회했습니다."))
    }

    @Operation(summary = "프로필 이미지 삭제", description = "프로필 이미지를 삭제하고 기본 상태로 되돌립니다.")
    @DeleteMapping(value = ["/{memberId}/profile-image"])
    fun deleteProfileImage(
        @PathVariable memberId: Long
    ): ResponseEntity<CommonResponse<Void?>> {
        profileImageService.deleteProfileImage(memberId)
        return ResponseEntity.ok(CommonResponse.success(null, "프로필 이미지가 삭제되었습니다."))
    }
}
