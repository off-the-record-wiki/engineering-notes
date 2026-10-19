# TDD - Handling Unexpected Test Failures

#### 내부 설계에 의존하는 테스트

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0cf81275-d690-48ec-a18c-200b39f8463c.png)

* 기본값은 동작(입출력) 테스트. 내부 의존은 예외다.
* 예외가 정당한 유일한 조건
  * "출력이 같아도 내부가 다르면 버그인가?"에 대한 질문에 "예"라고 답할 수 있을 때(ex. 암호화 저장, 캐시 적중, 이벤트 발행, N+1 방지)
* 의존은 ASSERT에만, ACT는 공개 경계로, 행위는 바깥 문(HTTP API)으로 수행하고 확인만 필요한 최소 지점을 들여다본다.
* 내부 중에서 추상에 의존. 구현체가 아니라 포트에 단언해야 구현 교체에 살아남는다.
* 호출보다 상태. `verify(encode 호출됨)`보다 "저장된 값이 안전한 형태다"가 리팩토링에 강하다.

#### Spring 빈 등록

* 인터페이스는 추상 파트로 그 구현을 제공하지 않는다.
* 적절한 구현체를 Spring 빈으로 등록해야만 Spring 컨테이너가 적절한 빈을 제공할 수 있다.

#### [소프트웨어 회귀(Software Regression)](https://en.wikipedia.org/wiki/Software_regression)

* 소프트웨어 회귀는 이전에 작동했던 기능이 작동을 멈추는 소프트웨어 버그의 한 유형이다.
* 이는 새로운 기능 추가 및 버그 수정을 포함하여 소프트웨어 소스 코드에 변경 사항이 적용된 후 발생할 수 있다.

#### 단언문 가독성이란?

* 테스트의 `Assert` 부분을 읽었을 때, "이 코드가 무엇을 보장하는지"가 명세 문장처럼 바로 읽히는 정도를 말한다.
* **읽을 때: 의도가 문장으로 드러나는가**
* **실패할 때: 메시지가 원인을 말해주는가**

#### 테스트 실행 적절 범위

* TDD를 적용하면 개발 과정에서 수시로 테스트를 실행하게 된다.
* 테스트 실행 범위가 넓으면 많은 것을 확인할 수 있고 더 큰 안정감을 얻을 수 있지만 긴 시간이 소요된다.
* 테스트 실행 범위가 좁으면 빠르게 진행할 수 있지만 안정감은 줄어든다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/36601ab3-25e9-4662-b41d-d60e3b29f43b.png)
