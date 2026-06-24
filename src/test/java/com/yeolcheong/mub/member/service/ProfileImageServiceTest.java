package com.yeolcheong.mub.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.yeolcheong.mub.member.config.ImageProperties;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.MemberProfileImage;
import com.yeolcheong.mub.member.domain.MemberStatus;
import com.yeolcheong.mub.member.dto.ProfileImageResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.MemberProfileImageRepository;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.storage.ProfileImageStorage;
import com.yeolcheong.mub.member.storage.StoredImage;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileImageService")
class ProfileImageServiceTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private MemberProfileImageRepository profileImageRepository;
	@Mock
	private ProfileImageStorage profileImageStorage;

	// record 라 mock 대신 실제 인스턴스를 주입한다.
	private final ImageProperties imageProperties =
		new ImageProperties("images", "profile", "http://localhost:8083", "/images", "profile/default.png");

	private ProfileImageService profileImageService;

	@BeforeEach
	void setUp() {
		profileImageService = new ProfileImageService(
			memberRepository, profileImageRepository, profileImageStorage, imageProperties);
	}

	@Test
	@DisplayName("register - creates image and links to member when none exists")
	void register_success() {
		// given
		Member member = buildMember(1L, null);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(profileImageStorage.store(any())).willReturn(buildStoredImage());
		given(profileImageRepository.save(any())).willReturn(buildImage(100L, "profile/uuid.png"));

		// when
		ProfileImageResponse response = profileImageService.register(1L, buildFile());

		// then
		assertThat(response.getImageUrl()).isEqualTo("http://localhost:8083/images/profile/uuid.png");
		assertThat(response.isDefault()).isFalse();
		assertThat(member.getImageId()).isEqualTo(100L);
		then(profileImageRepository).should().save(any(MemberProfileImage.class));
	}

	@Test
	@DisplayName("register - replaces existing image and deletes old file")
	void register_replacesExisting() {
		// given
		Member member = buildMember(1L, 100L);
		MemberProfileImage existing = buildImage(100L, "profile/old.png");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(profileImageStorage.store(any())).willReturn(buildStoredImage());
		given(profileImageRepository.findById(100L)).willReturn(Optional.of(existing));

		// when
		ProfileImageResponse response = profileImageService.register(1L, buildFile());

		// then
		then(profileImageStorage).should().delete("profile/old.png");
		assertThat(existing.getFilePath()).isEqualTo("profile/uuid.png");
		assertThat(response.getImageUrl()).isEqualTo("http://localhost:8083/images/profile/uuid.png");
		then(profileImageRepository).should(never()).save(any());
	}

	@Test
	@DisplayName("register - throws MEMBER_NOT_FOUND when member does not exist")
	void register_memberNotFound() {
		given(memberRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> profileImageService.register(999L, buildFile()))
			.isInstanceOf(MemberServiceApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("update - throws PROFILE_IMAGE_NOT_FOUND when member has no custom image")
	void update_noExistingImage() {
		Member member = buildMember(1L, null);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		assertThatThrownBy(() -> profileImageService.update(1L, buildFile()))
			.isInstanceOf(MemberServiceApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
		then(profileImageStorage).should(never()).store(any());
	}

	@Test
	@DisplayName("delete - removes custom image and reverts to default")
	void delete_revertsToDefault() {
		Member member = buildMember(1L, 100L);
		MemberProfileImage existing = buildImage(100L, "profile/old.png");
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(profileImageRepository.findById(100L)).willReturn(Optional.of(existing));

		ProfileImageResponse response = profileImageService.delete(1L);

		then(profileImageStorage).should().delete("profile/old.png");
		then(profileImageRepository).should().delete(existing);
		assertThat(member.getImageId()).isNull();
		assertThat(response.isDefault()).isTrue();
		assertThat(response.getImageUrl()).isEqualTo("http://localhost:8083/images/profile/default.png");
	}

	@Test
	@DisplayName("delete - idempotent when member already has no custom image")
	void delete_idempotent() {
		Member member = buildMember(1L, null);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		ProfileImageResponse response = profileImageService.delete(1L);

		then(profileImageStorage).should(never()).delete(any());
		then(profileImageRepository).should(never()).delete(any());
		assertThat(response.isDefault()).isTrue();
	}

	@Test
	@DisplayName("get - returns default image when member has no custom image")
	void get_default() {
		Member member = buildMember(1L, null);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));

		ProfileImageResponse response = profileImageService.get(1L);

		assertThat(response.isDefault()).isTrue();
		assertThat(response.getImageUrl()).isEqualTo("http://localhost:8083/images/profile/default.png");
	}

	@Test
	@DisplayName("get - returns custom image url when member has one")
	void get_custom() {
		Member member = buildMember(1L, 100L);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(profileImageRepository.findById(100L)).willReturn(Optional.of(buildImage(100L, "profile/uuid.png")));

		ProfileImageResponse response = profileImageService.get(1L);

		assertThat(response.isDefault()).isFalse();
		assertThat(response.getImageUrl()).isEqualTo("http://localhost:8083/images/profile/uuid.png");
	}

	@Test
	@DisplayName("get - throws PROFILE_IMAGE_NOT_FOUND when linked image row is missing")
	void get_danglingImageId() {
		Member member = buildMember(1L, 100L);
		given(memberRepository.findById(1L)).willReturn(Optional.of(member));
		given(profileImageRepository.findById(100L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> profileImageService.get(1L))
			.isInstanceOf(MemberServiceApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
	}

	@Test
	@DisplayName("resolveImageUrls - resolves custom urls and falls back to default for none/dangling")
	void resolveImageUrls_mixed() {
		// given — member with custom image, member without image, member with dangling image id
		Member withImage = buildMember(1L, 100L);
		Member noImage = buildMember(2L, null);
		Member dangling = buildMember(3L, 200L);
		given(profileImageRepository.findAllById(List.of(100L, 200L)))
			.willReturn(List.of(buildImage(100L, "profile/uuid.png")));

		// when
		Map<Long, String> result = profileImageService.resolveImageUrls(List.of(withImage, noImage, dangling));

		// then
		String defaultUrl = "http://localhost:8083/images/profile/default.png";
		assertThat(result).hasSize(3);
		assertThat(result.get(1L)).isEqualTo("http://localhost:8083/images/profile/uuid.png");
		assertThat(result.get(2L)).isEqualTo(defaultUrl);
		assertThat(result.get(3L)).isEqualTo(defaultUrl);
	}

	@Test
	@DisplayName("resolveImageUrls - returns empty map for empty input without hitting the repository")
	void resolveImageUrls_empty() {
		// when
		Map<Long, String> result = profileImageService.resolveImageUrls(List.of());

		// then
		assertThat(result).isEmpty();
		then(profileImageRepository).should(never()).findAllById(any());
	}

	@Test
	@DisplayName("resolveImageUrls - returns empty map for null input")
	void resolveImageUrls_null() {
		// when
		Map<Long, String> result = profileImageService.resolveImageUrls(null);

		// then
		assertThat(result).isEmpty();
		then(profileImageRepository).should(never()).findAllById(any());
	}

	private Member buildMember(Long id, Long imageId) {
		Member member = Member.builder()
			.id(id)
			.status(MemberStatus.ACTIVE)
			.build();
		if (imageId != null) {
			member.assignImage(imageId);
		}
		return member;
	}

	private MemberProfileImage buildImage(Long id, String filePath) {
		return MemberProfileImage.builder()
			.id(id)
			.storedFileName("stored.png")
			.filePath(filePath)
			.contentType("image/png")
			.fileSize(1024L)
			.build();
	}

	private StoredImage buildStoredImage() {
		return new StoredImage("x.png", "uuid.png", "profile/uuid.png", "image/png", 3L);
	}

	private MultipartFile buildFile() {
		return new MockMultipartFile("file", "x.png", "image/png", new byte[] {1, 2, 3});
	}
}
