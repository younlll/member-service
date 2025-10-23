package com.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
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
import com.member.domain.Member;
import com.member.dto.SnsUserInfoResponse;
import com.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 테스트")
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@InjectMocks
	private MemberService memberService;

	private Member testMember;
	private SnsUserInfoResponse kakaoUserInfo;

	@BeforeEach
	void setUp() {
		// 테스트용 회원 데이터 준비
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

		// 카카오 사용자 정보 준비
		SnsUserInfoResponse.KakaoAccount kakaoAccount = SnsUserInfoResponse.KakaoAccount.builder()
			.email("test@example.com")
			.build();

		kakaoUserInfo = SnsUserInfoResponse.builder()
			.id(1234567890L)
			.connectedAt("2025-10-24T00:00:00Z")
			.kakaoAccount(kakaoAccount)
			.build();
	}

	@Test
	@DisplayName("소셜 ID로 회원을 조회한다")
	void shouldFindMemberBySocialId() {
		// given
		given(memberRepository.findBySnsProviderAndSocialId(SnsProvider.KAKAO, "1234567890")).willReturn(
			Optional.of(testMember));

		// when
		Optional<Member> foundMember = memberService.findBySocialId(SnsProvider.KAKAO, "1234567890");

		// then
		assertThat(foundMember).isPresent();
		assertThat(foundMember.get().getSocialId()).isEqualTo("1234567890");
		verify(memberRepository, times(1)).findBySnsProviderAndSocialId(SnsProvider.KAKAO, "1234567890");
	}

	@Test
	@DisplayName("존재하지 않는 소셜 ID 조회 시 빈 Optional 반환")
	void shouldReturnEmptyWhenSocialIdNotFound() {
		// given
		given(memberRepository.findBySnsProviderAndSocialId(SnsProvider.KAKAO, "unknown")).willReturn(Optional.empty());

		// when
		Optional<Member> foundMember = memberService.findBySocialId(SnsProvider.KAKAO, "unknown");

		// then
		assertThat(foundMember).isEmpty();
		verify(memberRepository, times(1)).findBySnsProviderAndSocialId(SnsProvider.KAKAO, "unknown");
	}

	@Test
	@DisplayName("카카오 사용자 정보로 회원을 생성한다")
	void shouldCreateMemberFromKakaoUser() {
		// given
		Member newMember = Member.builder()
			.id(2L)
			.snsProvider(SnsProvider.KAKAO)
			.socialId("1234567890")
			.email("test@example.com")
			.status(MemberStatus.INACTIVE)
			.createdAt(LocalDateTime.now())
			.build();

		given(memberRepository.save(any(Member.class))).willReturn(newMember);

		// when
		Member createdMember = memberService.createdFromSnsUser(kakaoUserInfo);

		// then
		assertThat(createdMember).isNotNull();
		assertThat(createdMember.getId()).isEqualTo(2L);
		assertThat(createdMember.getSnsProvider()).isEqualTo(SnsProvider.KAKAO);
		assertThat(createdMember.getSocialId()).isEqualTo("1234567890");
		assertThat(createdMember.getEmail()).isEqualTo("test@example.com");
		assertThat(createdMember.getStatus()).isEqualTo(MemberStatus.INACTIVE);

		verify(memberRepository, times(1)).save(any(Member.class));
	}

	@Test
	@DisplayName("이메일이 없는 카카오 사용자도 회원 생성 가능")
	void shouldCreateMemberWithoutEmail() {
		// given
		SnsUserInfoResponse userInfoWithoutEmail = SnsUserInfoResponse.builder()
			.id(9999L)
			.connectedAt("2025-10-24T00:00:00Z")
			.kakaoAccount(SnsUserInfoResponse.KakaoAccount.builder().build())
			.build();

		Member newMember = Member.builder()
			.id(3L)
			.snsProvider(SnsProvider.KAKAO)
			.socialId("9999")
			.email(null)
			.status(MemberStatus.INACTIVE)
			.build();

		given(memberRepository.save(any(Member.class))).willReturn(newMember);

		// when
		Member createdMember = memberService.createdFromSnsUser(userInfoWithoutEmail);

		// then
		assertThat(createdMember).isNotNull();
		assertThat(createdMember.getEmail()).isNull();
		verify(memberRepository, times(1)).save(any(Member.class));
	}
}