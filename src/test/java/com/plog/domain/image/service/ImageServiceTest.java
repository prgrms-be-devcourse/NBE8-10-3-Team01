package com.plog.domain.image.service;

import com.plog.domain.image.dto.ImageUploadRes;
import com.plog.domain.image.entity.Image;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ImageServiceTest {

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

        Long memberId = 1L;
        String originalFilename = "test-image.jpg";
        MockMultipartFile file = new MockMultipartFile(
                "file", originalFilename, "image/jpeg", "content".getBytes()
        );
        String mockUrl = "http://minio-url/bucket/uuid-filename.jpg";

        Member mockMember = Member.builder().build();
        given(memberRepository.getReferenceById(memberId)).willReturn(mockMember);

        given(objectStorage.upload(any(MultipartFile.class), anyString()))
                .willReturn(mockUrl);

        // [When]
        ImageUploadRes result = imageService.uploadImage(file, memberId);

        // [Then]
        assertThat(result.successUrls()).hasSize(1);
        assertThat(result.successUrls().get(0)).isEqualTo(mockUrl);
        assertThat(result.failedFilenames()).isEmpty();

        // 파일명 변환 검증
        ArgumentCaptor<String> filenameCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorage).upload(any(MultipartFile.class), filenameCaptor.capture());
        String savedFilename = filenameCaptor.getValue();
        assertThat(savedFilename).isNotEqualTo(originalFilename);
        assertThat(savedFilename).endsWith(".jpg");

        verify(imageRepository).save(any(Image.class));
    }

    @Test
    @DisplayName("다중 이미지 업로드 성공 시 모든 파일의 URL을 반환한다")
    void uploadImagesSuccess() {
        // [Given]
        Long memberId = 1L;
        List<MultipartFile> files = List.of(
                new MockMultipartFile("f1", "a.png", "image/png", "d1".getBytes()),
                new MockMultipartFile("f2", "b.jpg", "image/jpeg", "d2".getBytes())
        );
        String mockUrl = "http://mock-url/img";

        Member mockMember = Member.builder().build();
        given(memberRepository.getReferenceById(memberId)).willReturn(mockMember);

        given(objectStorage.upload(any(MultipartFile.class), anyString()))
                .willReturn(mockUrl);

        // [When]
        ImageUploadRes result = imageService.uploadImages(files, memberId);

        // [Then]
        assertThat(result.successUrls()).hasSize(2);
        assertThat(result.failedFilenames()).isEmpty();

        verify(objectStorage, times(2)).upload(any(MultipartFile.class), anyString());
        verify(imageRepository, times(2)).save(any(Image.class));
    }

    @Test
    @DisplayName("다중 이미지 업로드 부분 실패 시 성공/실패 파일을 구분하여 반환한다")
    void uploadImagesPartialFailure() {
        // [Given]
        Long memberId = 1L;
        MockMultipartFile validFile = new MockMultipartFile("f1", "ok.jpg", "image/jpeg", "data".getBytes());
        MockMultipartFile invalidFile = new MockMultipartFile("f2", "bad.exe", "app/exe", "bad".getBytes());

        Member mockMember = Member.builder().build();
        given(memberRepository.getReferenceById(memberId)).willReturn(mockMember);

        given(objectStorage.upload(any(MultipartFile.class), anyString()))
                .willReturn("http://mock.jpg");

        // [When]
        ImageUploadRes result = imageService.uploadImages(List.of(validFile, invalidFile), memberId);

        // [Then]
        assertThat(result.successUrls()).hasSize(1);
        assertThat(result.failedFilenames()).containsExactly("bad.exe");
        verify(objectStorage, times(1)).upload(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("지원하지 않는 확장자는 예외가 발생한다")
    void uploadImageInvalidExtension() {
        // [Given]
        Long memberId = 1L;
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "danger.exe", "application/x-msdownload", "content".getBytes()
        );

        // [When & Then]
        assertThatThrownBy(() -> imageService.uploadImage(txtFile, memberId))
                .isInstanceOf(ImageException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_FILE_EXTENSION);
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외가 발생한다")
    void uploadImageEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

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

        Member mockMember = Member.builder().build();
        ReflectionTestUtils.setField(mockMember, "id", memberId);

        Image mockImage = Image.builder()
                .accessUrl(imageUrl)
                .storedName(storedName)
                .build();
        ReflectionTestUtils.setField(mockImage, "uploader", mockMember);

        given(objectStorage.parsePath(imageUrl)).willReturn(storedName);
        given(imageRepository.findByAccessUrl(imageUrl)).willReturn(Optional.of(mockImage));

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

        given(imageRepository.findByAccessUrl(wrongUrl)).willReturn(Optional.empty());

        // [When & Then]
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
        Member mockMember = Member.builder().build();
        ReflectionTestUtils.setField(mockMember, "id", memberId);

        String url1 = "http://minio/bucket/1.jpg";
        String url2 = "http://minio/bucket/2.jpg";
        List<String> urls = List.of(url1, url2);

        given(objectStorage.parsePath(url1)).willReturn("1.jpg");
        given(objectStorage.parsePath(url2)).willReturn("2.jpg");

        Image img1 = Image.builder().accessUrl(url1).storedName("1.jpg").build();
        Image img2 = Image.builder().accessUrl(url2).storedName("2.jpg").build();
        ReflectionTestUtils.setField(img1, "uploader", mockMember);
        ReflectionTestUtils.setField(img2, "uploader", mockMember);

        given(imageRepository.findByAccessUrl(url1)).willReturn(Optional.of(img1));
        given(imageRepository.findByAccessUrl(url2)).willReturn(Optional.of(img2));

        // [When]
        imageService.deleteImages(urls, memberId);

        // [Then]
        verify(objectStorage, times(2)).delete(anyString());
        verify(imageRepository, times(2)).delete(any(Image.class));
    }
}
