package com.yeolcheong.mub.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.yeolcheong.mub.member.config.ImageProperties;
import com.yeolcheong.mub.member.domain.Member;
import com.yeolcheong.mub.member.domain.MemberProfileImage;
import com.yeolcheong.mub.member.dto.ProfileImageResponse;
import com.yeolcheong.mub.member.exception.ErrorCode;
import com.yeolcheong.mub.member.exception.MemberServiceApiException;
import com.yeolcheong.mub.member.repository.MemberProfileImageRepository;
import com.yeolcheong.mub.member.repository.MemberRepository;
import com.yeolcheong.mub.member.storage.ProfileImageStorage;
import com.yeolcheong.mub.member.storage.StoredImage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 프로필 이미지 관리 서비스.
 * <p>
 * 등록/수정/삭제/조회를 담당한다. 실제 파일 저장은 {@link ProfileImageStorage}(현재 로컬,
 * 향후 서버 스토리지 경유 업로드)에 위임하고, DB에는 스토리지 중립 상대 경로만 보관한다.
 * 회원 ↔ 이미지 연결은 {@code members.image_id} 단방향 참조(1:1)다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileImageService {

	private final MemberRepository memberRepository;
	private final MemberProfileImageRepository profileImageRepository;
	private final ProfileImageStorage profileImageStorage;
	private final ImageProperties imageProperties;

	/**
	 * 프로필 이미지 등록(또는 교체). 이미 이미지가 있으면 새 파일로 교체한다.
	 */
	@Transactional
	public ProfileImageResponse register(Long memberId, MultipartFile file) {
		Member member = findMember(memberId);
		StoredImage stored = profileImageStorage.store(file);

		MemberProfileImage image = upsertImage(member, stored);
		member.assignImage(image.getId());

		log.info("Profile image registered: memberId={}, imageId={}", memberId, image.getId());
		return toResponse(image.getFilePath());
	}

	/**
	 * 프로필 이미지 수정. 기존 커스텀 이미지가 있어야 한다(없으면 404).
	 */
	@Transactional
	public ProfileImageResponse update(Long memberId, MultipartFile file) {
		Member member = findMember(memberId);
		if (member.getImageId() == null) {
			throw new MemberServiceApiException(ErrorCode.PROFILE_IMAGE_NOT_FOUND);
		}
		StoredImage stored = profileImageStorage.store(file);

		MemberProfileImage image = upsertImage(member, stored);
		member.assignImage(image.getId());

		log.info("Profile image updated: memberId={}, imageId={}", memberId, image.getId());
		return toResponse(image.getFilePath());
	}

	/**
	 * 프로필 이미지 삭제 → 기본 이미지로 복귀. 커스텀 이미지가 없어도 멱등하게 동작한다.
	 */
	@Transactional
	public ProfileImageResponse delete(Long memberId) {
		Member member = findMember(memberId);
		Long imageId = member.getImageId();

		if (imageId != null) {
			profileImageRepository.findById(imageId).ifPresent(image -> {
				profileImageStorage.delete(image.getFilePath());
				profileImageRepository.delete(image);
			});
			member.removeImage();
			log.info("Profile image deleted, reverted to default: memberId={}", memberId);
		}

		return toResponse(imageProperties.defaultProfilePath());
	}

	/**
	 * 현재 프로필 이미지 조회. 커스텀 이미지가 없으면 기본 이미지를 반환한다.
	 */
	public ProfileImageResponse get(Long memberId) {
		Member member = findMember(memberId);
		Long imageId = member.getImageId();

		if (imageId == null) {
			return toResponse(imageProperties.defaultProfilePath());
		}

		MemberProfileImage image = profileImageRepository.findById(imageId)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.PROFILE_IMAGE_NOT_FOUND));
		return toResponse(image.getFilePath());
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberServiceApiException(ErrorCode.MEMBER_NOT_FOUND));
	}

	/**
	 * 회원의 기존 이미지 행이 있으면 갱신, 없으면 새로 생성한다.
	 */
	private MemberProfileImage upsertImage(Member member, StoredImage stored) {
		Long imageId = member.getImageId();
		if (imageId != null) {
			MemberProfileImage existing = profileImageRepository.findById(imageId).orElse(null);
			if (existing != null) {
				// 기존 파일 삭제 후 새 파일 정보로 교체
				profileImageStorage.delete(existing.getFilePath());
				existing.changeFile(stored.originalFileName(), stored.storedFileName(),
					stored.filePath(), stored.contentType(), stored.fileSize());
				return existing;
			}
		}

		MemberProfileImage created = MemberProfileImage.builder()
			.originalFileName(stored.originalFileName())
			.storedFileName(stored.storedFileName())
			.filePath(stored.filePath())
			.contentType(stored.contentType())
			.fileSize(stored.fileSize())
			.build();
		return profileImageRepository.save(created);
	}

	private ProfileImageResponse toResponse(String filePath) {
		boolean isDefault = imageProperties.defaultProfilePath().equals(filePath);
		return ProfileImageResponse.of(imageProperties.toPublicUrl(filePath), isDefault);
	}
}
