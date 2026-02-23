package com.plog.global.minio.storage

import org.springframework.web.multipart.MultipartFile

/**
 * 파일 스토리지(File Storage) 기능을 추상화한 인터페이스입니다.
 *
 * 비즈니스 로직이 구체적인 저장소 기술(MinIO, AWS S3, Local Disk 등)에 종속되지 않도록
 * 표준화된 파일 업로드 및 관리 메서드를 정의합니다.
 * 이 인터페이스를 통해 서비스 계층은 저장소가 바뀌어도 코드를 수정할 필요가 없습니다.
 *
 * @author Jaewon Ryu
 * @since 2026-01-16
 */
interface ObjectStorage {

    /**
     * MultipartFile 형태의 파일을 스토리지에 업로드합니다.
     *
     * @param file 업로드할 파일 객체
     * @param destination 저장될 파일의 전체 경로 (파일명 포함)
     * @return 저장된 파일의 전체 URL (Endpoint + Bucket + Path)
     */
    fun upload(file: MultipartFile, destination: String): String

    /**
     * 지정된 경로의 파일을 스토리지에서 삭제합니다.
     *
     * @param destination 삭제할 파일의 경로 (파일명 포함)
     */
    fun delete(destination: String)

    /**
     * 전체 URL에서 스토리지 내부 저장 경로(Object Key)를 추출합니다.
     *
     * @param url 파일의 전체 URL
     * @return 버킷 내부의 파일 경로
     */
    fun parsePath(url: String): String
}