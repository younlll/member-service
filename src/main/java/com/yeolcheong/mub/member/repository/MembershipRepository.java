package com.yeolcheong.mub.member.repository;

import java.util.Optional;

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

	/**
	 * 스토어 거래 식별자로 멤버십을 조회한다. 구매검증 멱등(중복 검증 시 기존 반영)에 사용한다.
	 */
	Optional<Membership> findByStoreTransactionId(String storeTransactionId);

	/**
	 * 회원의 특정 상태 멤버십 중 만료일이 가장 늦은 것을 조회한다. '내 멤버십'(ACTIVE)에 사용한다.
	 */
	Optional<Membership> findFirstByMemberIdAndStatusOrderByExpiresAtDesc(Long memberId, MembershipStatus status);
}
