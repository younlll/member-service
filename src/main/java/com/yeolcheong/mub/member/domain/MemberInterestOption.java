package com.yeolcheong.mub.member.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
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

@Entity
@Table(name = "member_interest_options",
	uniqueConstraints = @UniqueConstraint(columnNames = {"member_interest_id", "option_type"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class MemberInterestOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_interest_id", nullable = false)
	private MemberInterest memberInterest;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private InterestOption optionType;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public MemberInterestOption(MemberInterest memberInterest, InterestOption optionType) {
		this.memberInterest = memberInterest;
		this.optionType = optionType;
	}
}
