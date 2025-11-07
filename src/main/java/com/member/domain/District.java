package com.member.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "districts",
	uniqueConstraints = @UniqueConstraint(columnNames = {"dist_code1", "dist_code2"}))
@Getter
@NoArgsConstructor
public class District {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 10)
	private String distCode1;

	@Column(name = "dist_code1_name", nullable = false, length = 50)
	private String distCode1Name;

	@Column(nullable = false, length = 10)
	private String distCode2;

	@Column(name = "dist_code2_name", nullable = false, length = 50)
	private String distCode2Name;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public District(String distCode1, String distCode1Name, String distCode2, String distCode2Name) {
		this.distCode1 = distCode1;
		this.distCode1Name = distCode1Name;
		this.distCode2 = distCode2;
		this.distCode2Name = distCode2Name;
	}
}
