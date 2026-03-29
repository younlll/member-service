package com.yeolcheong.mub.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.common.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findBySnsProviderAndSocialId(SnsProvider snsProvider, String socialId);

	Optional<Member> findByEmail(String email);
}
