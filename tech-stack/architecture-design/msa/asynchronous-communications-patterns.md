# MicroService Architecture - Asynchronous Communications Patterns

### MicroService Architecture - Asynchronous Communications Patterns

#### 비동기 통신에서의 메시지 처리 방법

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f5b14e74-fb09-4d55-b877-1510fe0b20db.png)

* Single-Receiver Meesage-based Communication(단일 수신자 비동기 일대일 통신)
* One to One 또는 Point to Point
* 단일 생산자와 단일 수신자(Command Pattern으로 구현)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c644cd78-c665-4ba1-9805-c0fe0d1637e0.png)

* Multiple-Receiver Message-based Communication(여러 수신자가 있는 게시/구독 메커니즘)
* 생산자가 메시지를 발행 → 메시지 브로커 시스템이 메시지를 구독
* 이벤트 기반 마이크로서비스 아키텍처 패턴
* 생산자는 소비자를 알 필요가 없어야 한다. → 서비스 간의 종속성을 만들지 않기 위함이다.
* One to Many → 여러 마이크로서비스에서 소비하며 각 요청은 0~N개의 수신자가 처리할 수 있으며, 메시지는 토픽에 영구적으로 유지된다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/70068754-2a4a-4977-be23-889748fd5bf3.png)

* 게시자/구독자 모델
  * 비동기적으로 메시지 통신이 가능하다.
  * 논리적 액세스 지점인 Topic을 정의한다.
* **Fan-Out**
  * Pub/Sub Messaging Pattern
  * 여러 목적지에 병렬로 fan-out
  * 각 목적지는 메시지를 병렬로 작업 처리한다.
  * 여러 수신자에게 동일한 메시지를 전달한다.

#### Publish/Subscribe 패턴

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/d97a8318-0641-4b0d-bf4e-bc82fab38181.png)

* Pub/Sub Messaging - 비동기 서비스 간 통신, Topic에 발행된 모든 메시지는 Topic의 모든 구독자에게 즉시 수신된다.
* Event Driven Architecture - 이벤트 기반 아키텍처를 활성화, 애플리케이션을 분리해 성능 안정성 및 확장성을 유도한다.
* 애플리케이션은 개발, 배포 및 유지 관리가 더 쉽도록 분리할 수 있다.

> Topic-Queue Chaining

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/aafa4241-8a29-4ad4-b648-f988648d5354.png)

> * 서비스가 실패할 경우 → 데이터 손실 방지를 위한 서비스 간의 버퍼링 로드 밸런서 역할(Queue를 사용)<br>
> * 서비스 다운/예외 발생/유지 관리를 위해 오프라인 전환될 때 → 이벤트가 손실되거나 사라질 수 있는데 이렇게 되면 구독자 서비스가 가동되어도 처리가 불가능하다.<br>
> * AWS SQS Queue에 저장하면 메시지가 손실되지 않는다.<br>
* 요청 시작 → 클라이언트 애플리케이션이 API Gateway를 통해 HTTP 요청을 보낸다.
* 주문 처리 및 기록 → Lambda가 요청을 받아 처리 후 주문 정보를 DynamoDB에 저장해 상태를 기록한다.
* 이벤트 발행 → 서비스가 메시지를 Amazon SNS로 발행한다.
* 필터링 및 분산 → SNS에 설정된 Event Filter를 통해서 필요한 메시지만 선별되고 선별된 메시지는 연결된 여러 개의 Amazon SQS 대기열로 복제되어 전달된다.
* 비동기 소비 - 각 목적에 맞는 서비스들이 SQS에서 메시지를 가져가 독립적으로 작업을 수행한다.
