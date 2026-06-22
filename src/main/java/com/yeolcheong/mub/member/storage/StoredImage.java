package com.yeolcheong.mub.member.storage;

/**
 * 스토리지에 저장된 이미지의 메타데이터.
 *
 * @param originalFileName 업로드 원본 파일명
 * @param storedFileName   저장된 파일명(UUID 기반)
 * @param filePath         스토리지 루트 기준 상대 경로(예: profile/uuid.png) — DB에 저장되는 값
 * @param contentType      MIME 타입
 * @param fileSize         파일 크기(byte)
 */
public record StoredImage(
	String originalFileName,
	String storedFileName,
	String filePath,
	String contentType,
	Long fileSize
) {
}
