package com.member.domain;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_terms_agreements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class MemberTermsAgreement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private TermsType termsType;

	@Column(nullable = false)
	private Boolean isRequired;

	@Column(nullable = false)
	private Boolean agreed;

	@Column
	private LocalDateTime agreedAt;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public MemberTermsAgreement(Member member, TermsType termsType, Boolean agreed) {
		this.member = member;
		this.termsType = termsType;
		this.isRequired = termsType.isRequired();
		this.agreed = agreed;
		this.agreedAt = agreed ? LocalDateTime.now() : null;
	}

	public void updateAgreed(Boolean agreed) {
		this.agreed = agreed;
		this.agreedAt = agreed ? LocalDateTime.now() : null;
	}
}
