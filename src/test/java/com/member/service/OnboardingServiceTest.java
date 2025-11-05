package com.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.member.common.MemberStatus;
import com.member.common.SnsProvider;
import com.member.domain.District;
import com.member.domain.InterestOption;
import com.member.domain.InterestType;
import com.member.domain.Member;
import com.member.domain.MemberInterest;
import com.member.domain.MemberTermsAgreement;
import com.member.domain.TermsType;
import com.member.dto.OnboardingRequest;
import com.member.dto.OnboardingResponse;
import com.member.exception.MemberServiceApiException;
import com.member.exception.OnboardingServiceApiException;
import com.member.repository.DistrictRepository;
import com.member.repository.MemberInterestRepository;
import com.member.repository.MemberRepository;
import com.member.repository.MemberTermsAgreementRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingService 테스트")
class OnboardingServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MemberTermsAgreementRepository termsAgreementRepository;

	@Mock
	private DistrictRepository districtRepository;

	@Mock
	private MemberInterestRepository memberInterestRepository;

	@InjectMocks
	private OnboardingService onboardingService;

	private Member member;
	private District district;
	private OnboardingRequest onboardingRequest;

	@BeforeEach
	void setUp() {
		member = Member.builder()
			.id(1L)
			.snsProvider(SnsProvider.KAKAO)
			.socialId("1234567890")
			.email("test@kakao.com")
			.status(MemberStatus.INACTIVE)
			.build();

		district = District.builder()
			.distCode1("11")
			.distCode1Name("서울특별시")
			.distCode2("11680")
			.distCode2Name("강남구")
			.build();

		onboardingRequest = OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true,    // termsOfService
				true,    // privacyPolicy
				true,    // locationService
				false    // marketing
			))
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(List.of(new OnboardingRequest.InterestRequest(InterestType.SELF_DEVELOPMENT,
				Arrays.asList(InterestOption.INTERNET_AVAILABLE, InterestOption.COMFORTABLE_CHAIR))))
			.build();
	}

	@Test
	@DisplayName("요청한 정보가 모두 유효한경우 회원가입에 성공한다")
	void shouldCompleteOnboardingSuccessfully() {
		// Given
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString())).willReturn(
			Optional.of(district));
		given(termsAgreementRepository.saveAll(anyList())).willReturn(List.of());
		given(memberInterestRepository.save(any(MemberInterest.class))).willReturn(MemberInterest.builder().build());

		// When
		OnboardingResponse response = onboardingService.completeOnboarding(1L, onboardingRequest);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.getMemberId()).isEqualTo(1L);
		assertThat(response.getNickname()).isEqualTo("테스트유저");
		assertThat(response.getMessage()).isEqualTo("회원가입이 완료되었습니다");

		// Verify
		then(memberRepository).should().findById(1L);
		then(termsAgreementRepository).should().saveAll(anyList());
		then(districtRepository).should().findByDistCode1AndDistCode2("11", "11680");
		then(memberInterestRepository).should().save(any(MemberInterest.class));
		then(memberRepository).should().save(member);

		// 회원 상태 확인
		assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(member.getNickname()).isEqualTo("테스트유저");
	}

	@Test
	@DisplayName("존재하지 않는 ID로 회원가입 요청 시, 실패한다")
	void shouldFailWhenMemberNotFound() {
		// Given
		given(memberRepository.findById(1L)).willReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, onboardingRequest)).isInstanceOf(
			MemberServiceApiException.class).hasMessage("회원을 찾을 수 없습니다");

		then(termsAgreementRepository).should(never()).saveAll(anyList());
	}

	@Test
	@DisplayName("이미 회원가입을 완료한 회원이 다시 회원가입을 요청하는 경우 실패한다")
	void shouldFailWhenAlreadyOnboarded() {
		// Given
		member = Member.builder().id(1L).status(MemberStatus.ACTIVE).build();
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		// When & Then
		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, onboardingRequest)).isInstanceOf(
			MemberServiceApiException.class).hasMessage("이미 회원가입이 완료된 회원입니다");

		then(termsAgreementRepository).should(never()).saveAll(anyList());
	}

	@Test
	@DisplayName("회원가입 시, 필수 약관을 미동의하는 경우 회원가입에 실패한다")
	void shouldFailWhenRequiredTermsNotAgreed() {
		// Given
		OnboardingRequest requestWithoutTerms1 = OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(false, true, true, true))
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(onboardingRequest.getInterests())
			.build();

		OnboardingRequest requestWithoutTerms2 = OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, false, true, true))
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(onboardingRequest.getInterests())
			.build();

		OnboardingRequest requestWithoutTerms3 = OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, false, true))
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(onboardingRequest.getInterests())
			.build();

		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		// When & Then
		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, requestWithoutTerms1)).isInstanceOf(
			MemberServiceApiException.class).hasMessageContaining("동의해야 합니다");

		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, requestWithoutTerms2)).isInstanceOf(
			MemberServiceApiException.class).hasMessageContaining("동의해야 합니다");

		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, requestWithoutTerms3)).isInstanceOf(
			MemberServiceApiException.class).hasMessageContaining("동의해야 합니다");

		then(memberRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("유효하지 않은 지역코드의 요청인 경우 회원가입에 실패한다")
	void shouldFailWhenInvalidDistrictCode() {
		// Given
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString())).willReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, onboardingRequest)).isInstanceOf(
			OnboardingServiceApiException.class).hasMessage("유효하지 않은 지역 코드입니다");

		then(memberInterestRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("관심사의 갯수가 3개 초과의 경우 회원가입에 실패한다")
	void shouldFailWhenTooManyInterests() {
		// Given
		OnboardingRequest requestWith4Interests = OnboardingRequest.builder()
			.termsAgreementRequest(onboardingRequest.getTermsAgreementRequest())
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(Arrays.asList(new OnboardingRequest.InterestRequest(InterestType.SELF_DEVELOPMENT,
					List.of(InterestOption.INTERNET_AVAILABLE)),
				new OnboardingRequest.InterestRequest(InterestType.SPORTS, List.of(InterestOption.INDOOR)),
				new OnboardingRequest.InterestRequest(InterestType.RESTAURANT, List.of(InterestOption.PARKING)),
				new OnboardingRequest.InterestRequest(InterestType.MUSIC, List.of(InterestOption.SOUNDPROOF))))
			.build();

		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString())).willReturn(
			Optional.of(district));

		// When & Then
		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, requestWith4Interests)).isInstanceOf(
			MemberServiceApiException.class).hasMessage("관심사는 1개 이상 3개 이하로 선택해야 합니다");
	}

	@Test
	@DisplayName("관심사에 대한 옵션을 선택하지 않는 경우, 회원가입에 실패합니다")
	void shouldFailWhenNoOptionSelected() {
		// Given
		OnboardingRequest requestWithoutOptions = OnboardingRequest.builder()
			.termsAgreementRequest(onboardingRequest.getTermsAgreementRequest())
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(List.of(new OnboardingRequest.InterestRequest(InterestType.SELF_DEVELOPMENT, List.of())))
			.build();

		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString())).willReturn(
			Optional.of(district));

		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, requestWithoutOptions)).isInstanceOf(
			MemberServiceApiException.class).hasMessage("관심사별 옵션은 최소 1개 이상 선택해야 합니다");
	}

	@Test
	@DisplayName("유효하지 않은 관심사 옵션으로 회원가입 시도시 실패한다")
	void shouldFailWhenInvalidInterestOption() {
		// Given
		OnboardingRequest requestWithInvalidOption = OnboardingRequest.builder()
			.termsAgreementRequest(onboardingRequest.getTermsAgreementRequest())
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(List.of(new OnboardingRequest.InterestRequest(InterestType.SELF_DEVELOPMENT,
				List.of(InterestOption.GRASS_FIELD))))
			.build();

		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString()))
			.willReturn(Optional.of(district));

		// When & Then
		assertThatThrownBy(() -> onboardingService.completeOnboarding(1L, requestWithInvalidOption))
			.isInstanceOf(MemberServiceApiException.class)
			.hasMessageContaining("옵션을 선택할 수 없습니다");
	}

	@Test
	@DisplayName("관심사를 최대 3개까지 선택해서 회원가입에 성공한다")
	void shouldCompleteOnboardingWith3Interests() {
		// Given
		OnboardingRequest requestWith3Interests = OnboardingRequest.builder()
			.termsAgreementRequest(onboardingRequest.getTermsAgreementRequest())
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(List.of(new OnboardingRequest.InterestRequest(
					InterestType.SELF_DEVELOPMENT, List.of(InterestOption.INTERNET_AVAILABLE)
				),
				new OnboardingRequest.InterestRequest(InterestType.SPORTS,
					List.of(InterestOption.INDOOR, InterestOption.SHOWER)),
				new OnboardingRequest.InterestRequest(InterestType.RESTAURANT, List.of(InterestOption.PARKING))))
			.build();

		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString()))
			.willReturn(Optional.of(district));
		given(memberInterestRepository.save(any(MemberInterest.class)))
			.willReturn(MemberInterest.builder().build());

		// When
		OnboardingResponse response = onboardingService.completeOnboarding(1L, requestWith3Interests);

		// Then
		assertThat(response).isNotNull();
		then(memberInterestRepository).should(times(3)).save(any(MemberInterest.class));
	}

	@Test
	@DisplayName("마케팅 약관에 동의하고 회원가입에 성공한다")
	void shouldCompleteOnboardingWithMarketingAgreed() {
		// Given
		OnboardingRequest request = OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, true))
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(onboardingRequest.getInterests())
			.build();

		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString()))
			.willReturn(Optional.of(district));
		given(memberInterestRepository.save(any(MemberInterest.class)))
			.willReturn(MemberInterest.builder().build());

		// When
		OnboardingResponse response = onboardingService.completeOnboarding(1L, request);

		// Then
		assertThat(response).isNotNull();
		then(termsAgreementRepository).should().saveAll(argThat(list -> {
			List<MemberTermsAgreement> agreementList = (List<MemberTermsAgreement>)list;
			return agreementList.stream().anyMatch(agreement ->
				agreement.getTermsType() == TermsType.MARKETING && agreement.getAgreed()
			);
		}));
	}

	@Test
	@DisplayName("마케팅 약관에 동의하고 회원가입에 시ㅣㄹ패한다")
	void shouldCompleteOnboardingWithMarketingNotAgreed() {
		// Given
		OnboardingRequest request = OnboardingRequest.builder()
			.termsAgreementRequest(new OnboardingRequest.TermsAgreementRequest(true, true, true, false))
			.nickname("테스트유저")
			.distCode1("11")
			.distCode2("11680")
			.interests(onboardingRequest.getInterests())
			.build();

		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(districtRepository.findByDistCode1AndDistCode2(anyString(), anyString()))
			.willReturn(Optional.of(district));
		given(memberInterestRepository.save(any(MemberInterest.class)))
			.willReturn(MemberInterest.builder().build());

		// When
		OnboardingResponse response = onboardingService.completeOnboarding(1L, request);

		// Then
		assertThat(response).isNotNull();
		then(termsAgreementRepository).should().saveAll(argThat(list -> {
			List<MemberTermsAgreement> agreementList = (List<MemberTermsAgreement>)list;
			return agreementList.stream().anyMatch(agreement ->
				agreement.getTermsType() == TermsType.MARKETING && !agreement.getAgreed()
			);
		}));
	}
}
