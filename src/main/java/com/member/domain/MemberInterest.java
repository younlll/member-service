package com.member.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_interests",
	uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "interest_type"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class MemberInterest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private InterestType interestType;

	@OneToMany(mappedBy = "memberInterest", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MemberInterestOption> options = new ArrayList<>();

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public MemberInterest(Member member, InterestType interestType) {
		this.member = member;
		this.interestType = interestType;
		this.options = new ArrayList<>();
	}

	public void addOption(InterestOption optionType) {
		boolean exists = this.options.stream()
			.anyMatch(option -> option.getOptionType() == optionType);

		if (exists) {
			return;
		}

		MemberInterestOption interestOption = MemberInterestOption.builder()
			.memberInterest(this)
			.optionType(optionType)
			.build();
		this.options.add(interestOption);
	}

	public void replaceOptions(List<InterestOption> newOptions) {
		this.options.clear();
		newOptions.forEach(this::addOption);
	}
}
