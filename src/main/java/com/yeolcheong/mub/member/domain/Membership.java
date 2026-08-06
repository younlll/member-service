package com.yeolcheong.mub.member.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원의 멤버십(구독) 엔타이틀먼트. 스토어 구매 검증 성공 시 생성되며, 만료일은 스토어가 통지하는 값을 반영한다.
 * 기수는 최초 가입 시점의 라벨로 고정된다(자동갱신되어도 승급 없음).
 */
@Entity
@Table(name = "memberships")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Membership {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long memberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cohort_id", nullable = false)
	private MembershipCohort cohort;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipPlatform platform;

	/** 스토어 구매 식별자(Apple originalTransactionId / Google purchaseToken). 멱등·중복 방지용 유니크. */
	@Column(nullable = false, unique = true, length = 255)
	private String storeTransactionId;

	/** 구매된 스토어 상품 ID. */
	@Column(nullable = false, length = 120)
	private String productId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipStatus status;

	/** 이용 시작 일시. */
	@Column(nullable = false)
	private LocalDateTime startedAt;

	/** 만료(다음 결제) 일시. 스토어가 통지하는 값. */
	@Column(nullable = false)
	private LocalDateTime expiresAt;

	/**
	 * 스토어 재검증 결과로 만료일·상태를 갱신한다(갱신/해지 반영). 기수·플랫폼·거래ID는 불변.
	 *
	 * @param status    갱신된 상태
	 * @param expiresAt 갱신된 만료 일시
	 */
	public void renew(MembershipStatus status, LocalDateTime expiresAt) {
		this.status = status;
		this.expiresAt = expiresAt;
	}

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
