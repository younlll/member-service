package com.member.domain;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * member_interests 테이블의 관심사 유형
 */
@Getter
@RequiredArgsConstructor
public enum InterestType {

	SELF_DEVELOPMENT("자기계발", Arrays.asList(
		InterestOption.INTERNET_AVAILABLE,
		InterestOption.CONTENT_AVAILABLE,
		InterestOption.PARTITION_SPACE,
		InterestOption.WINDOW_SEAT,
		InterestOption.SINGLE_TABLE,
		InterestOption.MEETING_ROOM,
		InterestOption.WIDE_TABLE,
		InterestOption.COMFORTABLE_CHAIR,
		InterestOption.QUIET_MUSIC,
		InterestOption.WHITE_NOISE,
		InterestOption.CLEAN_RESTROOM,
		InterestOption.SEPARATE_RESTROOM,
		InterestOption.NO_KIDS_ZONE,
		InterestOption.NO_SENIOR_ZONE,
		InterestOption.SMOKING_AREA,
		InterestOption.BEAM_PROJECTOR,
		InterestOption.MEETING_SPACE,
		InterestOption.OUTSIDE_FOOD_OK,
		InterestOption.PARKING,
		InterestOption.TEAM_PROJECT,
		InterestOption.SNACK_PROVIDED
	)),

	INVESTMENT("재테크/투자", Arrays.asList(
		InterestOption.INTERNET_AVAILABLE,
		InterestOption.CONTENT_AVAILABLE,
		InterestOption.PARTITION_SPACE,
		InterestOption.WINDOW_SEAT,
		InterestOption.SINGLE_TABLE,
		InterestOption.MEETING_ROOM,
		InterestOption.WIDE_TABLE,
		InterestOption.COMFORTABLE_CHAIR,
		InterestOption.QUIET_MUSIC,
		InterestOption.WHITE_NOISE,
		InterestOption.CLEAN_RESTROOM,
		InterestOption.SEPARATE_RESTROOM,
		InterestOption.NO_KIDS_ZONE,
		InterestOption.NO_SENIOR_ZONE,
		InterestOption.SMOKING_AREA,
		InterestOption.BEAM_PROJECTOR,
		InterestOption.MEETING_SPACE,
		InterestOption.OUTSIDE_FOOD_OK,
		InterestOption.PARKING,
		InterestOption.TEAM_PROJECT,
		InterestOption.SNACK_PROVIDED
	)),

	READING("독서/글쓰기", Arrays.asList(
		InterestOption.INTERNET_AVAILABLE,
		InterestOption.CONTENT_AVAILABLE,
		InterestOption.PARTITION_SPACE,
		InterestOption.WINDOW_SEAT,
		InterestOption.SINGLE_TABLE,
		InterestOption.MEETING_ROOM,
		InterestOption.WIDE_TABLE,
		InterestOption.COMFORTABLE_CHAIR,
		InterestOption.QUIET_MUSIC,
		InterestOption.WHITE_NOISE,
		InterestOption.CLEAN_RESTROOM,
		InterestOption.SEPARATE_RESTROOM,
		InterestOption.NO_KIDS_ZONE,
		InterestOption.NO_SENIOR_ZONE,
		InterestOption.SMOKING_AREA,
		InterestOption.BEAM_PROJECTOR,
		InterestOption.MEETING_SPACE,
		InterestOption.OUTSIDE_FOOD_OK,
		InterestOption.PARKING,
		InterestOption.TEAM_PROJECT,
		InterestOption.SNACK_PROVIDED
	)),

	EDUCATION("교육/멘토링", Arrays.asList(
		InterestOption.INTERNET_AVAILABLE,
		InterestOption.CONTENT_AVAILABLE,
		InterestOption.PARTITION_SPACE,
		InterestOption.WINDOW_SEAT,
		InterestOption.SINGLE_TABLE,
		InterestOption.MEETING_ROOM,
		InterestOption.WIDE_TABLE,
		InterestOption.COMFORTABLE_CHAIR,
		InterestOption.QUIET_MUSIC,
		InterestOption.WHITE_NOISE,
		InterestOption.CLEAN_RESTROOM,
		InterestOption.SEPARATE_RESTROOM,
		InterestOption.NO_KIDS_ZONE,
		InterestOption.NO_SENIOR_ZONE,
		InterestOption.SMOKING_AREA,
		InterestOption.BEAM_PROJECTOR,
		InterestOption.MEETING_SPACE,
		InterestOption.OUTSIDE_FOOD_OK,
		InterestOption.PARKING,
		InterestOption.TEAM_PROJECT,
		InterestOption.SNACK_PROVIDED
	)),

	SPORTS("운동/스포츠", Arrays.asList(
		InterestOption.GRASS_FIELD,
		InterestOption.PARKING,
		InterestOption.SHOWER,
		InterestOption.EQUIPMENT_RENTAL,
		InterestOption.LESSON,
		InterestOption.INDOOR,
		InterestOption.OUTDOOR,
		InterestOption.LOCKER,
		InterestOption.SEPARATE_RESTROOM
	)),

	RESTAURANT("맛집/카페", Arrays.asList(
		InterestOption.PET_FRIENDLY,
		InterestOption.PARKING,
		InterestOption.OCEAN_VIEW,
		InterestOption.RIVER_VIEW,
		InterestOption.MOUNTAIN_VIEW,
		InterestOption.CITY_VIEW
	)),

	MUSIC("음악/악기", Arrays.asList(
		InterestOption.SOUNDPROOF,
		InterestOption.RECORDING,
		InterestOption.EQUIPMENT_RENTAL,
		InterestOption.SEPARATE_RESTROOM
	)),

	DANCE("댄스/무용/연기", Arrays.asList(
		InterestOption.FULL_LENGTH_MIRROR,
		InterestOption.EQUIPMENT_RENTAL,
		InterestOption.BEAM_PROJECTOR,
		InterestOption.LOCKER,
		InterestOption.SEPARATE_RESTROOM,
		InterestOption.SHOWER
	)),

	PHOTO("사진/영상", Arrays.asList(
		InterestOption.RENTAL_STUDIO,
		InterestOption.EQUIPMENT_RENTAL,
		InterestOption.SEPARATE_RESTROOM
	));

	private final String description;
	private final List<InterestOption> availableOptions;
}
