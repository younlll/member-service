package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.yeolcheong.mub.member.client.GroupClient;
import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.domain.SnsProvider;
import com.yeolcheong.mub.member.domain.District;
import com.yeolcheong.mub.member.domain.InterestOption;
import com.yeolcheong.mub.member.domain.InterestType;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.MemberInterest;
import com.yeolcheong.mub.member.domain.MemberProfileImage;
import com.yeolcheong.mub.member.domain.MemberTermsAgreement;
import com.yeolcheong.mub.member.domain.TermsType;
import com.yeolcheong.mub.member.dto.OnboardingRequest;
import com.yeolcheong.mub.member.dto.OnboardingResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.exception.OnboardingServiceApiException;
import com.yeolcheong.mub.member.repository.DistrictRepository;
import com.yeolcheong.mub.member.repository.MemberInterestRepository;
import com.yeolcheong.mub.member.repository.MemberProfileImageRepository;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.repository.MemberTermsAgreementRepository;
import com.yeolcheong.mub.member.storage.ProfileImageStorage;
import com.yeolcheong.mub.member.storage.StoredImage;

@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingService")
class OnboardingServiceTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private MemberTermsAgreementRepository memberTermsAgreementRepository;
	@Mock
	private DistrictRepository districtRepository;
	@Mock
	private MemberInterestRepository memberInterestRepository;
	@Mock
	private MemberProfileImageRepository memberProfileImageRepository;
	@Mock
	private ProfileImageStorage profileImageStorage;
	@Mock
	private GroupClient groupClient;

	@InjectMocks
	private OnboardingService onboardingService;

	private Member inactiveMember;
	private District seoulGangnam;

	@BeforeEach
	void setUp() {
		inactiveMember = Member.builder()
			.id(1L)
			.snsProvider(SnsProvider.KAKAO)
			.socialId("1234567890")
			.email("test@kakao.com")
			.status(MemberStatus.INACTIVE)
			.build();

		seoulGangnam = District.builder()
			.distCode1("11")
			.distCode1Name("서울특별시")
			.distCode2("11680")
			.distCode2Name("강남구")
			.build();
	}

	private void setupCommonMocks() {
		given(memberRepository.findById(1L)).willReturn(Optional.of(inactiveMember));
		given(memberTermsAgreementRepository.saveAll(anyList())).willReturn(List.of());
		given(districtRepository.findByDistCode1AndDistCode2("11", "11680"))
			.willReturn(Optional.of(seoulGangnam));
		given(memberInterestRepository.save(any(MemberInterest.class)))
			.willReturn(MemberInterest.builder().build());
	}

	private void setupTermsAndDistrictMocks() {
		given(memberRepository.findById(1L)).willReturn(Optional.of(inactiveMember));
		given(memberTermsAgreementRepository.saveAll(anyList())).willReturn(List.of());
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString()))
			.willReturn(Optional.of(seoulGangnam));
	}

	private OnboardingRequest buildValidRequest(String nickname, int interestCount) {
		return OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, false))
			.nickname(nickname)
			.distCode1("11")
			.distCode2("11680")
			.interests(buildInterests(interestCount))
			.build();
	}

	private OnboardingRequest buildRequestWithTerms(OnboardingRequest.TermsAgreementRequest termsRequest) {
		return OnboardingRequest.builder()
			.termsAgreementRequest(termsRequest)
			.nickname("닉네임")
			.distCode1("11").distCode2("11680")
			.interests(buildInterests(1))
			.build();
	}

	private OnboardingRequest buildRequestWithDistrict(String distCode1, String distCode2) {
		return OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, false))
			.nickname("닉네임")
			.distCode1(distCode1).distCode2(distCode2)
			.interests(buildInterests(1))
			.build();
	}

	// =========================================================
	// Helper
	// =========================================================

	private OnboardingRequest buildRequestWithInterests(List<OnboardingRequest.InterestRequest> interests) {
		return OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, false))
			.nickname("닉네임")
			.distCode1("11").distCode2("11680")
			.interests(interests)
			.build();
	}

	/**
	 * 사용 가능한 InterestType 목록에서 순서대로 count개를 뽑아 관심사 리스트 생성
	 * InterestType이 최소 3개 이상이어야 count=3까지 지원 가능
	 */
	private List<OnboardingRequest.InterestRequest> buildInterests(int count) {
		InterestType[] types = InterestType.values();
		return java.util.stream.IntStream.range(0, count)
			.mapToObj(i -> {
				InterestType type = types[i % types.length];
				InterestOption firstOption = type.getAvailableOptions().get(0);
				return new OnboardingRequest.InterestRequest(type, List.of(firstOption));
			})
			.toList();
	}

	// =========================================================
	// completeOnboarding — 성공 케이스
	// =========================================================
	@Nested
	@DisplayName("completeOnboarding - success")
	class CompleteOnboardingSuccess {

		@Test
		@DisplayName("should complete onboarding and return memberId and nickname")
		void shouldCompleteOnboardingAndReturnMemberIdAndNickname() {
			// given
			setupCommonMocks();
			OnboardingRequest request = buildValidRequest("테스트유저", 1);

			// when
			OnboardingResponse response = onboardingService.completeOnboarding(1L, request, null);

			// then
			assertSoftly(softly -> {
				softly.assertThat(response).isNotNull();
				softly.assertThat(response.getMemberId()).isEqualTo(1L);
				softly.assertThat(response.getNickname()).isEqualTo("테스트유저");
				softly.assertThat(response.getMessage()).isEqualTo("회원가입이 완료되었습니다");
			});
		}

		@Test
		@DisplayName("should store profile image and link it to member when image is provided")
		void shouldStoreProfileImageWhenProvided() {
			// given
			setupCommonMocks();
			MockMultipartFile image = new MockMultipartFile(
				"profileImage", "profileSample1.png", "image/png", new byte[] {1, 2, 3});
			given(profileImageStorage.store(image))
				.willReturn(new StoredImage("profileSample1.png", "uuid.png", "profile/uuid.png", "image/png", 3L));
			given(memberProfileImageRepository.save(any(MemberProfileImage.class)))
				.willReturn(MemberProfileImage.builder().id(100L).storedFileName("uuid.png").filePath("profile/uuid.png").build());

			// when
			onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 1), image);

			// then
			then(profileImageStorage).should(times(1)).store(image);
			assertThat(inactiveMember.getImageId()).isEqualTo(100L);
		}

		@Test
		@DisplayName("should not touch storage when no profile image is provided")
		void shouldSkipProfileImageWhenAbsent() {
			// given
			setupCommonMocks();

			// when
			onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 1), null);

			// then
			then(profileImageStorage).shouldHaveNoInteractions();
			assertThat(inactiveMember.getImageId()).isNull();
		}

		@Test
		@DisplayName("should set member status to ACTIVE after onboarding")
		void shouldSetMemberStatusToActiveAfterOnboarding() {
			// given
			setupCommonMocks();

			// when
			onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 1), null);

			// then
			assertThat(inactiveMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
			then(memberRepository).should(times(1)).save(inactiveMember);
		}

		@Test
		@DisplayName("should update member nickname after onboarding")
		void shouldUpdateMemberNicknameAfterOnboarding() {
			// given
			setupCommonMocks();

			// when
			onboardingService.completeOnboarding(1L, buildValidRequest("새닉네임", 1), null);

			// then
			assertThat(inactiveMember.getNickname()).isEqualTo("새닉네임");
		}

		@Test
		@DisplayName("should update member region after onboarding")
		void shouldUpdateMemberRegionAfterOnboarding() {
			// given
			setupCommonMocks();

			// when
			onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 1), null);

			// then
			assertSoftly(softly -> {
				softly.assertThat(inactiveMember.getRegionProvince()).isEqualTo("서울특별시");
				softly.assertThat(inactiveMember.getRegionCity()).isEqualTo("강남구");
			});
		}

		@ParameterizedTest(name = "should complete onboarding with {0} interest(s)")
		@DisplayName("should complete onboarding with 1 to 3 interests")
		@ValueSource(ints = {1, 2, 3})
		void shouldCompleteOnboardingWithOneToThreeInterests(int interestCount) {
			// given
			setupCommonMocks();
			OnboardingRequest request = buildValidRequest("닉네임", interestCount);

			// when
			OnboardingResponse response = onboardingService.completeOnboarding(1L, request, null);

			// then
			assertThat(response).isNotNull();
			then(memberInterestRepository).should(times(interestCount)).save(any(MemberInterest.class));
		}

		@Test
		@DisplayName("should save all terms agreements including optional marketing")
		void shouldSaveAllTermsAgreementsIncludingOptionalMarketing() {
			// given
			setupCommonMocks();
			OnboardingRequest request = OnboardingRequest.builder()
				.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, true))
				.nickname("닉네임")
				.distCode1("11").distCode2("11680")
				.interests(buildInterests(1))
				.build();

			// when
			onboardingService.completeOnboarding(1L, request, null);

			// then
			then(memberTermsAgreementRepository).should().saveAll(argThat(list -> {
				List<MemberTermsAgreement> agreements = (List<MemberTermsAgreement>)list;
				return agreements.stream().anyMatch(a ->
					a.getTermsType() == TermsType.MARKETING && a.getAgreed()
				);
			}));
		}

		@Test
		@DisplayName("should save marketing as false when marketing terms not agreed")
		void shouldSaveMarketingAsFalseWhenMarketingTermsNotAgreed() {
			// given
			setupCommonMocks();
			OnboardingRequest request = OnboardingRequest.builder()
				.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, false))
				.nickname("닉네임")
				.distCode1("11").distCode2("11680")
				.interests(buildInterests(1))
				.build();

			// when
			onboardingService.completeOnboarding(1L, request, null);

			// then
			then(memberTermsAgreementRepository).should().saveAll(argThat(list -> {
				List<MemberTermsAgreement> agreements = (List<MemberTermsAgreement>)list;
				return agreements.stream().anyMatch(a ->
					a.getTermsType() == TermsType.MARKETING && !a.getAgreed()
				);
			}));
		}

		@Test
		@DisplayName("should delete existing interests before saving new ones")
		void shouldDeleteExistingInterestsBeforeSavingNewOnes() {
			// given
			setupCommonMocks();

			// when
			onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 2), null);

			// then — deleteByMemberId가 save보다 먼저 호출되어야 함
			var inOrder = inOrder(memberInterestRepository);
			inOrder.verify(memberInterestRepository).deleteByMemberId(1L);
			inOrder.verify(memberInterestRepository, times(2)).save(any(MemberInterest.class));
		}

		@Test
		@DisplayName("should issue welcome activity voucher after onboarding completes")
		void shouldIssueWelcomeVoucherAfterOnboarding() {
			// given
			setupCommonMocks();

			// when
			onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 1), null);

			// then
			then(groupClient).should(times(1)).issueWelcomeVoucher(1L);
		}

		@Test
		@DisplayName("should complete onboarding even when welcome voucher issuance fails")
		void shouldCompleteOnboardingEvenWhenVoucherIssuanceFails() {
			// given
			setupCommonMocks();
			willThrow(new RuntimeException("issue failed")).given(groupClient).issueWelcomeVoucher(1L);

			// when
			OnboardingResponse response = onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 1), null);

			// then — 쿠폰 발급 실패가 가입을 막지 않는다
			assertThat(response).isNotNull();
			assertThat(inactiveMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		}
	}

	// =========================================================
	// completeOnboarding — 회원 조회 실패
	// =========================================================
	@Nested
	@DisplayName("completeOnboarding - member validation")
	class MemberValidation {

		@Test
		@DisplayName("should throw MEMBER_NOT_FOUND when member does not exist")
		void shouldThrowMemberNotFoundWhenMemberDoesNotExist() {
			// given
			given(memberRepository.findById(999L)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(999L, buildValidRequest("닉네임", 1), null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.MEMBER_NOT_FOUND));

			then(memberTermsAgreementRepository).should(never()).saveAll(anyList());
		}

		@ParameterizedTest(name = "should throw ALREADY_ONBOARDED when member status is {0}")
		@DisplayName("should throw ALREADY_ONBOARDED for ACTIVE and MUBACTIVE members")
		@MethodSource("com.yeolcheong.mub.member.service.OnboardingServiceTest#alreadyOnboardedStatuses")
		void shouldThrowAlreadyOnboardedForActiveAndMubActiveMembers(MemberStatus status) {
			// given
			Member alreadyOnboarded = Member.builder().id(1L).status(status).build();
			given(memberRepository.findById(1L)).willReturn(Optional.of(alreadyOnboarded));

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 1), null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.ALREADY_ONBOARDED));

			then(memberTermsAgreementRepository).should(never()).saveAll(anyList());
		}
	}

	// =========================================================
	// completeOnboarding — 약관 동의 검증
	// =========================================================
	@Nested
	@DisplayName("completeOnboarding - terms agreement validation")
	class TermsAgreementValidation {

		@ParameterizedTest(name = "should throw TERMS_AGREEMENT_REQUIRED when {0} is not agreed")
		@DisplayName("should throw TERMS_AGREEMENT_REQUIRED for each required terms")
		@MethodSource("com.yeolcheong.mub.member.service.OnboardingServiceTest#requiredTermsRequests")
		void shouldThrowTermsAgreementRequiredForEachRequiredTerms(
			String description, OnboardingRequest.TermsAgreementRequest termsRequest
		) {
			// given
			given(memberRepository.findById(1L)).willReturn(Optional.of(inactiveMember));
			OnboardingRequest request = buildRequestWithTerms(termsRequest);

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, request, null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> {
					MemberServiceApiException apiEx = (MemberServiceApiException)ex;
					assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.TERMS_AGREEMENT_REQUIRED);
					assertThat(apiEx.getMessage()).contains("동의해야 합니다");
				});

			then(memberRepository).should(never()).save(any());
		}

		@Test
		@DisplayName("should not throw exception when only optional marketing terms is not agreed")
		void shouldNotThrowExceptionWhenOnlyOptionalMarketingTermsIsNotAgreed() {
			// given
			setupCommonMocks();
			OnboardingRequest request = OnboardingRequest.builder()
				.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, false))
				.nickname("닉네임")
				.distCode1("11").distCode2("11680")
				.interests(buildInterests(1))
				.build();

			// when & then
			assertThatNoException()
				.isThrownBy(() -> onboardingService.completeOnboarding(1L, request, null));
		}
	}

	// =========================================================
	// completeOnboarding — 지역 코드 검증
	// =========================================================
	@Nested
	@DisplayName("completeOnboarding - district validation")
	class DistrictValidation {

		@Test
		@DisplayName("should throw OnboardingServiceApiException when district code is invalid")
		void shouldThrowExceptionWhenDistrictCodeIsInvalid() {
			// given
			given(memberRepository.findById(1L)).willReturn(Optional.of(inactiveMember));
			given(memberTermsAgreementRepository.saveAll(anyList())).willReturn(List.of());
			given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString()))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, buildValidRequest("닉네임", 1), null))
				.isInstanceOf(OnboardingServiceApiException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_DISTRICT_CODE);

			then(memberInterestRepository).should(never()).save(any());
			then(memberRepository).should(never()).save(any());
		}

		@ParameterizedTest(name = "should throw exception for invalid district code pair distCode1={0}, distCode2={1}")
		@DisplayName("should throw exception for various invalid district code combinations")
		@MethodSource("com.yeolcheong.mub.member.service.OnboardingServiceTest#invalidDistrictCodes")
		void shouldThrowExceptionForInvalidDistrictCodeCombinations(String distCode1, String distCode2) {
			// given
			given(memberRepository.findById(1L)).willReturn(Optional.of(inactiveMember));
			given(memberTermsAgreementRepository.saveAll(anyList())).willReturn(List.of());
			given(districtRepository.findByDistCode1AndDistCode2(distCode1, distCode2))
				.willReturn(Optional.empty());

			OnboardingRequest request = buildRequestWithDistrict(distCode1, distCode2);

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, request, null))
				.isInstanceOf(OnboardingServiceApiException.class);
		}
	}

	// =========================================================
	// completeOnboarding — 관심사 검증
	// =========================================================
	@Nested
	@DisplayName("completeOnboarding - interest validation")
	class InterestValidation {

		@Test
		@DisplayName("should throw INVALID_INTEREST_COUNT when interest list is empty")
		void shouldThrowInvalidInterestCountWhenInterestListIsEmpty() {
			// given
			setupTermsAndDistrictMocks();
			OnboardingRequest request = buildRequestWithInterests(List.of());

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, request, null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_INTEREST_COUNT));
		}

		@ParameterizedTest(name = "should throw INVALID_INTEREST_COUNT when interest count is {0}")
		@DisplayName("should throw INVALID_INTEREST_COUNT when interest count exceeds 3")
		@ValueSource(ints = {4, 5, 10})
		void shouldThrowInvalidInterestCountWhenInterestCountExceedsThree(int count) {
			// given
			setupTermsAndDistrictMocks();
			OnboardingRequest request = buildRequestWithInterests(buildInterests(count));

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, request, null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_INTEREST_COUNT));
		}

		@Test
		@DisplayName("should throw INTEREST_OPTION_REQUIRED when interest has null options")
		void shouldThrowInterestOptionRequiredWhenInterestHasNullOptions() {
			// given
			setupTermsAndDistrictMocks();
			OnboardingRequest request = buildRequestWithInterests(
				List.of(new OnboardingRequest.InterestRequest(InterestType.SELF_DEVELOPMENT, null))
			);

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, request, null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INTEREST_OPTION_REQUIRED));
		}

		@Test
		@DisplayName("should throw INTEREST_OPTION_REQUIRED when interest has empty options")
		void shouldThrowInterestOptionRequiredWhenInterestHasEmptyOptions() {
			// given
			setupTermsAndDistrictMocks();
			OnboardingRequest request = buildRequestWithInterests(
				List.of(new OnboardingRequest.InterestRequest(InterestType.SELF_DEVELOPMENT, List.of()))
			);

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, request, null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INTEREST_OPTION_REQUIRED));
		}

		@Test
		@DisplayName("should throw INVALID_INTEREST_OPTION when option does not belong to interest type")
		void shouldThrowInvalidInterestOptionWhenOptionDoesNotBelongToInterestType() {
			// given
			setupTermsAndDistrictMocks();
			// SELF_DEVELOPMENT 타입에 GRASS_FIELD(스포츠 옵션) 사용
			OnboardingRequest request = buildRequestWithInterests(
				List.of(new OnboardingRequest.InterestRequest(
					InterestType.SELF_DEVELOPMENT, List.of(InterestOption.GRASS_FIELD)))
			);

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, request, null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> {
					MemberServiceApiException apiEx = (MemberServiceApiException)ex;
					assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_INTEREST_OPTION);
					assertThat(apiEx.getMessage()).contains("옵션을 선택할 수 없습니다");
				});
		}

		@ParameterizedTest(name = "should throw INVALID_INTEREST_OPTION when {1} option used for SELF_DEVELOPMENT")
		@DisplayName("should throw INVALID_INTEREST_OPTION for various mismatched options")
		@MethodSource("com.yeolcheong.mub.member.service.OnboardingServiceTest#mismatchedInterestOptions")
		void shouldThrowInvalidInterestOptionForMismatchedOptions(
			InterestType type, InterestOption invalidOption
		) {
			// given
			setupTermsAndDistrictMocks();
			OnboardingRequest request = buildRequestWithInterests(
				List.of(new OnboardingRequest.InterestRequest(type, List.of(invalidOption)))
			);

			// when & then
			assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, request, null))
				.isInstanceOf(MemberServiceApiException.class)
				.satisfies(ex -> assertThat(((MemberServiceApiException)ex).getErrorCode())
					.isEqualTo(ErrorCode.INVALID_INTEREST_OPTION));
		}
	}

	static List<Arguments> requiredTermsRequests() {
		return List.of(
			Arguments.of("termsOfService 미동의",
				new OnboardingRequest.TermsAgreementRequest(false, true, true, false)),
			Arguments.of("privacyPolicy 미동의",
				new OnboardingRequest.TermsAgreementRequest(true, false, true, false)),
			Arguments.of("locationService 미동의",
				new OnboardingRequest.TermsAgreementRequest(true, true, false, false))
		);
	}

	static List<Arguments> invalidDistrictCodes() {
		return List.of(
			Arguments.of("00", "00000"),
			Arguments.of("99", "99999"),
			Arguments.of("", ""),
			Arguments.of("11", "99999")
		);
	}

	static List<Arguments> mismatchedInterestOptions() {
		return List.of(
			// SELF_DEVELOPMENT 타입에 스포츠 전용 옵션
			Arguments.of(InterestType.SELF_DEVELOPMENT, InterestOption.GRASS_FIELD),
			Arguments.of(InterestType.SELF_DEVELOPMENT, InterestOption.INDOOR),
			// SPORTS 타입에 자기계발 전용 옵션
			Arguments.of(InterestType.SPORTS, InterestOption.INTERNET_AVAILABLE),
			Arguments.of(InterestType.SPORTS, InterestOption.COMFORTABLE_CHAIR)
		);
	}

	static List<Arguments> alreadyOnboardedStatuses() {
		return List.of(
			Arguments.of(MemberStatus.ACTIVE),
			Arguments.of(MemberStatus.MUBACTIVE)
		);
	}
}