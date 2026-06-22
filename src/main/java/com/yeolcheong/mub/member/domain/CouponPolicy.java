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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 쿠폰 정책(템플릿).
 * 쿠폰명·할인유형·적용대상·혜택값·최소조건 등 쿠폰의 정의를 보유한다.
 * 발급 이후 값 변경은 금지하며, 조건 변경이 필요하면 새 정책 row를 추가한다.
 */
@Entity
@Table(name = "coupon_policies")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class CouponPolicy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false, length = 20)
	private DiscountType discountType;

	@Enumerated(EnumType.STRING)
	@Column(name = "apply_target", nullable = false, length = 20)
	private ApplyTarget applyTarget;

	// 정액 금액·정률 %. 이용권(VOUCHER)은 null
	@Column(name = "benefit_value")
	private Integer benefitValue;

	// 정률 할인의 최대 할인 한도
	@Column(name = "max_discount_amount")
	private Integer maxDiscountAmount;

	// 사용 가능 최소 결제 금액 조건
	@Column(name = "min_order_amount", nullable = false)
	private Integer minOrderAmount;

	// 자동 발급 매수 (예: 모임 3회 체험권 = 3)
	@Column(name = "default_issue_quantity", nullable = false)
	private Integer defaultIssueQuantity;

	// 발급일 기준 유효기간(일)
	@Column(name = "valid_days")
	private Integer validDays;

	@Column(length = 255)
	private String description;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public CouponPolicy(String name, DiscountType discountType, ApplyTarget applyTarget,
		Integer benefitValue, Integer maxDiscountAmount, Integer minOrderAmount,
		Integer defaultIssueQuantity, Integer validDays, String description, Boolean isActive) {
		this.name = name;
		this.discountType = discountType;
		this.applyTarget = applyTarget;
		this.benefitValue = benefitValue;
		this.maxDiscountAmount = maxDiscountAmount;
		this.minOrderAmount = minOrderAmount != null ? minOrderAmount : 0;
		this.defaultIssueQuantity = defaultIssueQuantity != null ? defaultIssueQuantity : 1;
		this.validDays = validDays;
		this.description = description;
		this.isActive = isActive != null ? isActive : Boolean.TRUE;
	}

	/**
	 * 이용권(횟수 차감형) 정책 여부
	 */
	public boolean isVoucher() {
		return discountType == DiscountType.VOUCHER;
	}

	/**
	 * 발급 가능한(활성) 정책 여부
	 */
	public boolean isIssuable() {
		return Boolean.TRUE.equals(isActive);
	}
}
