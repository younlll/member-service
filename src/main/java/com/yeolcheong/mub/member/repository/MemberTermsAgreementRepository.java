package com.yeolcheong.mub.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.domain.MemberTermsAgreement;

@Repository
public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {

	void deleteByMemberId(Long memberId);
}
