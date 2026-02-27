package com.plog.domain.image.service

import com.plog.domain.image.dto.ImageUploadRes
import com.plog.domain.image.entity.Image
import com.plog.domain.image.repository.ImageRepository
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.AuthErrorCode
import com.plog.global.exception.errorCode.ImageErrorCode
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.exception.exceptions.ImageException
import com.plog.global.minio.storage.ObjectStorage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import com.plog.domain.image.entity.Image.ImageDomain
import com.plog.domain.image.entity.Image.ImageStatus

/**
 * 이미지 업로드 및 메타데이터 관리를 담당하는 서비스 구현체입니다.
 *
 * [ImageService] 인터페이스를 구현하여 실제 비즈니스 로직을 수행합니다.
 * [ObjectStorage]를 통해 물리적 파일을 저장하고, [ImageRepository]를 통해 DB에 메타데이터를 저장합니다.
 * 모든 업로드 작업은 `@Transactional` 안에서 원자적으로 수행됩니다.
 *
 * @author Jaewon Ryu
 * @since 2026-01-20
 */
@Service
class ImageServiceImpl(
    private val objectStorage: ObjectStorage,
    private val imageRepository: ImageRepository,
    private val memberRepository: MemberRepository
) : ImageService {

    @Transactional
    override fun uploadImage(file: MultipartFile, memberId: Long): ImageUploadRes {
        if (file.isEmpty || file.originalFilename == null) {
            throw ImageException(
                ImageErrorCode.EMPTY_FILE,
                "[ImageServiceImpl#uploadImage] file is empty or filename is null",
                "이미지 파일이 비어있거나 잘못된 요청입니다."
            )
        }

        val originalFileName = file.originalFilename!!

        if (!isValidExtension(originalFileName)) {
            throw ImageException(
                ImageErrorCode.INVALID_FILE_EXTENSION,
                "[ImageServiceImpl#uploadImage] invalid file extension request: $originalFileName",
                "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 가능)"
            )
        }

        val uploader: Member = memberRepository.getReferenceById(memberId)
        val storedFileName = createStoredFileName(originalFileName)
        val accessUrl = objectStorage.upload(file, storedFileName)

        val image = Image.builder()
            .originalName(originalFileName)
            .storedName(storedFileName)
            .accessUrl(accessUrl)
            .uploader(uploader)
            .domain(ImageDomain.POST)
            .status(ImageStatus.PENDING)
            .domainId(null)
            .build()

        imageRepository.save(image)
        return ImageUploadRes(listOf(accessUrl), emptyList())
    }

    @Transactional
    override fun uploadImages(files: List<MultipartFile>, memberId: Long): ImageUploadRes {
        if (files.isEmpty()) return ImageUploadRes(emptyList(), emptyList())

        val successUrls = mutableListOf<String>()
        val failedFilenames = mutableListOf<String>()

        for (file in files) {
            try {
                val singleResult = uploadImage(file, memberId)
                successUrls.addAll(singleResult.successUrls)
            } catch (e: Exception) {
                failedFilenames.add(file.originalFilename ?: "unknown-file")
            }
        }

        return ImageUploadRes(successUrls, failedFilenames)
    }

    @Transactional
    override fun deleteImage(imageUrl: String, memberId: Long) {
        if (imageUrl.isBlank()) return

        val image = imageRepository.findByAccessUrl(imageUrl)
            .orElseThrow {
                ImageException(
                    ImageErrorCode.IMAGE_NOT_FOUND,
                    "[ImageServiceImpl#deleteImage] image not found in DB. url=$imageUrl",
                    "해당 이미지를 찾을 수 없습니다."
                )
            }

        if (image.uploader?.id != memberId) {
            throw AuthException(
                AuthErrorCode.USER_AUTH_FAIL,
                "[ImageServiceImpl#deleteImage] unauthorized deletion attempt. uploaderId=${image.uploader?.id}, requesterId=$memberId",
                "이미지를 삭제할 권한이 없습니다."
            )
        }

        val storedName = objectStorage.parsePath(imageUrl)
        objectStorage.delete(storedName)
        imageRepository.delete(image)
    }

    @Transactional
    override fun deleteImages(imageUrls: List<String>, memberId: Long) {
        if (imageUrls.isEmpty()) return
        imageUrls.forEach { deleteImage(it, memberId) }
    }

    private fun createStoredFileName(originalFilename: String): String {
        val uuid = UUID.randomUUID().toString()
        val ext = originalFilename.substring(originalFilename.lastIndexOf("."))
        return uuid + ext
    }

    private fun isValidExtension(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".gif")
    }
}
