package com.yeolcheong.mub.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.domain.MembershipCohort;
import com.yeolcheong.mub.member.domain.MembershipPlan;

@Repository
public interface MembershipCohortRepository extends JpaRepository<MembershipCohort, Long> {

	/**
	 * 상품의 가장 최근 기수(cohortNumber 내림차순 첫 번째)를 조회한다.
	 */
	Optional<MembershipCohort> findFirstByPlanOrderByCohortNumberDesc(MembershipPlan plan);
}
