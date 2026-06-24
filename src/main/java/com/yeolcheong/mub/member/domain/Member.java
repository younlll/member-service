package com.yeolcheong.mub.member.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "social_provider", nullable = false, length = 20)
	private SnsProvider snsProvider;

	@Column(nullable = false, unique = true, length = 100)
	private String socialId;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(length = 100)
	private String nickname;

	@Column(length = 100)
	private String bio;

	@Column
	private Long imageId;

	@Transient
	private String imageUrl;

	@Column(length = 50)
	private String regionProvince;

	@Column(length = 50)
	private String regionCity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Builder.Default
	private MemberStatus status = MemberStatus.INACTIVE;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Column
	private LocalDateTime lastLoginAt;

	public void updateNickname(String nickname) {
		this.nickname = nickname;
	}

	public void updateBio(String bio) {
		this.bio = bio;
	}

	public void updateRegion(String regionProvince, String regionCity) {
		this.regionProvince = regionProvince;
		this.regionCity = regionCity;
	}

	public void updateMemberState(MemberStatus status) {
		this.status = status;
	}

	/**
	 * 탈퇴(DELETED) 회원의 재가입 처리.
	 * <p>
	 * 같은 레코드(id·socialId·email)를 재사용하되, 프로필 정보를 초기화하고 상태를 {@code INACTIVE}로
	 * 되돌려 온보딩을 다시 진행하도록 한다.
	 */
	public void reactivate() {
		this.status = MemberStatus.INACTIVE;
		this.nickname = null;
		this.bio = null;
		this.regionProvince = null;
		this.regionCity = null;
		this.imageId = null;
	}

	/**
	 * 프로필 이미지를 연결한다(등록/수정).
	 */
	public void assignImage(Long imageId) {
		this.imageId = imageId;
	}

	/**
	 * 프로필 이미지 연결을 해제한다(삭제 → 기본 이미지).
	 */
	public void removeImage() {
		this.imageId = null;
	}

	/**
	 * 조회 응답용 공개 이미지 URL(비영속 필드)을 바인딩한다.
	 */
	public void bindImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
}
