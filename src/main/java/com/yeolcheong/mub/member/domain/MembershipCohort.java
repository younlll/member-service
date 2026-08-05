package com.yeolcheong.mub.member.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * 멤버십 기수(모집 차수). 상품(플랜)에 붙는 라벨로, 기수별 모집 기간만 다르고 상품 자체는 동일하다.
 * 새 기수는 이 테이블에 row 만 추가하면 되며, 스토어 상품 재등록은 필요 없다.
 */
@Entity
@Table(name = "membership_cohorts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MembershipCohort {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id", nullable = false)
	private MembershipPlan plan;

	/** 기수(예: 3기 → 3). */
	@Column(nullable = false)
	private int cohortNumber;

	/** 모집 시작일. */
	@Column(nullable = false)
	private LocalDate recruitStartDate;

	/** 모집 종료일(해당일 포함). */
	@Column(nullable = false)
	private LocalDate recruitEndDate;

	/** 노출 대상 기수 여부. */
	@Column(nullable = false)
	private boolean active;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * 주어진 날짜가 모집 기간(시작일~종료일, 양끝 포함) 안에 있고 활성 기수인지 여부.
	 *
	 * @param today 기준 날짜
	 * @return 모집 중이면 true
	 */
	public boolean isRecruiting(LocalDate today) {
		return active
			&& !today.isBefore(recruitStartDate)
			&& !today.isAfter(recruitEndDate);
	}
}
