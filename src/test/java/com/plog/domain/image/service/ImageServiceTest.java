package com.plog.domain.image.service;


import com.plog.domain.image.entity.Image;
import com.plog.domain.image.dto.ImageUploadRes;
import com.plog.domain.image.repository.ImageRepository;
import com.plog.domain.member.entity.Member;
import com.plog.domain.member.repository.MemberRepository;
import com.plog.global.exception.errorCode.ImageErrorCode;
import com.plog.global.exception.exceptions.ImageException;
import com.plog.global.minio.storage.ObjectStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * ImageService의 비즈니스 로직을 검증하는 단위 테스트(Unit Test)입니다.
 * <p>
 * 스프링 컨텍스트를 로드하지 않고({@code @SpringBootTest} 제외),
 * {@code @ExtendWith(MockitoExtension.class)}를 사용하여 가볍고 빠르게 동작합니다.
 * 외부 의존성(MinIO 스토리지, DB Repository)은 Mock 객체로 대체하여,
 * 순수하게 서비스 계층의 파일명 변환 로직, 확장자 검사, 예외 처리 등을 검증합니다.
 *
 * <p><b>테스트 환경:</b><br>
 * JUnit 5, Mockito, AssertJ 사용
 *
 * <p><b>주요 검증 포인트:</b><br>
 * 1. 파일 업로드 시 UUID 기반 파일명 생성 여부 확인 <br>
 * 2. 지원하지 않는 파일 확장자 및 빈 파일에 대한 예외 처리 검증 <br>
 * 3. 다중 파일 업로드 시 반복 호출 로직 검증
 *
 * @author Jaewon Ryu
 * @see ImageServiceImpl
 * @since 2026-01-21
 */

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class ImageServiceTest {

    @InjectMocks
    private ImageServiceImpl imageService;

    @Mock
    private ObjectStorage objectStorage;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이미지 업로드 시 UUID가 적용된 고유한 파일명으로 저장소에 전달된다")
    void uploadImageSuccess() {
        // [Given]
        Long memberId = 1L; // 가짜 ID

        Member mockMember = Member.builder()
                .email("test@test.com") // 필수 필드만 대충 채움
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(mockMember, "id", memberId);

        // 👇 [추가] 회원 조회 Mocking
        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.of(mockMember));

        String originalFilename = "test-image.jpg";
        MockMultipartFile file = new MockMultipartFile(
                "file", originalFilename, "image/jpeg", "content".getBytes()
        );
        String mockUrl = "http://minio-url/bucket/uuid-filename.jpg";
        given(objectStorage.upload(any(MultipartFile.class), anyString()))
                .willReturn(mockUrl);

        // [When]
        // 👇 [수정] memberId 파라미터 추가
        ImageUploadRes result = imageService.uploadImage(file, memberId);

        // [Then]
        assertThat(result.successUrls()).hasSize(1);
        assertThat(result.successUrls().get(0)).isEqualTo(mockUrl);
        assertThat(result.failedFilenames()).isEmpty();

        // 파일명 변환 검증 (기존 유지)
        ArgumentCaptor<String> filenameCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorage).upload(any(MultipartFile.class), filenameCaptor.capture());
        String savedFilename = filenameCaptor.getValue();
        assertThat(savedFilename).isNotEqualTo(originalFilename);
        assertThat(savedFilename).endsWith(".jpg");

        // DB 저장 검증
        verify(imageRepository).save(any(Image.class));
    }

    @Test
    @DisplayName("다중 이미지 업로드 성공 시 모든 파일의 URL을 반환한다")
    void uploadImagesSuccess() {
        // [Given]
        Long memberId = 1L;
        Member mockMember = Member.builder()
                .email("test@test.com") // 필수 필드만 대충 채움
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(mockMember, "id", memberId);

        // 👇 [추가] 여러 번 호출되므로 Optional.of 반환
        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.of(mockMember));

        List<MultipartFile> files = List.of(
                new MockMultipartFile("f1", "a.png", "image/png", "d1".getBytes()),
                new MockMultipartFile("f2", "b.jpg", "image/jpeg", "d2".getBytes())
        );
        String mockUrl = "http://mock-url/img";
        given(objectStorage.upload(any(MultipartFile.class), anyString()))
                .willReturn(mockUrl);

        // [When]
        // 👇 [수정] memberId 추가
        ImageUploadRes result = imageService.uploadImages(files, memberId);

        // [Then]
        assertThat(result.successUrls()).hasSize(2);
        assertThat(result.failedFilenames()).isEmpty();

        // 호출 횟수 검증
        verify(objectStorage, times(2)).upload(any(MultipartFile.class), anyString());
        verify(imageRepository, times(2)).save(any(Image.class));
    }

    @Test
    @DisplayName("다중 이미지 업로드 부분 실패 시 성공/실패 파일을 구분하여 반환한다")
    void uploadImagesPartialFailure() {
        // [Given]
        Long memberId = 1L;
        Member mockMember = Member.builder()
                .email("test@test.com") // 필수 필드만 대충 채움
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(mockMember, "id", memberId);
        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.of(mockMember));

        MockMultipartFile validFile = new MockMultipartFile("f1", "ok.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile invalidFile = new MockMultipartFile("f2", "bad.exe", "app/exe", "bad".getBytes());

        given(objectStorage.upload(any(MultipartFile.class), anyString()))
                .willReturn("http://mock.jpg");

        // [When]
        // 👇 [수정] memberId 추가
        ImageUploadRes result = imageService.uploadImages(List.of(validFile, invalidFile), memberId);

        // [Then]
        assertThat(result.successUrls()).hasSize(1);
        assertThat(result.failedFilenames()).containsExactly("bad.exe");
        verify(objectStorage, times(1)).upload(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 예외가 발생한다")
    void uploadImageInvalidExtension() {
        Long memberId = 1L;
        Member mockMember = Member.builder()
                .email("test@test.com") // 필수 필드만 대충 채움
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(mockMember, "id", memberId);
        given(memberRepository.findById(memberId)).willReturn(java.util.Optional.of(mockMember));

        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "danger.exe", "application/x-msdownload", "content".getBytes()
        );

        // 👇 [수정] memberId 추가
        assertThatThrownBy(() -> imageService.uploadImage(txtFile, memberId))
                .isInstanceOf(ImageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_FILE_EXTENSION);
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외가 발생한다")
    void uploadImageEmptyFile() {
        // 빈 파일 체크는 Member 조회 전에 일어나므로 memberRepository Mocking 필요 없음
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        // 👇 [수정] memberId 추가
        assertThatThrownBy(() -> imageService.uploadImage(emptyFile, 1L))
                .isInstanceOf(ImageException.class);
    }

    @Test
    @DisplayName("이미지 단일 삭제 성공 시 스토리지와 DB에서 모두 삭제된다")
    void deleteImageSuccess() {
        // [Given]
        String imageUrl = "http://minio/bucket/uuid-image.jpg";
        String storedName = "uuid-image.jpg";
        Long memberId = 1L;

        // 1. 멤버 생성
        Member mockMember = Member.builder()
                .email("test@test.com")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(mockMember, "id", memberId);

        // 2. 이미지 생성 (uploader 제외)
        Image mockImage = Image.builder()
                .accessUrl(imageUrl)
                .storedName(storedName)
                // .uploader(mockMember) ❌ 이거 빼고
                .build();

        // 3. Reflection으로 uploader 주입 💉
        ReflectionTestUtils.setField(mockImage, "uploader", mockMember);

        given(objectStorage.parsePath(imageUrl)).willReturn(storedName);
        given(imageRepository.findByAccessUrl(imageUrl)).willReturn(java.util.Optional.of(mockImage));

        // [When]
        imageService.deleteImage(imageUrl, memberId);

        // [Then]
        verify(objectStorage, times(1)).delete(storedName);
        verify(imageRepository, times(1)).delete(mockImage);
    }

    @Test
    @DisplayName("존재하지 않는 이미지 삭제 시 예외가 발생한다")
    void deleteImageNotFound() {
        // [Given]
        String wrongUrl = "http://minio/bucket/ghost.jpg";
        Long memberId = 1L;

        given(imageRepository.findByAccessUrl(wrongUrl)).willReturn(java.util.Optional.empty());

        // [When & Then]
        // 👇 [수정] memberId 추가
        assertThatThrownBy(() -> imageService.deleteImage(wrongUrl, memberId))
                .isInstanceOf(ImageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_FOUND);

        verify(objectStorage, times(0)).delete(anyString());
    }

    @Test
    @DisplayName("다중 이미지 삭제 시 리스트 개수만큼 반복하여 삭제한다")
    void deleteImagesSuccess() {
        // [Given]
        Long memberId = 1L;
        // 1. 멤버 생성 및 ID 주입
        Member mockMember = Member.builder()
                .email("test@test.com")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(mockMember, "id", memberId);

        String url1 = "http://minio/bucket/1.jpg";
        String url2 = "http://minio/bucket/2.jpg";
        List<String> urls = List.of(url1, url2);

        given(objectStorage.parsePath(url1)).willReturn("1.jpg");
        given(objectStorage.parsePath(url2)).willReturn("2.jpg");

        // 2. 이미지 생성 (빌더에서 uploader 빼고 생성)
        Image img1 = Image.builder()
                .accessUrl(url1)
                .storedName("1.jpg")
                .build();

        Image img2 = Image.builder()
                .accessUrl(url2)
                .storedName("2.jpg")
                .build();

        // 3. Reflection으로 uploader 강제 주입! 💉
        ReflectionTestUtils.setField(img1, "uploader", mockMember);
        ReflectionTestUtils.setField(img2, "uploader", mockMember);

        given(imageRepository.findByAccessUrl(url1)).willReturn(java.util.Optional.of(img1));
        given(imageRepository.findByAccessUrl(url2)).willReturn(java.util.Optional.of(img2));

        // [When]
        imageService.deleteImages(urls, memberId);

        // [Then]
        verify(objectStorage, times(2)).delete(anyString());
        verify(imageRepository, times(2)).delete(any(Image.class));
    }
}