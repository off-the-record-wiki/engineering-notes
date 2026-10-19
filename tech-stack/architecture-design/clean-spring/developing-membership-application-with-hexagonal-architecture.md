# Clean Spring - Developing Membership Application with Hexagonal Architecture

#### [헥사고날 아키텍처](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software))

* 2005년 엘리스터 코번이 제안한 아키텍처이다.
* 계층형 아키텍처의 단방향 비대칭 구조가 아닌 대칭형 아키텍처이다.
* 위-아래, 좌-우가 아닌 애플리케이션의 내부와 외부 세계라는 대칭 구조를 가진다.
* 아키텍처의 대칭성을 가진 구조를, 그리기 쉬운 대표적인 육각형으로 설명한 것으로 육각형, 6개의 면을 가졌다는게 특별한 의미를 가지는 것은 아니다.

> **헥사곤의 내부**
> * 쉽게 변하지 않는 중요한 도메인 로직을 담은 코어 애플리케이션
> * 애플리케이션 서비스와 도메인 모델 패턴을 따라서 만든 도메인

> **헥사곤의 외부**
> * 헥사곤과 상호작용하는 모든 것 - 액터(Actor)
> * 사용자, 브라우저, CLI 명령, 기계, 다른 시스템
> * 운영 환경, DB, 메시징 시스템, 메일 시스템, 원격 서비스
> * 테스트

#### 헥사고날 아키텍처의 특징과 혜택

* 운영 시스템에 연결되지 않고 애플리케이션 테스트한다.
* 애플리케이션과 상호작용하는 액터가 바뀌더라도 다시 빌드하지 않고 테스트한다.
* UI 디테일이나 기술 정보가 도메인 로직 안으로 노출되지 않도록 보호한다.
* 컴포넌트를 각각 개발하고 연결하는 방식으로 큰 시스템을 분리할 수 있다.
* 시간이 지나면서 외부 연결을 다른 것으로 변경할 수 있다.
* 기술 요소를 제거했기 때문에 도메인 설계에 집중할 수 있다.

#### 포트(Port)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5c3df246-b1e4-4567-959b-7ad5a5921727.png)

* 애플리케이션이 외부 세계와 의도를 가지고 상호작용하는 아이디어를 캡처한 것을 말한다.
* 단순히 데이터를 주고받는 것이 아니라 명확한 목적과 방향을 가지고 외부와 연결된다.
* 애플리케이션이 정의한 인터페이스로 만들어진다.

#### 기능 요구 인터페이스(Required Interface)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/e5df4fdc-340e-46a1-95f4-7f5e95405bbd.png)

* 제공 인터페이스(Provided Interface) : 애플리케이션이 외부에 제공하는 기능의 목록이다.
* 요구 인터페이스(Required Interface) : 애플리케이션이 자기 일을 완수하기 위해 외부에 요구하는 기능의 목록이다.

#### 어댑터(Adapter)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6d3a65db-93a6-4c2d-a28d-d87276a860f4.png)

* 애플리케이션의 포트를 액터가 직접 연결할 수 없다면 인터페이스의 변환을 위한 어댑터를 도입한다.
* 브라우저를 통해서 애플리케이션의 회원 가입 포트의 기능 제공 인터페이스를 사용한다면?
  * 회원 가입 기능 제공 인터페이스를 사용하는 웹 컨트롤러 어댑터를 만든다.
* 애플리케이션이 가진 회원 정보 저장 포트의 기능 요구 인터페이스로 DB와 직접 연결할 수 없다면?
  * 기능 요구 인터페이스를 구현한 리포지토리 어댑터를 만든다.

#### 오해 1 : 애플리케이션 내부에 도메인 계층을 만들어야 한다.

* 헥사고날 아키텍처는 애플리케이션 내부 구현에 대한 원칙이나 요구사항이 없다.
  * 스파게티 코드로 만들어도 된다.
  * 트랜잭션 스크립트, 도메인 모델 패턴과 애플리케이션 서비스 등도 가능하다.
  * 도메인 계층을 포함하는 아키텍처는 클린 아키텍처이다.
* 헥사고날 아키텍처는 클린 아키텍처, 어니언 아키텍처가 아니다.

#### 오해 2 : 헥사고날 아키텍처 패키지 구조를 따라야 한다.

* 헥사고날 아키텍처가 요구하는 패키지 구조는 없다.
* 애플리케이션과 어댑터 패키지를 분리하는 것은 바람직하다.
* 포트를 구분된 패키지에 두는 것을 권장한다.

#### 오해 3 : 포트는 UseCase라는 접미사를 사용한다.

* 포트의 의도를 담은 이름을 사용하면 된다. 
* `For + ~ing` 스타일의 권장 네이밍이 있지만 이를 반드시 따를 필요는 없다.

#### 오해 4 : 애플리케이션에는 도메인 모델만 넣고 JPA 엔티티 등은 어댑터에 둬야 한다.

* 애플리케이션 코드와 포트 인터페이스가 외부 기술에 의존하지 않으면 된다.

#### 사실 : 헥사고날 아키텍처가 요구하는 것

* 애플리케이션은 모든 외부와의 상호작용을 위해서 provided interface와 required interface를 정의한다.
* 애플리케이션과 상호작용하는 액터는 런타임에 구성되어야 한다.
* 애플리케이션은 액터에 대한 코드 의존성을 가지면 안 된다.
* 액터는 정의된 포트를 통해서만 연결해야 한다.
* 포트의 인터페이스에는 기술 의존성을 가지지 않는다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fc653abb-c38f-4096-b345-4e7267affc98.png)

#### 헥사고날과 도메인 모델 패턴을 적용한 대칭형 계층 구조

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6d083a7a-1b67-4b34-8d74-1a4f6653eb4c.png)

* 외부에서 내부로 향하는 일종의 계층 구조이다.
* 코드의 의존 방향은 내부로만 향해야 한다. 즉, 어댑터 → 애플리케이션 → 도메인이 된다.
* 단, 사용의 흐름은 비대칭적이다.

#### 엔티티 식별자와 JPA 엔티티

* 고유성 : 두 개의 엔티티가 같은 값을 가지면 안 된다.
* 불변성 : 한 번 값이 할당되면 엔티티의 생명주기 동안 절대 변경되면 안 된다. 참조 정합성을 훼손하고, 연관관계를 깨뜨린다.

#### 레퍼런스

> * [hexagonal architecture?](https://medium.com/@hello-every-one/hexagonal-architecture-3729e9a9200b)
> * [Hexagonal Architecture, 진짜 하실 건가요?](https://tech.kakaopay.com/post/home-hexagonal-architecture/)
> * [Hexagonal architecture pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/hexagonal-architecture.html)
> * [지속 가능한 소프트웨어 설계 패턴: 포트와 어댑터 아키텍처 적용하기](https://engineering.linecorp.com/ko/blog/port-and-adapter-architecture)
