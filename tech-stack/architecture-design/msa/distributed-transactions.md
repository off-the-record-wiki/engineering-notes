# MicroService Architecture - Distributed Transactions

#### Transaction - ACID

* 트랜잭션 - 단일 작업 단위로 DB 작업의 context가 된다. 전체 Commit 혹은 전체 Rollback
* DB에서 변경 사항이 하나라도 실패하면 전체 트랜잭션이 실패하면서 DB가 이전 상태를 유지해야 한다.
* ACID - 원자성, 일관성, 격리 및 내구성
* RDB의 ACID 원칙 - 데이터 일관성을 보장한다. 강력한 일관성이 필요한 경우 사용한다.
  * Atomicity : 모든 작업이 성공적으로 실행되거나 모든 작업이 실패해 함께 롤백된다.
  * Consistency : DB의 데이터에 대한 모든 작업 이후에 유효하고 일관된 상태로 유지한다.
  * Isolation : 동시에 실행되는 분리형 트랜잭션. 서로 간섭하지 않는다.
  * Durability : 트랜잭션이 커밋된 이후 변경 사항은 내구성 있는 장소에 저장한다.

#### Bounded Context Pattern

* DDD 도메인은 자체 공통 언어를 정의한다.
  * 경계를 구체적, 독립적인 구성 요소로 나눈다.
  * 독립적인 단위 ⭢ Bounded Context가 된다.
* Bounded Context
  * 논리적 경계
  * 밀접하게 관련된 범위의 그룹화
* 복잡한 문제를 해결하기 위해 문제를 더 작은 부분으로 나누는 작업 
  * 비교적 작고 쉬운 문제에 집중
  * 복잡한 도메인에는 하위 도메인이 포함
  * 일부 하위 도메인은 공통 규칙과 책임을 위해 서로 결합하고 그룹화 

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/2cc2a28f-b27f-4ac0-81ea-e7c7ee77f5fd.png)

#### SAGA Pattern

* Saga 디자인 패턴
  * 분산 트랜잭션 ⭢ 데이터 일관성 관리
  * Pub/Sub 패턴
* 마이크로서비스를 순차적으로 업데이트
  * 서비스들의 다음 트랜잭션을 트리거 이벤트로 게시한다.
  * 여러 서비스들의 일련의 로컬 트랜잭션 ⭢ 전체 트랜잭션 관리
* 트랜잭션 단계 중 하나가 실패하면 Saga 패턴을 통해 트랜잭션을 롤백. 이전 마이크로서비스에 롤백 이벤트를 게시한다.(=보상 트랜잭션)

#### Choreography-based Saga
* 장점 : 분산화, 유연성. 단일 장애 지점을 피할 수 있다. 간단한 워크플로우 구성. 트랜잭션이 많지 않은 간단한 경우에 적합하다.
* 단점 : 복잡성 증가. 순환 이벤트 소비 위험 존재. 분산 시스템의 특정 요구사항과 제약 조건이 다르기에 일부 시스템에서는 적합하지 않다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ed747124-a749-481d-873b-2e28b0011281.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/33ef098f-efa2-4954-8c67-f143c1ef9cdc.png)

#### Orchestration-based Saga
* 장점 : 트랜잭션을 위한 명확하고 중앙 집중화된 제어/관리 제공. 시스템의 이해, 디버깅, 트랜잭션 추가 및 수정 용이. 많은 단계가 포함된 워크플로우에 적합하다.
* 단점 : 단일 장애 지점 발생 가능. 오케스트레이션 다운 시 전체 다운. 모든 트랜잭션이 오케스트레이션을 거치기 때문에 시스템에 부하가 많이 걸릴 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/59ff289b-48a2-4888-bbd8-0a1f305b4ae9.png)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5e8a2add-0435-4249-9ce4-abf0826450e8.png)

#### Compensating Transaction Pattern

* Saga 패턴의 롤백 프로세스
* 이전에 실행된 트랜잭션 단계를 역순으로 제공한다.
* 원래 작업의 단계별 실행 결과를 취소한다.
* 최종적으로 일관된 작업이며 실패할 수 있다. 시스템은 실패 지점에서 보상 트랜잭션을 재개하고 계속할 수 있어야 한다.
* 보상 트랜잭션의 단계 : 모두 `Idempotent` 명령으로 정의한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/83604d9d-8ccd-4281-b3f5-45e2481964ae.png)

#### Dual Write Problem

* 하나의 비즈니스 동작에서 서로 다른 두 저장소에 각각 쓰기를 해야 하는데, 두 쓰기를 하나의 트랜잭션으로 묶을 수 없을 때 생기는 문제이다.
  * Order 서비스가 DB에 주문을 저장한다.
  * Order 서비스가 Event Bus에 주문 생성 이벤트를 발행한다.
* DB와 메시지 브로커는 다른 시스템이기에 둘을 원자적으로 커밋할 수 없다. 어느 한쪽만 성공하는 순간이 반드시 존재한다.
 
![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4403fe9e-9322-4ee4-8cf3-9737adc6dcc5.png)

|순서|실패 지점|결과|
|:---:|:---:|:---:| 
|DB 쓰기 ⭢ 발행|발행 실패(브로커 다운, 네트워크, 프로세스 크래시)|DB에는 주문이 있는데 Stock/Delivery는 주문이 있는지 모른다. 재고 차감 및 배송이 영원히 안 된다.| 
|DB 쓰기 ⭢ 발행|발행은 됐는데 DB 커밋 실패(트랜잭션 안에서 발행한 경우)|주문은 없는데 Stock은 재고를 차감 ⭢ 유령 주문| 
|발행 ⭢ DB 쓰기|DB 쓰기 실패|위와 동일한 유령 주문| 
|어느 쪽이든|재시도|재시도하면 중복 발행/중복 저장 문제 발생| 

* **이중 쓰기 문제는 "DB와 브로커에 따로 쓰면 둘 중 하나만 성공하는 순간이 반드시 생긴다"는 것이고, 해법은 쓰기를 한 곳(DB)으로 모은 뒤 거기서 이벤트를 꺼내 발행하는 Outbox/CDC이며, 그 대가로 비동기 지연과 소비자 멱등성을 받아들이는 것이다.**

#### Transactional Outbox Pattern

* 이중 쓰기 문제의 해법 중 하나이다. "DB + 브로커" 두 곳에 쓰던 것을 "DB 한 곳"으로 줄여 원자성을 확보하고, 브로커 발행은 DB에서 꺼내는 후속 작업으로 분리한다.
* 마이크로서비스의 DB에 `outbox` 테이블에 저장한다. 이벤트를 안정적으로 게시할 수 있도록 제공하며 `outbox` 테이블에 모든 이벤트가 포함된다.
* 단일 트랜잭션으로 DB에 2개의 별도 데이터가 저장된다. API가 이벤트 메시지를 직접 게시하지 않으며, 메시지가 동일한 트랜잭션으로 DB 테이블에 저장한다.
* 하나의 데이터가 관련 DB 테이블과 `outbox` 테이블에 동시 저장한다. 정의된 시간 간격으로 Message Broker에 이벤트를 게시한다.
* 메시지를 트리거한 트랜잭션이 실패하더라도 마이크로서비스에서 다른 마이크로서비스로 메시지가 안정적으로 전달 가능하다.
* 아웃박스 테이블에서 레코드를 수신하고 폴링하는 마이크로서비스가 필요하다. 폴링하는 동안 시간/리소스 낭비 ⭢ 성능 저하로 이어진다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7d3dc24c-e6a0-41c8-9251-fd2857d425a6.png)

|컬럼|용도|
|:---:|:---:|
|`id`|이벤트 고유 ID - 소비자 멱등 처리의 기준|
|`aggregate_type`, `aggregate_id`|어떤 엔티티에 대한 이벤트인지 ⭢ 토픽, 파티션 키로 사용|
|`event_type`|이벤트 타입|
|`payload`|이벤트 본문(JSON)|
|`created_at`|발행 순서 보장용|
|`published_at`, `status`|릴레이 처리 상태(폴링 방식인 경우)|

* Message Transmitter를 구현하는 두 방식이 있다.
* 폴링 릴레이(Polling Publisher)
  * 스케줄러가 주기적으로 `SELECT ... WHERE published_at IS NULL ORDER BY created_at` ⭢ 발행 ⭢ UPDATE 처리한다.
  * 구현이 단순하고 인프라가 추가되지 않는다. 그러나 대신 폴링 주기만큼 지연, DB 부하, 다중 인스턴스 시 중복 발행 방지 필요하다.
* CDC(Changed Data Capture) - Debezium 등
  * DB 트랜잭션 로그를 읽어 `outbox` INSERT를 즉시 Kafka로 흘려보낸다. Debezium은 이 용도로 Outboux Event Router를 제공한다.
  * 지연이 거의 없고 DB 폴링 부하가 없다. 대신 Kafka Connect/Debezium 인프라가 붙고 운영 난이도가 올라간다. `outbox`행은 INSERT 직후 DELETE해도 로그에는 남아 있어 테이블이 비대해지지 않게 관리 가능하다.

#### CDC - Change Data Capture

* DB에 일어난 변경(INSERT/UPDATE/DELETE)을 실시간으로 포착해 다른 시스템으로 흘려보내는 기법이다.
* 애플리케이션이 이벤트를 따로 발행하는 것이 아니라 DB에 커밋된 사실 자체를 이벤트로 삼는다.

```java
orderRepository.save(order);   // ① DB 커밋
kafkaTemplate.send(event);     // ② 이벤트 발행
```

* DB와 메시지 브로커는 원자적으로 묶을 수 없기 때문에 이벤트를 같은 DB의 `outbox` 테이블에 넣는다. 그러면 하나의 로컬 트랜잭션이 되고 커밋 여부가 "주문 + 이벤트" 둘 다에 적용된다. 이벤트를 실제 브로커에 보내는 일은 CDC가 대신한다.
* 로그 기반을 사용하면 원본 DB 부하는 거의 0이 되고 이미 쓰인 로그를 읽어 처리한다.
* `At-least-once` : CDC 커네겉가 오프셋 커밋 전에 죽으면 같은 이벤트가 다시 나간다. 컨슈머가 멱등해야 한다.
* 순서 : 같은 테이블 · 같은 파티션 키 안에서는 커밋 순서가 유지된다. 파티션이 갈리면 순서 보장이 없다.
* 지연 : 보통 수십 ms ~ 초 단위. 실시간이지만 동기는 아니다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/6d6b7b0d-ef94-4fe2-b43e-60de885994dc.png)

* **"이벤트를 발행하지 말고, DB에 적어라. 발행은 로그를 읽는 CDC가 한다." — 이중 쓰기 문제를 로컬 트랜잭션 하나로 해결하는 것이 outbox + log-based CDC의 전부다.**
