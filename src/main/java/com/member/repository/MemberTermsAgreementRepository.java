package com.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.member.domain.MemberTermsAgreement;

@Repository
public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {

}
