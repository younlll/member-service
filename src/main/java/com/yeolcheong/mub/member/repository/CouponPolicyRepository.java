package com.yeolcheong.mub.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeolcheong.mub.member.domain.CouponPolicy;

@Repository
public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {
}
