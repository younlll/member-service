package com.yeolcheong.mub.member.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 프로필 이미지.
 * <p>
 * 실제 바이너리는 스토리지(현재는 로컬 {@code images/profile}, 향후 오브젝트 스토리지)에 저장하고,
 * DB에는 스토리지 루트 기준 <b>상대 경로({@code file_path})만</b> 보관한다.
 * 공개 URL은 조회 시점에 {@code base-url + url-path-prefix + file_path} 로 조합한다.
 * 회원과의 연결은 {@code members.image_id} 단방향 참조로 표현한다(1:1).
 */
@Entity
@Table(name = "member_profile_images")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberProfileImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 업로드 원본 파일명 (경로 기반 등록 시 null 가능)
	@Column(length = 255)
	private String originalFileName;

	// 스토리지에 저장된 파일명 (UUID 기반)
	@Column(nullable = false, length = 255)
	private String storedFileName;

	// 스토리지 루트 기준 상대 경로 (예: profile/{uuid}.png)
	@Column(nullable = false, length = 500)
	private String filePath;

	@Column(length = 100)
	private String contentType;

	@Column
	private Long fileSize;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * 새 파일 정보로 교체한다(프로필 사진 수정 시 동일 행 갱신).
	 */
	public void changeFile(String originalFileName, String storedFileName, String filePath,
		String contentType, Long fileSize) {
		this.originalFileName = originalFileName;
		this.storedFileName = storedFileName;
		this.filePath = filePath;
		this.contentType = contentType;
		this.fileSize = fileSize;
	}
}
