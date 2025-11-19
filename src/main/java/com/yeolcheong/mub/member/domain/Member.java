package com.yeolcheong.mub.member.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeolcheong.mub.member.common.MemberStatus;
import com.yeolcheong.mub.member.common.SnsProvider;

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
	@Column(nullable = false, length = 20)
	private SnsProvider snsProvider;

	@Column(nullable = false, unique = true, length = 100)
	private String socialId;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(length = 100)
	private String nickname;

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

	public void updateRegion(String regionProvince, String regionCity) {
		this.regionProvince = regionProvince;
		this.regionCity = regionCity;
	}

	public void updateMemberState(MemberStatus status) {
		this.status = status;
	}
}
