package com.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.member.domain.MemberInterest;

@Repository
public interface MemberInterestRepository extends JpaRepository<MemberInterest, Long> {

	void deleteByMemberId(Long memberId);
}
