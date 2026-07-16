package com.yeolcheong.mub.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.domain.MemberTermsAgreement;
import com.yeolcheong.mub.member.domain.TermsType;

@Repository
public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {

	void deleteByMemberId(Long memberId);

	Optional<MemberTermsAgreement> findByMemberIdAndTermsType(Long memberId, TermsType termsType);
}
