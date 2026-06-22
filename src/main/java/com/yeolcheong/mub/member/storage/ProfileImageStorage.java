package com.yeolcheong.mub.member.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.yeolcheong.mub.member.config.ImageProperties;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 프로필 이미지 스토리지.
 * <p>
 * 업로드된 파일을 스토리지 루트({@code uploadDir/profileDir})에 UUID 파일명으로 저장하고,
 * DB에는 <b>스토리지 중립 상대 경로</b>({@code profileDir/uuid.ext})만 반환한다. 이 상대 경로는
 * 조회 시 {@link ImageProperties#toPublicUrl(String)} 로 공개 URL이 조합되며, 로컬·서버 스토리지
 * 어디에 저장되든 동일하게 동작한다.
 * <p>
 * <b>향후 인프라</b>: 서버 스토리지로 전환되면 파일 쓰기 부분({@link #store})만 해당 스토리지 API로
 * 교체하면 된다. 클라이언트가 스토리지에 직접 업로드하고 경로만 전달하는 구조라면 본 클래스의
 * 쓰기 책임은 제거되고 경로 저장/조회만 남는다. 조회({@code toPublicUrl})는 어느 경우에도 그대로다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageStorage {

	private final ImageProperties imageProperties;

	/**
	 * 업로드 파일을 스토리지에 저장하고 저장된 이미지 메타데이터를 반환한다.
	 *
	 * @param file 업로드된 이미지 파일
	 * @return 저장 결과 메타데이터(스토리지 중립 상대 경로 포함)
	 */
	public StoredImage store(MultipartFile file) {
		validateImage(file);

		String originalFileName = StringUtils.cleanPath(
			file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
		String extension = extractExtension(originalFileName);
		String storedFileName = UUID.randomUUID() + extension;
		// DB 저장용 스토리지 중립 상대 경로 (예: profile/uuid.png)
		String relativePath = imageProperties.profileDir() + "/" + storedFileName;

		try {
			Path directory = Paths.get(imageProperties.uploadDir(), imageProperties.profileDir());
			Files.createDirectories(directory);

			Path destination = directory.resolve(storedFileName);
			file.transferTo(destination.toAbsolutePath());

			log.info("Profile image stored: path={}, size={}", relativePath, file.getSize());
		} catch (IOException e) {
			log.error("Failed to store profile image: original={}", originalFileName, e);
			throw new MemberServiceApiException(ErrorCode.IMAGE_UPLOAD_FAILED);
		}

		return new StoredImage(
			originalFileName.isBlank() ? null : originalFileName,
			storedFileName,
			relativePath,
			file.getContentType(),
			file.getSize());
	}

	/**
	 * 스토리지에서 파일을 삭제한다. 존재하지 않아도 예외를 던지지 않는다.
	 *
	 * @param filePath 스토리지 루트 기준 상대 경로
	 */
	public void delete(String filePath) {
		if (filePath == null || filePath.isBlank()) {
			return;
		}
		try {
			Path target = Paths.get(imageProperties.uploadDir()).resolve(filePath);
			boolean deleted = Files.deleteIfExists(target);
			log.info("Profile image delete attempted: path={}, deleted={}", filePath, deleted);
		} catch (IOException e) {
			// 파일 삭제 실패는 치명적이지 않으므로 경고만 남기고 진행한다.
			log.warn("Failed to delete profile image file: path={}", filePath, e);
		}
	}

	/**
	 * 이미지 파일 유효성 검사 (빈 파일/이미지 타입 여부).
	 */
	private void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new MemberServiceApiException(ErrorCode.INVALID_IMAGE_FILE);
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new MemberServiceApiException("이미지 파일만 업로드할 수 있습니다", ErrorCode.INVALID_IMAGE_FILE);
		}
	}

	/**
	 * 원본 파일명에서 확장자(점 포함)를 추출한다. 없으면 빈 문자열.
	 */
	private String extractExtension(String originalFileName) {
		int dotIndex = originalFileName.lastIndexOf('.');
		if (dotIndex < 0) {
			return "";
		}
		return originalFileName.substring(dotIndex).toLowerCase();
	}
}
