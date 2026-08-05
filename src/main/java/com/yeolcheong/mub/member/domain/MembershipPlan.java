package com.yeolcheong.mub.member.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버십 상품(머브크루). 스토어에 등록된 구독 상품 1개에 대응하며, 기수는 이 상품에 붙는 라벨이다.
 * 상품 ID는 스토어 콘솔 등록값과 동일해야 하며 iOS/Android 가 서로 다를 수 있다.
 */
@Entity
@Table(name = "membership_plans")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MembershipPlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 상품명(예: 머브크루). */
	@Column(nullable = false, length = 50)
	private String name;

	/** 월 구독 금액(원). */
	@Column(nullable = false)
	private int monthlyPrice;

	/** App Store Connect 등록 상품 ID(예: com.mub.app.membership.monthly). */
	@Column(nullable = false, length = 120)
	private String iosProductId;

	/** Google Play Console 등록 상품 ID(예: mub_membership_monthly). */
	@Column(nullable = false, length = 120)
	private String androidProductId;

	/** 혜택 목록(화면 노출용). */
	@ElementCollection
	@CollectionTable(name = "membership_plan_benefits", joinColumns = @JoinColumn(name = "plan_id"))
	@Column(name = "benefit", nullable = false, length = 200)
	@Builder.Default
	private List<String> benefits = new ArrayList<>();

	/** 현재 판매 중인 상품 여부. */
	@Column(nullable = false)
	private boolean active;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
