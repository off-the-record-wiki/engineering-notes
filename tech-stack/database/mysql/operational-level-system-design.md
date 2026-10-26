# MySQL ‐ Operational‐Level System Design

#### 실시간 변동 데이터 처리를 위한 스트리밍 데이터 처리 기법

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/832bba94-de3a-49de-bf0b-6d38290f3ddf.png)

**1. Data Collection — 데이터가 발생하는 곳**
* Web/App Server : 사용자가 앱에서 클릭·검색·결제 → 서버가 "클릭 이벤트" 발생한다.
* IoT Device: 센서가 온도·위치 값을 계속 측정해서 발생한다.
* Database CDC : DB에 주문이 INSERT되면 그 변경을 감지해 이벤트가 발생(CDC = Change Data Capture, "DB 바뀐 것 실시간 감지")한다.

**2. Stream Storage — 일단 큐에 모임(Kafka)**
* 발생한 이벤트를 곧바로 처리하지 않고 메시지 큐(Kafka)에 잠깐 쌓는다.
  * 완충(버퍼) : 갑자기 트래픽이 폭주해도 큐가 받아내고, 뒷단은 자기 속도로 소비할 수 있다.
  * 분리(디커플링) : 데이터 만드는 쪽과 처리하는 쪽이 서로 몰라도 됨 → 독립적으로 확장·교체 가능하다.
  * 여러 소비자 : 같은 이벤트를 여러 시스템이 나눠 읽을 수 있다.

**3. Stream Processing — 실시간으로 가공(Flink)**
* 스트림 프로세서가 큐에서 이벤트를 도착하는 즉시 꺼내서 계산한다.
* 데이터가 오는 순간 바로 처리해서 지연이 초 단위이다.

**4. Data Serving — 결과를 여러 곳에서 사용**
* 가공한 결과를 필요한 곳으로 팬아웃(fan-out)한다.

> 즉, 여러 소스에서 끊임없이 발생한 데이터를 Kafka에 모아 완충하고 스트림 프로세서가 즉시 가공해서 대시보드·알림·ML·DW 등 여러 곳에 실시간으로 공급하는 구조를 만들 수 있다.

#### 수천억대 데이터 처리를 위한 초대용량 배치 처리 방법

* 기존 배치는 스케줄러가 하나의 `INSERT ... SELECT`를 던지고, 스캔·그룹핑·집계·저장을 전부 운영 MySQL이 처리하는 구조다. 
* 데이터가 수천만 건일 때는 버티지만, 수억 ~ 수천억 건으로 늘면 세 갈래로 무너진다.
  * 처리 시간 증가 : 단일 스레드·단일 노드로 전 구간을 순차 스캔·집계 → 데이터가 커질수록 배치 윈도우를 초과한다.
  * DB 부하 → 서비스 영향 : 대량 스캔이 버퍼풀·I/O·CPU를 점유해 같은 DB를 쓰는 실서비스 응답이 함께 느려진다.
  * 장애 대응의 어려움 : 한 트랜잭션으로 8시간 돌다 실패하면 처음부터. 재시작·부분복구·멱등성 보장이 어렵다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e9eee5eb-ccd5-4d45-9cf2-5d8ae30ab216.png)

**해결 개념 - 연산을 DB 밖으로 위임한다**
* DB에게 집계를 시키지 말고, 데이터만 병렬로 꺼내와 밖에서 나눠 계산한다.
* Spark가 이 문제에 주는 두 가지 실질 이점이 있다.
  * 계산 위임 — GROUP BY·SUM 같은 무거운 연산을 MySQL이 아니라 Spark 클러스터가 수행한다.
  * 병렬 데이터 로딩 — 하나의 큰 테이블을 order_id 범위로 쪼개 여러 Executor가 동시에 나눠 읽는다. 순차 스캔이 병렬 스캔으로 바뀐다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/41fd6e1e-7bfd-428c-9965-2c32e2dd3e9e.png)

**1. 병렬 읽기용 뷰 정의**

* MySQL의 orders를 `partitionColumn` 기준으로 쪼개 여러 Executor가 동시에 읽도록 임시 뷰를 만든다. 
* `numPartitions`만큼의 범위 쿼리가 병렬로 나간다.

```sql
CREATE OR REPLACE TEMPORARY VIEW orders_view
USING "jdbc" OPTIONS (
  url             "jdbc:mysql://mysql-server:3306/service_db",
  dbtable         "orders",
  partitionColumn "order_id",   -- 나눌 기준 (숫자·PK 권장)
  lowerBound      "1",          -- 최소값
  upperBound      "100000000",  -- 최대값
  numPartitions   "100"         -- 100개로 분할 → 병렬 읽기
);
-- Executor 1  : WHERE order_id >= 1        AND order_id < 1000000
-- Executor 2  : WHERE order_id >= 1000000  AND order_id < 2000000
-- Executor 100: WHERE order_id >= 99000000 AND order_id <= 100000000
```

**2. 집계는 Spark 엔진이 수행한다.**

* 이 쿼리는 더 이상 MySQL이 아니라 Spark 엔진이 인메모리로 실행한다. 
* `WHERE created_at` 조건은 `Predicate Pushdown`으로 읽기 단계에서 DB에 밀어넣어 스캔량을 줄이고, `GROUP BY`는 노드 간 셔플링을 거쳐 집계된다.

```sql
CREATE OR REPLACE TEMPORARY VIEW daily_summary AS
SELECT product_id,
       SUM(sale_price * quantity) AS total_amount,
       COUNT(*)                    AS total_count
FROM orders_view
WHERE created_at >= '2024-05-20 00:00:00'
  AND created_at <  '2024-05-21 00:00:00'   -- ← Predicate Pushdown
GROUP BY product_id;                        -- ← Shuffle 발생
```

**3. 결과를 분석 DB에 저장한다.**
* 집계 결과는 운영 DB가 아닌 별도의 분석용 DB에 적재해 서비스와 격리한다.

```sql
INSERT INTO analytics_db.daily_product_sales
SELECT * FROM daily_summary;
```

#### 실시간 데이터 동기화를 위한 디자인 설계 패턴

**1. 문제 : 하나의 변경, 여러 목적지**
* Dual Write : 애플리케이션이 DB에 쓰고 또 Kafka에도 직접 쓴다. 두 쓰기는 한 트랜잭션이 아니라, 하나가 실패하면 시스템 간 데이터가 영구히 어긋난다.
* 주기적 배치 폴링 : `updated_at`을 N분마다 훑기. 실시간이 아니고, 매 폴링이 운영 DB에 부하를 주며, 삭제(DELETE)는 감지조차 못 한다.

> 핵심 : 이미 정합성이 보장된 단 하나의 소스. DB의 트랜잭션 로그를 진실의 원천으로 삼는다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9e9139ab-d733-4b56-be69-37ff2b38cd66.png)

#### CDC 파이프라인 — 4개 계층

**1. Source — MySQL (Binlog)**
* 모든 변경이 이미 binlog에 순서대로 기록된다.
* CDC의 전제 조건은 binlog_format=ROW 활성화이다.

**2. CDC — Debezium Connector**
* MySQL의 복제 슬레이브인 척 `binlog`를 구독해 변경을 표준 이벤트로 변환한다.
* 애플리케이션은 이 존재를 모른다.

**3. Streaming — Kafka Topic**
* 변경 이벤트의 완충·보존·순서 보장 버퍼. 소비자가 잠시 죽어도 이벤트는 토픽에 남아 재생 가능하다.

**4. Consumers**
* 검색 · 분석 · 캐시 · 각자 독립적으로 같은 토픽을 구독한다. 
* 소비자를 추가해도 생산 측은 전혀 바뀌지 않는다. 이것이 이벤트 브로커의 디커플링 이점이다.

> 왜 Kafka를 중간에 두나? - Debezium이 소비자에 직접 밀어주면, 소비자 하나가 느리면 전체가 막히고 재처리도 어렵다. Kafka가 이벤트를 보존하기에 소비자별 속도 차이·장애·나중 합류(replay)를 모두 흡수한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/db87060b-a92f-4680-a448-f10bcaa5a3b2.png)

**1. Debezium 동작 원리 — 스냅샷 → 스트리밍**
* 커넥터가 처음 붙을 때 풀어야 하는 문제 : 지금까지 쌓인 데이터(과거)와 앞으로 바뀔 데이터(미래)를 빠짐도 중복도 없이 이어붙이는 것이다. 이 때, 순서가 핵심이다.
* 짧은 Lock으로 기준점 고정 : 아주 짧게 `GLOBAL READ LOCK`을 잡고 현재 binlog 위치를 저장한 뒤 즉시 해제한다. 이 시점이 과거와 미래의 경계선이 된다.
* Lock 해제 후에도 변경은 안전 : 해제 순간부터의 변경은 그대로 binlog에 쌓이므로 유실되지 않는다. 그래서 Lock을 오래 잡을 필요가 없다(서비스 영향 최소).
* 초기 스냅샷 : 전건을 'c' 이벤트로 · 테이블 전체를 SELECT해 “현재 상태”를 Create 이벤트로 Kafka에 채운다. 소비자 입장에선 모든 행이 “새로 생성된” 것처럼 보인다.
* 저장한 위치부터 스트리밍 : 스냅샷이 끝나면 저장해둔 binlog 위치로 돌아가 'u'/'d' 이벤트를 순서대로 흘린다. 경계선 이후 것만 이어지므로 이중 반영이 없다.

> 최신 Debezium은 Lock을 더 줄인다. 위 흐름은 Lock 기반 스냅샷의 표준 모델이다. 실제로는 Incremental Snapshot(무Lock, 청크 단위 워터마크)으로 스냅샷과 스트리밍을 동시에 진행해 운영 부담을 더 낮추는 방식이 널리 쓰인다.

**2. 변경 이벤트 메시지 구조**
* Debezium의 이벤트는 “무엇이, 어떻게 바뀌었는지”를 담는 표준 envelope다. op는 연산 종류, before/after는 변경 전후 상태이다.
```sql
{
  "op": "u",                 // c=create, u=update, d=delete, r=snapshot read
  "ts_ms": 1721457600000,
  "before": { "id": 42, "price": 1000, "status": "OPEN" },
  "after":  { "id": 42, "price": 1200, "status": "OPEN" },
  "source": { "db": "service_db", "table": "orders",
              "file": "binlog.000042", "pos": 15832 }
}
```
* Kafka 메시지 Key = PK : 같은 레코드의 이벤트가 항상 같은 파티션으로 가도록 키를 PK로 둔다 → 그 레코드에 한해 순서가 보장된다.
* 삭제는 'd' + tombstone : Delete 이벤트 뒤에 `value=null` 툼스톤을 보내, 키 기반 log compaction 토픽에서 실제로 지워지게 한다.
* 'r' vs 'c' : 초기 스냅샷은 read(r), 실시간 생성은 create(c). 소비자는 대개 둘을 동일하게 “upsert”로 처리한다.

#### 설계 고려사항

* 주의 1 : 순서 보장은 “파티션 단위”까지만. 전역 순서는 없다. PK를 파티션 키로 삼아 레코드별 순서를 확보하고, 전역 순서가 필요하면 파티션 1개(=처리량 희생)를 감수한다.
* 주의 2 : 멱등 소비자(Idempotent Consumer) : Kafka는 기본 `at-least-once`로 같은 이벤트가 재전송될 수 있다. 소비 측을 UPSERT·버전 비교로 멱등하게 만들어 중복을 무해화한다.
* 주의 3 : Eventual Consistency 수용 : 소비자는 소스보다 항상 조금 뒤처진다. “강한 일관성”이 필요한 화면은 CDC 대상에서 제외하거나 소스를 직접 읽는다.
* 주의 4 : 스키마 진화 대비 · ALTER TABLE은 이벤트 스키마를 바꾼다. Schema Registry + 호환성 규칙(backward)으로 소비자가 깨지지 않게 한다.
* 주의 5 : 실패 격리 DLQ : 특정 이벤트가 소비 중 계속 실패하면 파이프라인 전체가 멈춘다. Dead Letter Queue로 밀어내 나머지를 계속 흐르게 하고 나중에 재처리한다.
* 주의 6 : Transaction Outbox로 Dual Write 봉쇄 : 애플리케이션이 이벤트를 발행해야 한다면, 비즈니스 데이터와 함께 outbox 테이블에 한 트랜잭션으로 넣고 그 테이블을 CDC로 흘린다.
* 주의 7 : 재적재(Backfill) 경로 확보 : 소비자 로직 변경·유실 시 스냅샷을 다시 태워 전량 재구성할 수 있어야 한다. Kafka 보존기간·compaction 정책을 이에 맞춰 설계한다.

#### 견고한 비동기 작업을 위한 MySQL 작업 큐 활용법

**1. 문제 : 동기 처리 안티패턴**
* 회원가입이 부가 작업까지 전부 끝나야 응답한다. 사용자 저장은 빠르지만, 이메일·쿠폰은 외부 의존이라 느리고 불안정하다.
```java
// Anti-pattern: Synchronous Processing
public void signUp(User user) {
    userRepository.save(user);                         // 1. 빠름
    emailService.sendWelcomeEmail(user.getEmail());    // 2. 느릴 수 있음
    couponService.issueSignUpCoupon(user.getId());    // 3. 느릴 수 있음
    // 4. 모든 작업이 끝나야 사용자에게 응답이 감
}
```
> 핵심 : 핵심 로직만 즉시 처리하고, 나머지는 나중에 처리한다. 부가 작업을 Job으로 만들어 큐에 넣고, 응답은 바로 돌려준다.

**DB를 큐로 쓴다.**
* 메시지 브로커(Kafka·SQS) 없이 DB 테이블 하나로 큐를 구현하는 패턴으로 규모가 폭발하기 전까지 가장 실용적이다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a5ae5348-176a-4224-9004-18251069dfe8.png)

**jobs 테이블 설계**
* 테이블 하나가 큐이자 상태 저장소이자 감사 로그가 된다. 컬럼 하나하나가 특정 요구를 담당한다.
```sql
CREATE TABLE `jobs` (
  `id`            BIGINT AUTO_INCREMENT,
  `job_type`      VARCHAR(50)  -- SEND_EMAIL, ISSUE_COUPON …
  `payload`       JSON,        -- 작업에 필요한 데이터 (user_id 등)
  `status`        VARCHAR(20) DEFAULT 'PENDING',  -- 생명주기
  `priority`      INT DEFAULT 100,       -- 낮을수록 먼저
  `retry_count`   INT DEFAULT 0,         -- 재시도 누적
  `last_error_message` TEXT,          -- 실패 진단
  `run_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 지연/백오프 예약
  `created_at`    DATETIME, `updated_at` DATETIME,
  PRIMARY KEY (`id`),
  INDEX `idx_status_priority_runat` (`status`,`priority`,`run_at`)
);
```
* `status` : 잡의 생명주기를 한 컬럼에 압축한다. 워커 획득·재시도·완료 판단의 축이다.
* `priority + run_at` : 무엇을 먼저(우선순위), 언제부터(예약) 가져갈지 결정한다. 지연 작업과 백오프 재시도를 같은 메커니즘으로 처리한다.
* `retry_count`, `last_error_message` : 실패를 데이터로 남겨 백오프 계산과 사후 진단을 가능하게 한다.
* `(status, priority, run_at)` Multi Column Index : 워커 획득 쿼리의 `WHERE`·`ORDER BY`를 그대로 커버 → 큐가 커져도 “다음 잡 찾기”가 인덱스로 빠르게 끝난다. 이 인덱스가 이 패턴의 심장이다.

**등록 — 하나의 트랜잭션으로**
* 핵심 데이터와 잡을 같은 트랜잭션에 넣는 것이 이 패턴의 정합성 보증이다. 커밋되면 둘 다, 롤백되면 둘 다 없다.
```sql
BEGIN;
  -- 1. 핵심 비즈니스 로직
  INSERT INTO users (name, email) VALUES ('John Doe', 'john@example.com');
  SET @user_id = LAST_INSERT_ID();

  -- 2. 비동기 작업 등록 (같은 트랜잭션)
  INSERT INTO jobs (job_type, payload, priority)
    VALUES ('SEND_WELCOME_EMAIL',  JSON_OBJECT('user_id', @user_id), 100);
  INSERT INTO jobs (job_type, payload, priority)
    VALUES ('ISSUE_SIGNUP_COUPON', JSON_OBJECT('user_id', @user_id), 200);
COMMIT;
```

**획득 — 워커 경쟁을 SKIP LOCKED로**
* 여러 워커가 같은 테이블을 동시에 노린다. 핵심은 한 잡을 정확히 한 워커만 집어가게 하는 것이다. MySQL 8.0의 `FOR UPDATE SKIP LOCKED`가 이를 락 대기 없이 해결한다.
```sql
BEGIN;
  -- 처리할 잡을 찾아 배타적 락. 이미 잠긴 행은 건너뛴다.
  SET @job_id = (
    SELECT id FROM jobs
    WHERE status = 'PENDING' AND run_at <= NOW()
    ORDER BY priority ASC, id ASC
    LIMIT 1
    FOR UPDATE SKIP LOCKED          -- ★ 다른 워커가 잡은 행은 스킵
  );

  IF @job_id IS NOT NULL THEN
    UPDATE jobs SET status = 'RUNNING' WHERE id = @job_id;
  END IF;
COMMIT;   -- 락 해제. 이후 앱 코드가 실제 작업(이메일 발송 등)을 수행
```
* `FOR UPDATE`만 쓰면 : 뒤늦은 워커들이 잠긴 행을 기다리며 줄을 선다 → 사실상 직렬 처리, 병렬성 붕괴된다. 일반적인 `SELECT`는 락을 보유하지 않지만 `SELECT ... FOR UPDATE`는 락을 보유한다고 했다.
* `SKIP LOCKED` : 잠긴 행은 즉시 건너뛰고 다음 잡을 집는다 → 워커를 늘린 만큼 처리량이 선형에 가깝게 확장할 수 있다.
* `run_at <= NOW()` : 미래로 예약된 잡(지연·백오프)은 자동으로 제외된다. 예약과 큐잉이 한 쿼리에 집약된다.

> 핵심 : 중복 방지의 핵심은 원자성. “잡 선택 → RUNNING 표시”가 한 트랜잭션 안에서 락으로 보호되므로, 두 워커가 같은 잡을 RUNNING으로 만드는 경쟁이 불가능하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3e7ce9a9-108b-45bd-892d-6b15488852ba.png)

**실패와 재시도 - 지수 백오프**
* 일시적 실패(네트워크 순단 등)는 즉시 재시도하면 더 악화된다. 재시도 간격을 점점 벌리는 지수 백오프로 상대 시스템에 숨 쉴 틈을 준다.
```sql
-- 실패 처리 (의사 코드)
SET @new_retry_count = retry_count + 1;
SET @delay = POWER(10, @new_retry_count);   -- 10, 100, 1000 … 초

UPDATE jobs
SET status = 'PENDING',               -- 다시 큐로 (아직 죽이지 않음)
    retry_count = @new_retry_count,
    last_error_message = '...',
    run_at = NOW() + INTERVAL @delay SECOND -- 다음 실행을 미래로
WHERE id = @job_id;
```
* `status`를 `PENDING`으로 되돌린다 : 실패했다고 바로 버리지 않고 큐에 재투입한다. 단 `run_at`을 미래로 밀어 바로 다시 잡히지 않게 한다.
* `run_at`이 백오프의 열쇠 : 워커 획득 쿼리의 `run_at <= NOW()` 덕분에, 지연이 끝나는 시점까지 이 잡은 자연히 무시된다. 별도 스케줄러가 필요 없다.
* 한도를 넘으면 `FAILED` : `retry_count`가 상한을 넘으면 `FAILED`로 확정하고 알림·수동 개입 대상(Dead Letter)으로 분리한다.

#### 설계 고려사항

* 주의 1 : 멱등성은 필수. 이 큐는 `at-least-once`다. 워커가 작업 후 `DONE` 표시 직전에 죽으면 같은 잡이 재실행된다. 이메일 중복 발송을 막으려면 소비 로직 자체가 멱등해야 한다.
* 주의 2 : 멈춘 `RUNNING` 회수. 워커가 `RUNNING`인 채로 크래시되면 그 잡은 영원히 갇힌다. `updated_at`이 오래된 `RUNNING`을 주기적으로 `PENDING`으로 되돌리는 리퍼(reaper)가 필요하다.
* 주의 3 : 폴링 간격의 트레이드오프. 자주 폴링하면 지연이 발생하고 DB 부하가 증가한다. 지연에 민감하디면 `SELECT ... SLEEP` 대신 알림(NOTIFY) 방식을 검토할 수 있다.
* 주의 4 : 완료 잡 정리. `DONE`이 쌓이면 테이블과 인덱스가 비대해진다. 아카이브 이관이나 `created_at` 기준 파티셔닝·주기 삭제로 관리하는 것을 권장한다.
* 주의 5 : 우선순위 기아 주의. 높은 우선순위 잡이 계속 유입되면 낮은 잡이 영원히 밀린다. 대기 시간 가중치(aging) 등의 보정을 고려한다.
* 주의 6 : 처리량 상한을 알고 쓸 것. DB 큐는 초당 수백 ~ 수천 건 규모에 적합하다. 그 이상·다중 서비스 팬아웃이 필요해지면 Kafka·SQS로 이행하는 것이 자연스러운 다음 단계다.

#### MySQL + NoSQL을 결합한 시스템 아키텍처 설계

**1. 문제 — 하나의 DB로 모든 질의를 감당할 수 없다**
* 관계형 DB는 트랜잭션과 정합성에 최적이다. 하지만 같은 데이터에 대한 요구는 제각각이다.

**2. 해결 - 단일 진실 + 목적별 읽기 모델**
* 두 개의 검증된 패턴을 결합한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6042938d-f1d7-43b6-a9c2-4dee1d427c9b.png)

* 각 NoSQL은 MySQL이 못하는 특정 질의를 대신한다. 선택 기준은 데이터 모델과 질의 형태가 된다.
