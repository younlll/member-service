package com.yeolcheong.mub.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.common.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	/**
 * Finds a Member by SNS provider and the provider-specific social identifier.
 *
 * @param snsProvider the social network provider associated with the member
 * @param socialId the provider-specific user identifier for the member
 * @return an Optional containing the matching Member if present, or Optional.empty() otherwise
 */
Optional<Member> findBySnsProviderAndSocialId(SnsProvider snsProvider, String socialId);

	/**
 * Finds a Member by their email address.
 *
 * @param email the member's email address to search for
 * @return an Optional containing the matching Member if one exists, or Optional.empty() otherwise
 */
Optional<Member> findByEmail(String email);
}
