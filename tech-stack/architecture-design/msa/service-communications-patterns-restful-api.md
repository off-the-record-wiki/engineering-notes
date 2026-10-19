# MicroService Architecture - Service Communications Patterns(RESTful API)

#### Request/Response Communication

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/560ab93f-0897-4f9d-822e-6b5cffc29c45.png)

* **장점**
  * 단순함 · 낮은 진입장벽 : 별도 인프라 필요 X
  * 직관적인 흐름 : 요청 → 응답이 한 줄로 이어져 코드 읽기가 쉽고 추론 역시 쉽다. 호출 결과를 바로 받아 쓰는 동기 코드라 로직이 명확하다.
  * 범용성 · 호환성 : HTTP/JSON은 어떤 언어·툴로도 바로 테스트가 가능하다.
  * 즉시 일관성 : 호출 시점에 상대 서비스의 최신 데이터를 바로 받을 수 있다.
  * 명확한 에러 전파 : HTTP 상태코드로 실패를 바로 인지할 수 있다.
* **단점**
  * 강한 시간적 결합 : 호출하는 순간 상대 서버가 살아있어야 한다.
  * 동기 블로킹 = 자원 낭비 : 응답이 올 때까지 쓰레드 점유가 된다. 호출이 느리면 쓰레드 풀 고갈이 일어나며 연쇄 장애(cascading failure) 위험이 높다.
  * 지연 누적 : 호출 체인이 A → B → C로 길어지면 응답 시간이 합산되어 누적될 수 있다.
  * 강한 위치 결합 : 서버 주소를 하드코딩해서 서버가 바뀌거나 인스턴스가 늘면 코드/설정을 수정해야 한다.
  * 성능 · 대역폭 : JSON 텍스트 직렬화라 gRPC 대비 무겁고 느리다.
  * 계약 취약성 : 스키마 강제가 약해 응답 필드가 바뀌면 런타임에 가서야 문제를 발견하게 된다.
  * N+1 호출 위험 : 목록마다 상대 서비스를 반복 호출하면 성능이 저하된다.
