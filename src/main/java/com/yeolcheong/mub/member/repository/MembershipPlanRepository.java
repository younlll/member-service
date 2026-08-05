package com.yeolcheong.mub.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.domain.MembershipPlan;

@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

	/**
	 * 현재 판매 중인 멤버십 상품을 조회한다. 운영상 활성 상품은 1개를 전제로 한다.
	 */
	Optional<MembershipPlan> findFirstByActiveTrue();
}
