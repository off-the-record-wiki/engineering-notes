# TDD - Managing System Data

#### 데이터베이스에서 시작하는 설계 주요 흐름

***

1. 테이블 스키마 설계에 요구사항을 투영한다.
2. 테이블 스키마를 반영하기 위해 입출력 계약 설계를 변경한다.
3. 데이터베이스에 CRUD 연산을 위임한다.

***

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/589831bc-42d6-490b-be73-13b492869b68.png)

* 데이터베이스 제약 조건을 통해 데이터의 일관성과 무결성을 유지하기 쉽다.
* 데이터베이스 스키마는 모델과 코드에 비해 변경 비용이 크기 때문에 시스템 설계 유연성이 낮아진다.
* 데이터베이스 스키마는 비즈니스 지식 표현력이 낮다.
* 작업 부하 분산이 어렵다.
* 응용 프로그램의 역할을 클라이언트와 데이터베이스 연산의 연결수단으로 바라본다.
* 인터페이스 설계가 데이터베이스 스키마에 논리적으로 의존하게 되어 변경이 까다롭다.
* 기능 및 비기능 요구사항 난이도가 낮고 변경이 적은 방식이라면 유용하다.

> [데이터베이스 중심 아키텍처란?](https://en.wikipedia.org/wiki/Database-centric_architecture)

#### 인터페이스에서 시작하는 설계 주요 흐름

***

1. 인터페이스 설계에 요구사항을 투영한다.
2. 인터페이스 명세에 기반해 클라이언트에 제공할 기능을 구현한다.
3. 기능 구현에 필요하다면 테이블 스키마 설계를 변경한다.

***

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/45f52bde-1ee6-450e-b8ab-169adc784223.png)

* 클라이언트에 제공되는 가치가 설계 결정의 주도권을 가진다.
* 비즈니스 지식이 시스템에 풍부한 어휘로 표현된다.
* 인터페이스 설계는 데이터베이스 스키마에 의존하지 않는다.
* 구현 기술을 다양하게 검토할 수 있다.

#### TDD를 사용하는 주요 설계 흐름

***

1. 인터페이스 설계에 요구사항을 투영한다.
2. 테스트 시나리오 목록을 작성한다.
3. 한 가지 항목을 테스트로 전환한다.
4. 테스트를 통과하도록 기능 구현한다.
5. 기능 구현에 필요하다면 테이블 스키마 설계를 변경한다.
6. 테스트 시나리오 목록이 비워졌는지 확인한다.

***

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/05f48ebb-23c7-4529-919b-45bf7eece7a7.png)
