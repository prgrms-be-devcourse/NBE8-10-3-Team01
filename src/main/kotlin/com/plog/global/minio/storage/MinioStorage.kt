package com.plog.global.minio.storage

import com.plog.global.exception.errorCode.ImageErrorCode
import com.plog.global.exception.exceptions.ImageException
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

/**
 * MinIO 객체 스토리지와의 통신을 담당하는 구현체 클래스입니다.
 * <p>
 * {@link ObjectStorage} 인터페이스를 구현하여 파일 업로드, 삭제, 경로 파싱 등의 기능을 수행합니다.
 * 애플리케이션 초기화 시점에 버킷 존재 여부를 확인하고, 없을 경우 자동으로 생성합니다.
 *
 * <p><b>상속 정보:</b><br>
 * {@link ObjectStorage} 인터페이스를 구현합니다.
 *
 * <p><b>주요 생성자:</b><br>
 * {@code MinioStorage(MinioClient minioClient)} <br>
 * MinIO 클라이언트 객체를 주입받아 초기화합니다. <br>
 *
 * <p><b>빈 관리:</b><br>
 * {@code @Component} 어노테이션을 통해 스프링 빈으로 등록됩니다. <br>
 * {@code @ConditionalOnProperty(name = "enabled", havingValue = "true")} 설정에 의해
 * application.yml의 minio.enabled 속성이 true일 경우에만 빈이 생성됩니다.
 *
 * <p><b>외부 모듈:</b><br>
 * {@code io.minio:minio} 라이브러리를 사용하여 MinIO 서버와 통신합니다.
 *
 * @author Jaewon Ryu
 * @since 2026-01-16
 */
@Component
@ConditionalOnProperty(prefix = "minio", name = ["enabled"], havingValue = "true")
class MinioStorage(
    private val minioClient: MinioClient,
    @Value("\${minio.endpoint}") private val endpoint: String,
    @Value("\${minio.external_endpoint}") private val externalEndpoint: String,
    @Value("\${minio.bucket}") private val bucket: String
) : ObjectStorage {

    @PostConstruct
    fun init() = validateBucket()

    override fun upload(file: MultipartFile, destination: String): String {
        return try {
            file.inputStream.use { inputStream ->
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .`object`(destination)
                        .stream(inputStream, file.size, -1)
                        .contentType(file.contentType)
                        .build()
                )
            }
            "$externalEndpoint/$bucket/$destination"
        } catch (e: Exception) {
            throw ImageException(
                ImageErrorCode.IMAGE_UPLOAD_FAILED,
                "[MinioStorage#upload] failed. dest=$destination, cause=${e.message}",
                "이미지 업로드 중 오류가 발생했습니다."
            )
        }
    }

    override fun delete(destination: String) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(destination)
                    .build()
            )
        } catch (e: Exception) {
            throw ImageException(
                ImageErrorCode.IMAGE_DELETE_FAILED,
                "[MinioStorage#delete] failed. dest=$destination, cause=${e.message}",
                "이미지 삭제 중 오류가 발생했습니다."
            )
        }
    }

    override fun parsePath(url: String): String {
        if (url.isEmpty() || !url.contains("$endpoint/$bucket")) return ""
        val prefix = "$endpoint/$bucket"
        if (url == null || !url.contains(prefix)) return ""
        return url.substring(prefix.length + 1)
    }

    private fun validateBucket() {
        try {
            val isExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
            )
            if (isExists) return
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        } catch (e: Exception) {
            throw ImageException(
                ImageErrorCode.BUCKET_INIT_FAILED,
                "[MinioStorage#validateBucket] init failed. bucket=$bucket, cause=${e.message}",
                "이미지 저장소 연결에 실패했습니다."
            )
        }
    }
}