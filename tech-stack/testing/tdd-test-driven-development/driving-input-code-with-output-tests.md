# TDD - Driving Input Code with Output Tests

#### 범위 클레임

* Spring Security가 `OAuth2AuthorizationManagers.hasScope` 정적 메서드를 사용해서 특정 조건에 해당하는 요청에 접근할 수 있는 범위를 지정할 수 있다.
* JWT 페이로드에 "scope" 클레임을 추가할 수 있다.

#### 시간 확인

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b204d0ff-0a92-4989-b79e-628256429fac.png)

* 시간의 경우 정확히 같은 값(`isEqualTo`)으로 비교하지 않는다.
  * 왜냐하면 등록 시간은 서버가 요청을 처리하는 순간 `LocalDateTime.now(UTC)`로 생성된다. 테스트는 그 정확한 나노초를 알 수 없다.
  * `isEqualTo`로 비교하면 밀리초/나노초 차이로 항상 실패하거나 운 좋게 맞아도 극도로 불안정하다.
* 기준 시각(Reference Time) 패턴
  * 그래서 등록을 트리거하기 직전에 기준 시각을 찍어두고 "실제 기록된 시간이 이 기준에서 얼마 벗어나지 않았는가"를 검증한다.
  * 순서상 `referenceTime` ≤ 서버 기록 시간이 되고, 그 사이 간격은 네트워크 왕복 + 서버 처리 시간뿐이다.
* `isCloseTo(refernceTime, within(1, SECONDS))`
  * 1초는 양방향 허용치
  * 1초라는 폭은 절묘한 균형점
* UTC를 양쪽에서 맞춰야만 하는 이유
  * 테스트와 서버 둘 다 `LocalDateTime.now(UTC)`를 쓴다. `LocalDateTime`은 타임존 정보가 없는 값이라 한쪽이 KST고 다른 쪽이 UTC면 9시간 차이가 발생한다.
  * 그래서 UTC 기준으로 통일해 비교 기준 프레임을 일치시키는 것이 중요하다.
