package com.yeolcheong.mub.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.domain.Membership;
import com.yeolcheong.mub.member.domain.MembershipStatus;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

	/**
	 * 주어진 상태의 멤버십 수를 센다. 상품 조회의 '현재 가입자 수'(ACTIVE)에 사용한다.
	 */
	long countByStatus(MembershipStatus status);
}
