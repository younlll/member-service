package com.yeolcheong.mub.member.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.domain.MemberInterest;

@Repository
public interface MemberInterestRepository extends JpaRepository<MemberInterest, Long> {

	void deleteByMemberId(Long memberId);

	List<MemberInterest> findAllByMemberId(Long memberId);
}
