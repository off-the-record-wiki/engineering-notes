# MicroService Architecture - Event-Driven Architecture

#### Event-Driven Architecture 

* 마이크로서비스는 서로의 API를 직접 호출하는 것 대신 이벤트 메시지를 발행(Publish) 및 구독(Subscribe)하여 통신 가능하다.
* 데이터가 필요할 때 요청 대신 서비스에서 이벤트를 소비한다.
* 이벤트를 통한 서비스 간 비동기 소통으로 느슨한 결합을 달성할 수 있어 분리된 통신이 가능하다. 독립적, 서비스간 의존성을 최소화할 수 있으며 확장성 및 유연성 향상을 도모할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/eef911aa-c908-4003-a176-1df5eeddde33.png)

* 실시간 메시징 플랫폼
  * 메시지 큐나 이벤트 버스를 사용한다.
  * 이벤트가 발생하는 대로 Publish ⭢ Subscribe
  * CDC 사용 ⭢ DB 변경 사항 추적 시스템(실시간 이벤트)
* 대용량 처리(이벤트)
  * 필요에 따라 이벤트 소비자 추가 ⭢ 수평적 확장이 가능
  * 이벤트 버스 ⭢ 이벤트를 병렬로 처리, 분배 가능
  * 시스템의 다른 서비스들이 서로 간에 충돌 없이 사용 가능

| 구분 | Event Sourcing | Event Driven Architecture(EDA) |
|:---:|:---:|:---:|
| 목적 | 상태 관리 및 복원 | 서비스 간 비동기 통신 및 느슨한 결합 |
| 이벤트 활용 방법 | 상태 변화 자체를 이벤트로 기록 | 서비스 간 소통을 위한 이벤트 메시지 전달 |
| 주요 특징 | 상태를 이벤트 로그로 기록하고 Replay | 이벤트 Publish/Subscribe를 통한 비동기 처리 |
| 저장소 활용 여부 | 이벤트 스토어 필수 | 이벤트 스토어 필수 아님 |
| 상태 복원 가능 여부 | 이벤트 Replay | 상태 복원 자체와는 무관 |
| 주요 장점 | 감사 가능성, 상태 복원성 높음 | 시스템 확장성 및 유연성 증가 |

#### ❗카프카 기반 EDA에서 토픽·메시지 설계의 핵심 철학 정리

**1. 먼저 "무슨 이벤트인가"를 정한다.
* Event Notification : "일어났다"만 알림. ID 위주. 컨슈머가 적고 최신 상태를 직접 조회해도 될 때.
* Event-Carried State Transfer : 변경 후 상태 스냅샷을 함께 실음. 컨슈머가 원본 서비스를 조회하지 않도록 하고 싶을 때.
* Delta/Changed-fields : 바뀐 필드만. 대용량 엔티티, CDC 계열.
* Command : "해라". 수신자가 1명으로 정해지는 경우.

**2. 토픽 설계(네이밍) - 규칙을 문서가 아니라 이름 자체에 박는다.**
```
<domain>.<entity>.<event-type>.<version>      # 권장 기본형
order.order.created.v1
payment.payment.captured.v1
inventory.stock.reserved.v1

<scope>.<domain>.<entity>.<type>.v<n>         # 공개 범위까지 구분하는 형태
pub.order.order.created.v1     # 다른 팀이 구독 가능한 공개 계약
int.order.saga.step.v1         # 내 팀 전용 (언제든 바꿔도 됨)
```
* 이름은 과거형 사실로 기재한다. ⭢ `order.created` (O) / `create-order` (X)
* 비즈니스 언어를 쓴다. ⭢ `order.cancelled` (O) / `order.status.updated` (X)
* 구분자 하나만 쓴다. 카프카 메트릭에서 `.`과 `_`가 충돌하므로 `_`는 피하는 편이 안전하다.
* 버전을 토픽 이름에 넣을지는 선택이나 파괴적 변경이 필요한 경우 `v2` 토픽을 병행 운영하는 전략이 가장 안전하다.

**3. 토픽 설계(토픽 단위)**
* "이벤트 타입별 1토픽"과 "엔티티별 1토픽" 사이의 선택이다.
```
A안) 타입별 분리
order.created.v1 / order.paid.v1 / order.cancelled.v1

B안) 엔티티 단위 통합 (하나의 토픽에 여러 event_type)
order.events.v1   ← created/paid/cancelled 를 모두 담되 eventType 필드로 구분
```
* 판단 기준은 "순서 보장이 필요한가" 하나이다.
  * 같은 주문에 대해 created → paid → cancelled 순서가 반드시 지켜져야 한다. ⭢ B안. 카프카는 파티션 내에서만 순서를 보장하므로, 순서가 중요한 이벤트는 반드시 같은 토픽·같은 키여야 한다. 토픽을 쪼개는 순간 순서 보장은 사라진다.
  * 이벤트들이 서로 독립적이고 컨슈머가 완전히 다르다. ⭢ A안. 구독 시 불필요한 트래픽을 안 받는다.

**4. 파티션 키**
* 키는 라우팅 값이 아니라 순서 보장 단위이자 동시성의 최대치를 정의한다.
```
key = orderId     → 같은 주문의 이벤트는 항상 같은 파티션 = 순서 보장
key = customerId  → 고객 단위 순서 보장 (주문 간 순서도 보장해야 할 때)
key = null        → 순서 무보장, 라운드로빈 (로그/메트릭성 이벤트)
```
* 키의 카디널리티가 낮으면 핫 파티션이 생긴다.
* 파티션 수는 늘릴 수 있지만 그 순간 키 ⭢ 파티션 매핑이 깨져 순서 보장이 무너진다. 처음부터 목표 처리량의 2~3배로 잡는다.
* 키는 비즈니스 식별자여야 한다. DB PK나 UUID 난수를 키로 사용할 경우 나중에 재파티셔닝, 재처리에서 어려워진다.

**5. Retention vs Compaction**
* `delete + 7d` : 시간 지나면 삭제. 일반 도메인 이벤트에 적합.
* `compact` : 키별 최신 값만 영구 보존. 상태 스냅샷 토픽.
* `compact,delete` : 최신 값 유지 + 오래된 건 삭제.

**6. 메시지(페이로드) 설계**
* 메타데이터와 도메인 데이터를 섞지 않는다.
```json
{
  "_comment" : "UUIDv7/ULID — 멱등 처리의 기준키",
  "eventId": "01J8XQ...",
  "eventType": "order.created",
  "eventVersion": "1.2",

  "_comment" : "도메인에서 사실이 발생한 시각",
  "occurredAt": "2026-09-14T09:12:03.421Z",
  "producer": "order-service",

  "_comment" : "분산 추적",
  "traceId": "4bf92f...",

  "_comment" : "하나의 비즈니스 흐름 묶음",
  "correlationId": "req-8821",

  "_comment" : "이 이벤트를 유발한 직전 이벤트 id",
  "causationId": "01J8XQ...",
  "data": {
    "orderId": "ORD-10293",
    "customerId": "CUST-88",
    "totalAmount": { "value": "45000", "currency": "KRW" },
    "items": [ { "sku": "A-1", "qty": 2 } ],
    "status": "CREATED"
  }
}
```
* `eventId` : 반드시 넣는다. 카프카는 `at-least-once`가 기본이라 중복은 언젠가 반드시 발생하고, 컨슈머는 이 id로 멱등 처리를 한다.
* `occurredAt`(도메인 시각)과 카프카의 메시지 `timestamp`(발행 시각)는 다르다.
* `causationId`/`correlationId`는 Saga 패턴 디버깅에서 가치가 압도적이다. 코레오그래피 구조에선 이게 없으면 장애 추적이 사실상 불가능하다.
* Enum은 문자열.

**7. 키/헤더/페이로드의 역할 분담**
* Key : 순서 · 파티셔닝(`orderId`)
* Header : 라우팅 · 인프라 관심사 — `event_type`, `schema_version`, `content_type`, `traceparent`, `tenant_id`, `retry_count`. 본문을 역직렬화하지 않고도 필터링·라우팅할 수 있게 해준다.
* Value : 비즈니스 데이터
* 헤더만 보고 처리 여부를 판단할 수 있으면 컨슈머·커넥터·DLQ 처리기가 훨씬 단순해진다.

**8. 스키마는 코드가 아니라 계약으로 관리한다.**
* JSON을 문서로만 합의하는 방식은 굉장히 위험하다.
* Avro 또는 Protobuf + Schema Registry를 권장한다. Avro는 카프카 생태계(Connect/ksqlDB)와 궁합이 좋고, Protobuf는 gRPC와 스키마를 공유하기 좋다.
* 호환성 정책은 BACKWARD가 기본이다.
* 진화 규칙
  * ✅ 기본값 있는 optional 필드 추가
  * ✅ 필드 문서/주석 변경
  * ❌ 필드 삭제·이름 변경·타입 변경·의미 변경(가장 위험. 스키마는 통과하는데 컨슈머 로직이 조용히 틀어진다)
  * 파괴적 변경이 필요하면 → v2 토픽 신설 + 일정 기간 듀얼 발행 → 컨슈머 이관 → v1 폐기

**9. 크기**
* 메시지는 작게 유지(대략 1MB 미만, 실무 권장은 수십~수백 KB). 이미지·대용량 문서는 Claim-Check 패턴 - S3에 저장하고 이벤트엔 URI + 체크섬을 싣는다.

**10. 신뢰성 — 설계 철학의 절반**
* "DB 저장 후 카프카 발행"을 두 번의 네트워크 호출로 하면 반드시 유실/유령 이벤트가 생긴다. 같은 로컬 트랜잭션에서 도메인 테이블 + outbox 테이블에 함께 쓰고, CDC(Debezium)가 커밋 로그를 읽어 카프카로 보낸다.

**11. 전달 보장**
* 프로듀서 : `acks=all`, `enable.idempotence=true`, `max.in.flight=5`, `retries=Integer.MAX`
* Exactly-once(트랜잭션)는 카프카→카프카 처리(Streams)에서만 실질적으로 성립. 외부 DB · API가 끼면 성립하지 않는다.
* 따라서 기본 전제는 at-least-once + 컨슈머 멱등성이다. `eventId`를 처리 이력 테이블에 UNIQUE로 넣거나, UPSERT/상태 전이 검사로 중복을 흡수한다.
* 오프셋 커밋은 처리 완료 후 수동 커밋이 원칙이다.

**12. 실패 처리 — Retry + DLQ**
```
order.created.v1
  → order.created.v1.retry.5s
  → order.created.v1.retry.1m
  → order.created.v1.dlq
```
* 재시도 가능한 오류(네트워크, 일시적 장애)와 불가능한 오류(스키마 불일치, 비즈니스 규칙 위반)를 반드시 구분한다.
