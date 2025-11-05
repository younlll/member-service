package com.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * member_interest_options 테이블
 * 관심사별 옵션종류
 */
@Getter
@RequiredArgsConstructor
public enum InterestOption {

	// 공통
	// 1. 자기계발, 재테크/투자, 독서/글쓰기, 교육/멘토링
	INTERNET_AVAILABLE("인터넷 사용가능"),
	CONTENT_AVAILABLE("콘텐츠 이용가능"),
	PARTITION_SPACE("칸막이 공간"),
	WINDOW_SEAT("창가 자리"),
	SINGLE_TABLE("1인 테이블"),
	MEETING_ROOM("회의실"),
	WIDE_TABLE("넓은 테이블"),
	COMFORTABLE_CHAIR("편한의자"),
	QUIET_MUSIC("조용한 음악"),
	WHITE_NOISE("백색소음"),
	CLEAN_RESTROOM("깨끗한 화장실"),
	SEPARATE_RESTROOM("남/여 화장실구분"),
	NO_KIDS_ZONE("노키즈존"),
	NO_SENIOR_ZONE("노시니어존"),
	SMOKING_AREA("흡연공간"),
	BEAM_PROJECTOR("빔프로젝터"),
	MEETING_SPACE("회의공간"),
	OUTSIDE_FOOD_OK("외부 음식 가능"),
	PARKING("주차"),
	TEAM_PROJECT("팀플"),
	SNACK_PROVIDED("간식제공"),

	// 2. 운동/스포츠
	GRASS_FIELD("잔디구장"),
	SHOWER("샤워"),
	EQUIPMENT_RENTAL("장비대여"),
	LESSON("레슨"),
	INDOOR("실내"),
	OUTDOOR("실외"),
	LOCKER("물품보관함"),

	// 3. 맛집/카페
	PET_FRIENDLY("반려동물"),
	OCEAN_VIEW("오션뷰"),
	RIVER_VIEW("리버뷰"),
	MOUNTAIN_VIEW("마운틴뷰"),
	CITY_VIEW("시티뷰"),

	// 4. 음악/악기
	SOUNDPROOF("방음"),
	RECORDING("녹음"),

	// 5. 댄스/무용/연기
	FULL_LENGTH_MIRROR("전신거울"),

	// 6.  사진/영상
	RENTAL_STUDIO("렌탈스튜디오");

	private final String description;
}
