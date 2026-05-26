package com.yeolcheong.mub.member.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeolcheong.mub.member.dto.MemberSummaryResponse;
import com.yeolcheong.mub.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service-to-service endpoints under /api/internal/**.
 * Not intended for browser/mobile clients — should be reachable only
 * from inside the cluster (gateway/ingress must not expose this path).
 */
@RestController
@RequestMapping("/api/internal/members")
@RequiredArgsConstructor
@Slf4j
public class InternalMemberController {

	private final MemberService memberService;

	/**
	 * Look up multiple members by id in a single request.
	 * Used by other services (e.g. space-service) to enrich their own
	 * records with nickname/email without making N requests.
	 *
	 * @param ids member id list, passed as repeated query params or comma-separated
	 * @return summaries for the ids that exist (silently drops missing ones)
	 */
	@GetMapping
	public ResponseEntity<List<MemberSummaryResponse>> findByIds(@RequestParam List<Long> ids) {
		log.info("Internal member lookup: idsSize={}", ids.size());
		return ResponseEntity.ok(memberService.findSummariesByIds(ids));
	}
}
