package com.plog.domain.image.service

import com.plog.domain.image.dto.ProfileImageUploadRes
import com.plog.domain.image.entity.Image
import com.plog.domain.image.repository.ImageRepository
import com.plog.domain.member.entity.Member
import com.plog.domain.member.repository.MemberRepository
import com.plog.global.exception.errorCode.ImageErrorCode
import com.plog.global.exception.exceptions.AuthException
import com.plog.global.exception.exceptions.ImageException
import com.plog.global.minio.storage.ObjectStorage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import com.plog.global.exception.errorCode.AuthErrorCode.USER_NOT_FOUND

/**
 * 프로필 이미지 업로드 및 생명주기 관리를 담당하는 서비스 구현체입니다.
 *
 * [ProfileImageService] 인터페이스를 구현하여 실제 비즈니스 로직을 수행합니다.
 * [ObjectStorage]를 통해 물리적 파일을 관리하고, [ImageRepository]와 [MemberRepository]를 통해
 * DB 메타데이터 및 회원과의 연관관계를 관리합니다.
 * 모든 변경 작업은 `@Transactional` 안에서 원자적으로 수행됩니다.
 *
 * **상속 정보:**
 * [ProfileImageService] 인터페이스를 구현합니다.
 *
 * **주요 생성자:**
 * `ProfileImageServiceImpl(MemberRepository, ImageRepository, ObjectStorage)`
 * 필요한 의존성을 주입받습니다.
 *
 * **빈 관리:**
 * `@Service` 어노테이션을 통해 스프링 빈으로 등록됩니다.
 * 클래스 레벨에는 적용되지 않았으나, 메서드 레벨에서 `@Transactional`을 통해 트랜잭션을 관리합니다.
 *
 * **외부 모듈:**
 * `ObjectStorage`: 추상화된 파일 저장소 인터페이스를 사용합니다 (구현체: MinioStorage).
 * `ImageRepository`: JPA를 통해 DB와 통신합니다.
 * `MemberRepository`: 회원 정보를 조회하고 업데이트합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-01-23
 */
@Service
class ProfileImageServiceImpl(
    private val memberRepository: MemberRepository,
    private val imageRepository: ImageRepository,
    private val objectStorage: ObjectStorage
) : ProfileImageService {

    @Transactional
    override fun uploadProfileImage(memberId: Long, file: MultipartFile): ProfileImageUploadRes {
        // 1. 사용자 검증 및 조회
        validateFile(file)
        val member = memberRepository.findById(memberId)
            .orElseThrow { AuthException(USER_NOT_FOUND, "존재하지 않는 사용자입니다.") }

        deleteOldProfileImage(member)

        val originalFilename = file.originalFilename!!
        val storedName = createStoredFileName(memberId, originalFilename)

        val accessUrl = objectStorage.upload(file, storedName)

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    // 트랜잭션이 롤백된 경우에만 실행
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        try {
                            objectStorage.delete(storedName)
                        } catch (e: Exception) {
                            // 파일 삭제 중 에러가 나더라도, 원래 발생한 DB 트랜잭션 에러를 덮어쓰지 않도록 예외 무시
                        }
                    }
                }
            })
        }

        // 5. DB 저장
        val newImage = Image.builder()
            .originalName(originalFilename)
            .storedName(storedName)
            .accessUrl(accessUrl)
            .uploader(member)
            .build()

        imageRepository.save(newImage)
        member.updateProfileImage(newImage)

        return ProfileImageUploadRes.from(member)
    }

    @Transactional(readOnly = true)
    override fun getProfileImage(memberId: Long): ProfileImageUploadRes {
        val member = memberRepository.findById(memberId)
            .orElseThrow { AuthException(USER_NOT_FOUND,
                "[ProfileImageServiceImpl#getProfileImage] can't find user by id",
                "존재하지 않는 사용자입니다.") }

        return ProfileImageUploadRes.from(member)
    }

    @Transactional
    override fun deleteProfileImage(memberId: Long) {
        val member = memberRepository.findById(memberId)
            .orElseThrow { AuthException(USER_NOT_FOUND,
                "[ProfileImageServiceImpl#deleteProfileImage] can't find user",
                "존재하지 않는 사용자입니다.") }

        if (member.profileImage == null) return

        deleteOldProfileImage(member)
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty || file.originalFilename == null) {
            throw ImageException(
                ImageErrorCode.EMPTY_FILE,
                "[ProfileImageServiceImpl#uploadProfileImage] file is empty",
                "이미지 파일이 비어있습니다."
            )
        }

        val filename = file.originalFilename!!
        if (!isValidExtension(filename)) {
            throw ImageException(
                ImageErrorCode.INVALID_FILE_EXTENSION,
                "[ProfileImageServiceImpl#uploadProfileImage] invalid extension: $filename",
                "지원하지 않는 파일 형식입니다."
            )
        }
    }

    private fun isValidExtension(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".gif")
    }

    private fun createStoredFileName(memberId: Long, originalFilename: String): String {
        val uuid = UUID.randomUUID().toString()
        val ext = originalFilename.substring(originalFilename.lastIndexOf("."))
        return "profile/image/$memberId/$uuid$ext"
    }

    private fun deleteOldProfileImage(member: Member) {
        val oldImage = member.profileImage ?: return

        try {
            objectStorage.delete(oldImage.storedName)
        } catch (ignored: Exception) {}

        member.updateProfileImage(null)
        imageRepository.delete(oldImage)
    }
}
