package com.yeolcheong.mub.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findBySnsProviderAndSocialId(SnsProvider snsProvider, String socialId);

	Optional<Member> findByEmail(String email);

	/**
	 * 해당 id 의 회원이 존재하고 주어진 상태가 아닌지 여부.
	 * 인증 필터에서 탈퇴(DELETED) 회원을 즉시 차단하는 데 사용한다(엔티티 미로딩, 경량 조회).
	 */
	boolean existsByIdAndStatusNot(Long id, MemberStatus status);
}
