package com.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.member.dto.InterestResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InterestService {

	public List<InterestResponse> getAllInterests() {
		log.debug("관심사 목록 조회");
		return InterestResponse.getAllInterests();
	}
}
