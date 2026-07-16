# 관심사 API (`/api/interests`)

관심사 유형과 유형별 선택 가능 옵션 목록을 조회합니다. 인증이 필요 없습니다(public).
프로필/온보딩의 `interests[].interestType` 및 `interests[].options` 값으로 사용합니다.

---

## GET `/api/interests`

모든 관심사 유형과 각 유형의 옵션 목록을 조회합니다.

- **인증**: 불필요

### 성공 (200)

각 항목은 `interestType`(enum), `description`(한글 설명), `availableOptions`(옵션 배열)로 구성됩니다.
(아래는 첫 유형 발췌)

```json
[
  {
    "interestType": "SELF_DEVELOPMENT",
    "description": "자기계발",
    "availableOptions": [
      { "interestOption": "INTERNET_AVAILABLE", "description": "인터넷 사용가능" },
      { "interestOption": "CONTENT_AVAILABLE", "description": "콘텐츠 이용가능" },
      { "interestOption": "PARTITION_SPACE", "description": "칸막이 공간" },
      { "interestOption": "WINDOW_SEAT", "description": "창가 자리" }
    ]
  }
]
```

### 관심사 유형(interestType) 전체 목록

| interestType | description |
|---|---|
| `SELF_DEVELOPMENT` | 자기계발 |
| `INVESTMENT` | 재테크/투자 |
| `READING` | 독서/글쓰기 |
| `EDUCATION` | 교육/멘토링 |
| `SPORTS` | 운동/스포츠 |
| `RESTAURANT` | 맛집/카페 |
| `MUSIC` | 음악/악기 |
| `DANCE` | 댄스/무용/연기 |
| `PHOTO` | 사진/영상 |

`SELF_DEVELOPMENT`/`INVESTMENT`/`READING`/`EDUCATION`은 동일한 21개 공통 옵션을,
`SPORTS`/`RESTAURANT`/`MUSIC`/`DANCE`/`PHOTO`는 각자 고유 옵션 세트를 가집니다.
전체 옵션과 한글 설명은 응답의 `availableOptions`에서 확인할 수 있습니다.

> 프로필/온보딩에서 해당 유형에 없는 옵션을 보내면 `E40009`(예: `'음악/악기' 관심사에는 '잔디구장' 옵션을 선택할 수 없습니다`)가 발생합니다.

### 실패

별도의 실패 케이스 없음(파라미터 없음).
