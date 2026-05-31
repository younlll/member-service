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
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원에게 발급된 개별 쿠폰 인스턴스.
 * 1매 = 1회 사용이며, 횟수형(이용권)은 정책의 default_issue_quantity만큼 여러 매가 발급된다.
 * 본인 계정 전용으로 양도/현금화할 수 없다.
 */
@Entity
@Table(name = "member_coupons",
	uniqueConstraints = @UniqueConstraint(columnNames = "coupon_code"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class MemberCoupon {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 시스템 고유 식별 코드
	@Column(name = "coupon_code", nullable = false, unique = true, length = 50)
	private String couponCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "policy_id", nullable = false)
	private CouponPolicy policy;

	// 소유 회원 (members.id 참조 — FK 아님)
	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CouponStatus status;

	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "used_reference_type", length = 30)
	private CouponReferenceType usedReferenceType;

	// 사용된 활동/예약/구독 식별자 (타 서비스 참조 — FK 아님)
	@Column(name = "used_reference_id")
	private Long usedReferenceId;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	private MemberCoupon(String couponCode, CouponPolicy policy, Long memberId,
		LocalDateTime issuedAt, LocalDateTime expiresAt) {
		this.couponCode = couponCode;
		this.policy = policy;
		this.memberId = memberId;
		this.status = CouponStatus.AVAILABLE;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	/**
	 * 쿠폰 발급 (AVAILABLE 상태로 생성)
	 */
	public static MemberCoupon issue(String couponCode, CouponPolicy policy, Long memberId,
		LocalDateTime issuedAt, LocalDateTime expiresAt) {
		return MemberCoupon.builder()
			.couponCode(couponCode)
			.policy(policy)
			.memberId(memberId)
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build();
	}

	/**
	 * 기준 시각에 사용 가능한지 여부 (AVAILABLE 이면서 유효기간 내)
	 */
	public boolean isUsable(LocalDateTime at) {
		return status == CouponStatus.AVAILABLE && at.isBefore(expiresAt);
	}

	/**
	 * 사용 처리. 사용된 대상(활동/예약/구독)을 함께 기록한다.
	 */
	public void markUsed(CouponReferenceType referenceType, Long referenceId, LocalDateTime usedAt) {
		this.status = CouponStatus.USED;
		this.usedReferenceType = referenceType;
		this.usedReferenceId = referenceId;
		this.usedAt = usedAt;
	}

	/**
	 * 사용 복구. 활동/예약 시작 전 취소 시 AVAILABLE로 되돌린다.
	 */
	public void restore() {
		this.status = CouponStatus.AVAILABLE;
		this.usedReferenceType = null;
		this.usedReferenceId = null;
		this.usedAt = null;
	}

	/**
	 * 유효기간 만료 처리
	 */
	public void expire() {
		this.status = CouponStatus.EXPIRED;
	}
}
