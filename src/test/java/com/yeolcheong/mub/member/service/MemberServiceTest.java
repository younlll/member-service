package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.dto.MemberInfoResponse;
import com.yeolcheong.mub.member.dto.MemberSummaryResponse;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService")
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@InjectMocks
	private MemberService memberService;

	private Member testMember;
	private SnsUserInfoResponse kakaoUserInfo;

	@BeforeEach
	void setUp() {
		testMember = Member.builder()
			.id(1L)
			.snsProvider(SnsProvider.KAKAO)
			.socialId("1234567890")
			.email("test@example.com")
			.nickname("테스터")
			.status(MemberStatus.ACTIVE)
			.createdAt(LocalDateTime.now().minusDays(1))
			.lastLoginAt(LocalDateTime.now())
			.build();

		kakaoUserInfo = SnsUserInfoResponse.builder()
			.id(1234567890L)
			.connectedAt("2025-10-24T00:00:00Z")
			.kakaoAccount(SnsUserInfoResponse.KakaoAccount.builder()
				.email("test@example.com")
				.build())
			.build();
	}

	// =========================================================
	// findById
	// =========================================================
	@Nested
	@DisplayName("findById")
	class FindById {

		@Test
		@DisplayName("should return member when id exists")
		void shouldReturnMemberWhenIdExists() {
			// given
			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));

			// when
			Member result = memberService.findById(1L);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).isNotNull();
				softly.assertThat(result.getId()).isEqualTo(1L);
				softly.assertThat(result.getEmail()).isEqualTo("test@example.com");
				softly.assertThat(result.getSnsProvider()).isEqualTo(SnsProvider.KAKAO);
				softly.assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
			});
			verify(memberRepository, times(1)).findById(1L);
		}

		@Test
		@DisplayName("should throw MemberServiceApiException when id not found")
		void shouldThrowExceptionWhenIdNotFound() {
			// given
			given(memberRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> memberService.findById(999L))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.MEMBER_NOT_FOUND));

			verify(memberRepository, times(1)).findById(999L);
		}

		@ParameterizedTest(name = "should throw exception for edge-case id={0}")
		@DisplayName("should throw exception for edge-case ids")
		@ValueSource(longs = {-1L, 0L, Long.MAX_VALUE})
		void shouldThrowExceptionForEdgeCaseIds(long edgeId) {
			// given
			given(memberRepository.findById(edgeId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> memberService.findById(edgeId))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
		}
	}

	// =========================================================
	// withdraw
	// =========================================================
	@Nested
	@DisplayName("withdraw")
	class Withdraw {

		@Test
		@DisplayName("should soft-delete member and invalidate refresh token")
		void shouldSoftDeleteAndInvalidateToken() {
			// given
			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));

			// when
			memberService.withdraw(1L);

			// then
			assertThat(testMember.getStatus()).isEqualTo(MemberStatus.DELETED);
			verify(refreshTokenRepository, times(1)).deleteByMemberId(1L);
		}

		@Test
		@DisplayName("should throw MEMBER_NOT_FOUND when member does not exist")
		void shouldThrowWhenMemberNotFound() {
			// given
			given(memberRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> memberService.withdraw(999L))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
			verify(refreshTokenRepository, never()).deleteByMemberId(any());
		}

		@Test
		@DisplayName("should throw ALREADY_WITHDRAWN when member is already DELETED")
		void shouldThrowWhenAlreadyWithdrawn() {
			// given
			Member deleted = Member.builder()
				.id(2L)
				.snsProvider(SnsProvider.KAKAO)
				.socialId("2222")
				.email("gone@example.com")
				.status(MemberStatus.DELETED)
				.build();
			given(memberRepository.findById(2L)).willReturn(Optional.of(deleted));

			// when & then
			assertThatThrownBy(() -> memberService.withdraw(2L))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.ALREADY_WITHDRAWN));
			verify(refreshTokenRepository, never()).deleteByMemberId(any());
		}
	}

	// =========================================================
	// findBySocialId
	// =========================================================
	@Nested
	@DisplayName("findBySocialId")
	class FindBySocialId {

		@Test
		@DisplayName("should return member when valid socialId and provider given")
		void shouldReturnMemberWhenValidSocialIdAndProviderGiven() {
			// given
			given(memberRepository.findBySnsProviderAndSocialId(SnsProvider.KAKAO, "1234567890"))
				.willReturn(Optional.of(testMember));

			// when
			Optional<Member> result = memberService.findBySocialId(SnsProvider.KAKAO, "1234567890");

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).isPresent();
				softly.assertThat(result.get().getSocialId()).isEqualTo("1234567890");
				softly.assertThat(result.get().getSnsProvider()).isEqualTo(SnsProvider.KAKAO);
			});
			verify(memberRepository, times(1)).findBySnsProviderAndSocialId(SnsProvider.KAKAO, "1234567890");
		}

		@ParameterizedTest(name = "should return empty Optional for invalid socialId=\"{0}\"")
		@DisplayName("should return empty Optional for invalid or non-existent socialIds")
		@NullAndEmptySource
		@ValueSource(strings = {"unknown", "   ", "000000000"})
		void shouldReturnEmptyForInvalidSocialId(String invalidId) {
			// given
			given(memberRepository.findBySnsProviderAndSocialId(SnsProvider.KAKAO, invalidId))
				.willReturn(Optional.empty());

			// when
			Optional<Member> result = memberService.findBySocialId(SnsProvider.KAKAO, invalidId);

			// then
			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("should distinguish members with same socialId but different provider")
		void shouldDistinguishMembersByProvider() {
			// given
			String sameSocialId = "1234567890";
			given(memberRepository.findBySnsProviderAndSocialId(SnsProvider.KAKAO, sameSocialId))
				.willReturn(Optional.of(testMember));

			// when
			Optional<Member> kakaoResult = memberService.findBySocialId(SnsProvider.KAKAO, sameSocialId);

			// then
			assertThat(kakaoResult).isPresent();
		}
	}

	// =========================================================
	// createdFromSnsUser
	// =========================================================
	@Nested
	@DisplayName("createdFromSnsUser")
	class CreatedFromSnsUser {

		@Test
		@DisplayName("should create member with correct fields from kakao user info")
		void shouldCreateMemberWithCorrectFieldsFromKakaoUserInfo() {
			// given
			Member savedMember = Member.builder()
				.id(2L)
				.snsProvider(SnsProvider.KAKAO)
				.socialId("1234567890")
				.email("test@example.com")
				.status(MemberStatus.INACTIVE)
				.createdAt(LocalDateTime.now())
				.build();
			given(memberRepository.save(any(Member.class))).willReturn(savedMember);

			// when
			Member result = memberService.createdFromSnsUser(kakaoUserInfo);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result.getId()).isEqualTo(2L);
				softly.assertThat(result.getSnsProvider()).isEqualTo(SnsProvider.KAKAO);
				softly.assertThat(result.getSocialId()).isEqualTo("1234567890");
				softly.assertThat(result.getEmail()).isEqualTo("test@example.com");
				softly.assertThat(result.getStatus()).isEqualTo(MemberStatus.INACTIVE);
			});
			verify(memberRepository, times(1)).save(any(Member.class));
		}

		@Test
		@DisplayName("should always set INACTIVE status on new member")
		void shouldAlwaysSetInactiveStatusOnNewMember() {
			// given
			ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
			given(memberRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

			// when
			memberService.createdFromSnsUser(kakaoUserInfo);

			// then
			assertThat(captor.getValue().getStatus()).isEqualTo(MemberStatus.INACTIVE);
		}

		@Test
		@DisplayName("should always set KAKAO as snsProvider on new member")
		void shouldAlwaysSetKakaoAsSnsProvider() {
			// given
			ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
			given(memberRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

			// when
			memberService.createdFromSnsUser(kakaoUserInfo);

			// then
			assertThat(captor.getValue().getSnsProvider()).isEqualTo(SnsProvider.KAKAO);
		}

		@Test
		@DisplayName("should set lastLoginAt to approximately current time on creation")
		void shouldSetLastLoginAtToCurrentTimeOnCreation() {
			// given
			LocalDateTime before = LocalDateTime.now().minusSeconds(1);
			ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
			given(memberRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

			// when
			memberService.createdFromSnsUser(kakaoUserInfo);

			// then
			LocalDateTime after = LocalDateTime.now().plusSeconds(1);
			assertThat(captor.getValue().getLastLoginAt())
				.isAfterOrEqualTo(before)
				.isBeforeOrEqualTo(after);
		}

		@Test
		@DisplayName("should create member even when email is null")
		void shouldCreateMemberEvenWhenEmailIsNull() {
			// given
			SnsUserInfoResponse noEmailUserInfo = SnsUserInfoResponse.builder()
				.id(9999L)
				.connectedAt("2025-10-24T00:00:00Z")
				.kakaoAccount(SnsUserInfoResponse.KakaoAccount.builder().build())
				.build();

			Member savedMember = Member.builder()
				.id(3L)
				.snsProvider(SnsProvider.KAKAO)
				.socialId("9999")
				.email(null)
				.status(MemberStatus.INACTIVE)
				.build();
			given(memberRepository.save(any(Member.class))).willReturn(savedMember);

			// when
			Member result = memberService.createdFromSnsUser(noEmailUserInfo);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).isNotNull();
				softly.assertThat(result.getEmail()).isNull();
				softly.assertThat(result.getStatus()).isEqualTo(MemberStatus.INACTIVE);
			});
		}

		@Test
		@DisplayName("should propagate exception when repository save fails")
		void shouldPropagateExceptionWhenRepositorySaveFails() {
			// given
			given(memberRepository.save(any(Member.class)))
				.willThrow(new RuntimeException("DB 저장 실패"));

			// when & then
			assertThatThrownBy(() -> memberService.createdFromSnsUser(kakaoUserInfo))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("DB 저장 실패");
		}
	}

	// =========================================================
	// getMemberByEmail
	// =========================================================
	@Nested
	@DisplayName("getMemberByEmail")
	class GetMemberByEmail {

		@Test
		@DisplayName("should return MemberInfoResponse when email exists")
		void shouldReturnMemberInfoResponseWhenEmailExists() {
			// given
			given(memberRepository.findByEmail("test@example.com"))
				.willReturn(Optional.of(testMember));

			// when
			MemberInfoResponse result = memberService.getMemberByEmail("test@example.com");

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).isNotNull();
				softly.assertThat(result.getMemberId()).isEqualTo(testMember.getId());
				softly.assertThat(result.getEmail()).isEqualTo("test@example.com");
			});
			verify(memberRepository, times(1)).findByEmail("test@example.com");
		}

		@Test
		@DisplayName("should throw MemberServiceApiException when email not found")
		void shouldThrowExceptionWhenEmailNotFound() {
			// given
			given(memberRepository.findByEmail("notfound@example.com"))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> memberService.getMemberByEmail("notfound@example.com"))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.MEMBER_NOT_FOUND));

			verify(memberRepository, times(1)).findByEmail("notfound@example.com");
		}

		@ParameterizedTest(name = "should throw exception for invalid email=\"{0}\"")
		@DisplayName("should throw exception for invalid emails")
		@NullAndEmptySource
		@ValueSource(strings = {"notanemail", "   ", "@nodomain", "missing@"})
		void shouldThrowExceptionForInvalidEmails(String invalidEmail) {
			// given
			given(memberRepository.findByEmail(invalidEmail)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> memberService.getMemberByEmail(invalidEmail))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
		}

		@Test
		@DisplayName("should treat email lookup as case-sensitive")
		void shouldTreatEmailLookupAsCaseSensitive() {
			// given
			given(memberRepository.findByEmail("Test@Example.com")).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> memberService.getMemberByEmail("Test@Example.com"))
				.isInstanceOf(MemberServiceApiException.class);

			// 원본 소문자 이메일로는 조회하지 않음
			verify(memberRepository, never()).findByEmail("test@example.com");
		}
	}

	// =========================================================
	// findSummariesByIds — bulk lookup for service-to-service
	// =========================================================
	@Nested
	@DisplayName("findSummariesByIds")
	class FindSummariesByIds {

		@Test
		@DisplayName("should return summaries for all existing ids")
		void shouldReturnSummariesForAllExistingIds() {
			// given
			Member other = Member.builder()
				.id(2L)
				.snsProvider(SnsProvider.KAKAO)
				.socialId("9999")
				.email("other@example.com")
				.nickname("아더")
				.status(MemberStatus.ACTIVE)
				.build();
			List<Long> ids = List.of(1L, 2L);
			given(memberRepository.findAllById(ids)).willReturn(List.of(testMember, other));

			// when
			List<MemberSummaryResponse> result = memberService.findSummariesByIds(ids);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).hasSize(2);
				softly.assertThat(result).extracting(MemberSummaryResponse::getMemberId)
					.containsExactly(1L, 2L);
				softly.assertThat(result).extracting(MemberSummaryResponse::getNickname)
					.containsExactly("테스터", "아더");
				softly.assertThat(result).extracting(MemberSummaryResponse::getEmail)
					.containsExactly("test@example.com", "other@example.com");
			});
			verify(memberRepository, times(1)).findAllById(ids);
		}

		@Test
		@DisplayName("should silently drop ids that do not exist")
		void shouldSilentlyDropMissingIds() {
			// given — request 3 ids but DB only has 1
			List<Long> ids = List.of(1L, 99L, 100L);
			given(memberRepository.findAllById(ids)).willReturn(List.of(testMember));

			// when
			List<MemberSummaryResponse> result = memberService.findSummariesByIds(ids);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).hasSize(1);
				softly.assertThat(result.get(0).getMemberId()).isEqualTo(1L);
			});
		}

		@Test
		@DisplayName("should return empty list when ids is null")
		void shouldReturnEmptyListWhenIdsIsNull() {
			// when
			List<MemberSummaryResponse> result = memberService.findSummariesByIds(null);

			// then
			assertThat(result).isEmpty();
			verify(memberRepository, never()).findAllById(any());
		}

		@Test
		@DisplayName("should return empty list when ids is empty")
		void shouldReturnEmptyListWhenIdsIsEmpty() {
			// when
			List<MemberSummaryResponse> result = memberService.findSummariesByIds(Collections.emptyList());

			// then
			assertThat(result).isEmpty();
			verify(memberRepository, never()).findAllById(any());
		}
	}
}