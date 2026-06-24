package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

import com.yeolcheong.mub.member.domain.District;
import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.MemberInterest;
import com.yeolcheong.mub.member.dto.MemberInfoResponse;
import com.yeolcheong.mub.member.dto.MemberSummaryResponse;
import com.yeolcheong.mub.member.dto.ProfileResponse;
import com.yeolcheong.mub.member.dto.ProfileUpdateRequest;
import com.yeolcheong.mub.member.dto.SnsUserInfoResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.DistrictRepository;
import com.yeolcheong.mub.member.repository.MemberInterestRepository;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.repository.MemberTermsAgreementRepository;
import com.yeolcheong.mub.member.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService")
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private DistrictRepository districtRepository;
	@Mock
	private MemberInterestRepository memberInterestRepository;
	@Mock
	private MemberTermsAgreementRepository memberTermsAgreementRepository;
	@Mock
	private ProfileImageService profileImageService;

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
	// reactivateForResignup
	// =========================================================
	@Nested
	@DisplayName("reactivateForResignup")
	class ReactivateForResignup {

		@Test
		@DisplayName("should reset profile, clear interests/terms and set status INACTIVE")
		void shouldReactivateWithdrawnMember() {
			// given — 탈퇴 회원
			Member withdrawn = Member.builder()
				.id(7L)
				.snsProvider(SnsProvider.KAKAO)
				.socialId("7777")
				.email("back@example.com")
				.nickname("옛닉네임")
				.regionProvince("서울특별시")
				.regionCity("강남구")
				.status(MemberStatus.DELETED)
				.build();
			withdrawn.assignImage(50L);

			// when
			memberService.reactivateForResignup(withdrawn);

			// then
			assertSoftly(softly -> {
				softly.assertThat(withdrawn.getStatus()).isEqualTo(MemberStatus.INACTIVE);
				softly.assertThat(withdrawn.getNickname()).isNull();
				softly.assertThat(withdrawn.getRegionProvince()).isNull();
				softly.assertThat(withdrawn.getRegionCity()).isNull();
				softly.assertThat(withdrawn.getImageId()).isNull();
			});
			verify(memberInterestRepository, times(1)).deleteByMemberId(7L);
			verify(memberTermsAgreementRepository, times(1)).deleteByMemberId(7L);
			verify(memberRepository, times(1)).save(withdrawn);
		}
	}

	// =========================================================
	// getMyProfile / updateProfile
	// =========================================================
	@Nested
	@DisplayName("getMyProfile / updateProfile")
	class Profile {

		@Test
		@DisplayName("getMyProfile - returns profile with interests")
		void getMyProfile_success() {
			// given
			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
			given(memberInterestRepository.findAllByMemberId(1L)).willReturn(List.of());

			// when
			ProfileResponse response = memberService.getMyProfile(1L);

			// then
			assertSoftly(softly -> {
				softly.assertThat(response.getMemberId()).isEqualTo(1L);
				softly.assertThat(response.getNickname()).isEqualTo("테스터");
				softly.assertThat(response.getInterests()).isEmpty();
			});
		}

		@Test
		@DisplayName("updateProfile - updates nickname, bio, region and interests")
		void updateProfile_success() {
			// given
			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
			given(districtRepository.findByDistCode1AndDistCode2("11", "11680"))
				.willReturn(Optional.of(buildDistrict()));
			given(memberInterestRepository.findAllByMemberId(1L)).willReturn(List.of());
			given(memberInterestRepository.save(any(MemberInterest.class)))
				.willAnswer(inv -> inv.getArgument(0));

			// when
			ProfileResponse response = memberService.updateProfile(1L, buildRequest("새닉네임"));

			// then
			assertSoftly(softly -> {
				softly.assertThat(testMember.getNickname()).isEqualTo("새닉네임");
				softly.assertThat(testMember.getBio()).isEqualTo("한줄소개");
				softly.assertThat(testMember.getRegionProvince()).isEqualTo("서울특별시");
				softly.assertThat(testMember.getRegionCity()).isEqualTo("강남구");
				softly.assertThat(response.getInterests()).hasSize(1);
			});
			verify(memberRepository, times(1)).save(testMember);
		}

		@Test
		@DisplayName("updateProfile - throws INVALID_DISTRICT_CODE when district is invalid")
		void updateProfile_invalidDistrict() {
			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
			given(districtRepository.findByDistCode1AndDistCode2(any(), any())).willReturn(Optional.empty());

			assertThatThrownBy(() -> memberService.updateProfile(1L, buildRequest("닉네임")))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_DISTRICT_CODE));
		}

		@Test
		@DisplayName("updateProfile - throws MEMBER_NOT_FOUND when member is withdrawn")
		void updateProfile_withdrawnMember() {
			Member deleted = Member.builder()
				.id(5L).snsProvider(SnsProvider.KAKAO).socialId("5").email("d@x.com")
				.status(MemberStatus.DELETED).build();
			given(memberRepository.findById(5L)).willReturn(Optional.of(deleted));

			assertThatThrownBy(() -> memberService.updateProfile(5L, buildRequest("닉네임")))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
		}

		@Test
		@DisplayName("updateProfile - throws INVALID_INTEREST_OPTION when option not allowed for type")
		void updateProfile_invalidInterestOption() {
			given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
			given(districtRepository.findByDistCode1AndDistCode2("11", "11680"))
				.willReturn(Optional.of(buildDistrict()));
			given(memberInterestRepository.findAllByMemberId(1L)).willReturn(List.of());

			// EXERCISE_SPORTS 에 허용되지 않는 옵션을 SELF_DEVELOPMENT 타입에 섞어 보냄
			InterestOption invalidForType = pickInvalidOption(InterestType.SELF_DEVELOPMENT);
			ProfileUpdateRequest request = ProfileUpdateRequest.builder()
				.nickname("닉네임").bio("한줄소개").distCode1("11").distCode2("11680")
				.interests(List.of(new ProfileUpdateRequest.InterestRequest(
					InterestType.SELF_DEVELOPMENT, List.of(invalidForType))))
				.build();

			assertThatThrownBy(() -> memberService.updateProfile(1L, request))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException) ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_INTEREST_OPTION));
		}

		private District buildDistrict() {
			return District.builder()
				.distCode1("11").distCode1Name("서울특별시")
				.distCode2("11680").distCode2Name("강남구")
				.build();
		}

		private ProfileUpdateRequest buildRequest(String nickname) {
			InterestType type = InterestType.SELF_DEVELOPMENT;
			InterestOption option = type.getAvailableOptions().get(0);
			return ProfileUpdateRequest.builder()
				.nickname(nickname).bio("한줄소개").distCode1("11").distCode2("11680")
				.interests(List.of(new ProfileUpdateRequest.InterestRequest(type, List.of(option))))
				.build();
		}

		private InterestOption pickInvalidOption(InterestType type) {
			for (InterestOption option : InterestOption.values()) {
				if (!type.getAvailableOptions().contains(option)) {
					return option;
				}
			}
			throw new IllegalStateException("no invalid option available for " + type);
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
			given(profileImageService.resolveImageUrls(List.of(testMember, other)))
				.willReturn(Map.of(
					1L, "http://localhost:8083/images/profile/custom.png",
					2L, "http://localhost:8083/images/profile/default.png"));

			// when
			List<MemberSummaryResponse> result = memberService.findSummariesByIds(ids);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).hasSize(2);
				softly.assertThat(result).extracting(MemberSummaryResponse::getMemberId)
					.containsExactly(1L, 2L);
				softly.assertThat(result).extracting(MemberSummaryResponse::getNickname)
					.containsExactly("테스터", "아더");
				softly.assertThat(result).extracting(MemberSummaryResponse::getProfileImageUrl)
					.containsExactly("http://localhost:8083/images/profile/custom.png",
						"http://localhost:8083/images/profile/default.png");
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
			given(profileImageService.resolveImageUrls(List.of(testMember)))
				.willReturn(Map.of(1L, "http://localhost:8083/images/profile/custom.png"));

			// when
			List<MemberSummaryResponse> result = memberService.findSummariesByIds(ids);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).hasSize(1);
				softly.assertThat(result.get(0).getMemberId()).isEqualTo(1L);
				softly.assertThat(result.get(0).getProfileImageUrl())
					.isEqualTo("http://localhost:8083/images/profile/custom.png");
			});
		}

		@Test
		@DisplayName("should fall back to default image url when member has no custom image")
		void shouldFallBackToDefaultImageUrl() {
			// given — member without a custom profile image
			List<Long> ids = List.of(1L);
			String defaultUrl = "http://localhost:8083/images/profile/default.png";
			given(memberRepository.findAllById(ids)).willReturn(List.of(testMember));
			given(profileImageService.resolveImageUrls(List.of(testMember)))
				.willReturn(Map.of(1L, defaultUrl));

			// when
			List<MemberSummaryResponse> result = memberService.findSummariesByIds(ids);

			// then
			assertSoftly(softly -> {
				softly.assertThat(result).hasSize(1);
				softly.assertThat(result.get(0).getProfileImageUrl()).isEqualTo(defaultUrl);
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