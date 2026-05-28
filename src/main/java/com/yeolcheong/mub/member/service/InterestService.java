package com.yeolcheong.mub.member.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yeolcheong.mub.member.dto.InterestResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestService {

	public List<InterestResponse> getAllInterests() {
		log.debug("관심사 목록 조회");
		return InterestResponse.getAllInterests();
	}
}
