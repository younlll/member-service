package com.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.member.domain.District;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

	/**
	 * 특정 지역 조회
	 */
	Optional<District> findByDistCode1AndDistCode2(String distCode1, String distCode2);

	/**
	 * 모든 시/도 목록 조회
	 * @return District1 List
	 */
	@Query("SELECT DISTINCT d.distCode1, d.distCode1Name FROM District d ORDER BY d.distCode1")
	List<Object[]> findDistinctDistCode1();

	/**
	 * 특정 시/도의 시/군/구 목록 조회
	 * @param distCode1
	 * @return District2 List
	 */
	@Query("SELECT d FROM District d WHERE d.distCode1 = :distCode1 ORDER BY d.distCode2")
	List<District> findByDistCode1(String distCode1);
}
