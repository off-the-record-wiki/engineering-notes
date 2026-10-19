# Clean Spring - Hexagonal Architecture: Ports

#### 포트(Port)

* 애플리케이션이 외부 세계와 의도를 가지고 상호작용하는 아이디어를 캡처한 것이다.
* 단순히 데이터를 주고받는 것이 아니라 명확한 목적과 방향을 가지고 외부와 연결된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/32c6a8ca-a86b-4678-82e1-b55fe84be6de.png)

* 소통 의도를 기준으로 애플리케이션 포트를 구성한다.
  * 어댑터와 다른 애플리케이션 컴포넌트에서 필요한 인터페이스에만 의존할 수 있다.
  * 구현 클래스를 클린하게 재구성, 리팩터링 하기 쉽다.
  * Mock 테스트 작성이 편리하다.
  * 멀티 모듈 분리, 마이크로 서비스 등으로 확장이 편리하다.
