# Apache Kafka - Group by & Mview

#### Group by

> [KSQLDB Aggregate Functions](https://docs.confluent.io/platform/current/ksqldb/developer-guide/ksqldb-reference/aggregate-functions.html)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/32270a13-c89f-4a50-bed0-08a4c43fd48c.png)

* `Group by`절에 기술된 컬럼 값으로 그룹화한 뒤 집계(Aggregation) 함수와 함께 사용되어 그룹화된 집계 정보를 제공한다.
* `Group by`절에 기술된 컬럼 값으로 반드시 1의 집합을 갖게 된다.
* `Select`절에는 `Group by`절에 기술된 컬럼과 집계 함수만 사용될 수 있다.

#### latest_by_offset과 earliest_by_offset 이해

* `earliest_by_offset` : 가장 먼저 들어온 데이터를 선택한다.
* `latest_by_offset` : 가장 나중에 들어온 데이터를 선택한다.

#### Mview(Materialized View)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d1ae39fc-a00d-4f6d-a750-2aae8f0e0022.png)

* `Mview`는 대량의 데이터에 대한 분석 SQL을 보다 빠르게 추출하기 위해서 사용된다.
* `Mview` 쿼리에 사용되는 테이블들의 변경사항을 즉각 또는 느리게 변경 로그를 적용하여 `Mview`에 반영한다.
* 사용자는 대용량 분석 SQL을 원본 테이블이 아닌 `Mview`를 통해 조회하여 보다 신속하게 결과 추출이 가능하다.
* `View`와 다르게 `Mview`는 데이터가 저장되는 실제적인 Storage를 가진다.
* `Mview` 쿼리에 사용되는 테이블들의 변경사항을 실시간으로 즉각 반영하면서 대용량 데이터의 `Mview`의 경우 DB 성능 전체에 큰 영향을 미치는 현상이 발생할 수 있다.

#### Mview CTAS(CREATE TABLE AS SELECT)

```sql
create table customer_activity_mv01
with (
KAFKA_TOPIC = 'customer_activity_mv01_topic',
KEY_FORMAT = 'KAFKA', 
VALUE_FORMAT = 'JSON',
PARTITIONS = 3
)
as
select customer_id, avg(activity_point) as avg_point
from customer_activity_stream group by customer_id;

show tables;

describe customer_activity_mv01 extended;

show queries;
```

| 옵션 | 의미 |
|---|---|
| `KAFKA_TOPIC` | 집계 결과가 기록될 출력 토픽. 없으면 ksqlDB가 자동 생성 |
| `KEY_FORMAT = 'KAFKA'` | 키를 Kafka 기본 직렬화(문자열은 UTF-8, int는 4바이트 BE)로 저장. 일반 컨슈머도 그대로 읽을 수 있음 |
| `VALUE_FORMAT = 'JSON'` | 값은 JSON. 스키마 관리가 필요하면 `AVRO`/`PROTOBUF` + Schema Registry |
| `PARTITIONS = 3` | 출력 토픽 파티션 수 |

* `CREATE TABLE ... AS SELECT`는 단발 명령이 아니라 persistent query를 띄운다. ksql DB 서버에서 Kafka Streams 애플리케이션이 계속 돌면서 다음과 같은 과정을 거치게 된다.

1. Repartition : 입력 스트림의 키가 `customer_id`가 아니면 내부 repartition 토픽으로 다시 파티셔닝한다. 같은 고객의 이벤트가 같은 태스크로 모여야 집계가 성립하기 때문이다.
2. State store : 각 키의 누적 상태를 로컬 RocksDB에 유지한다.
3. Changelog 토픽 : State store 내용을 내부 Changelog 토픽(compact 정책)에 백업한다. 서버가 죽어도 여기서 상태를 복구한다.
4. Sink 토픽 write : 갱신된 값을 `customer_activity_mv01_topic`에 쓴다.

#### Mview CTAS(CREATE TABLE AS SELECT) 데이터 처리 동작 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/c67b0214-629a-4654-a9d6-5d5a06fb676a.png)

```
customer_activity_stream 토픽
        │  ① consume
        ▼
   [repartition]  ── customer_id로 다시 파티셔닝 (내부 토픽)
        │  ②
        ▼
   [aggregate]  ── state store 읽기 → 갱신 → 쓰기
        │  ③
        ├──────────────► changelog 내부 토픽  (상태 백업, compact)
        │  ④
        ▼  ⑤
   MV01_TOPIC  (집계 결과 발행)
```
* `consume` : 원본 스트림을 읽는다.
* `repartition` : `GROUP BY customer_id`인데 입력 토픽의 키가 `customer_id`가 아니면, 같은 고객의 이벤트가 서로 다른 파티션에 흩어지게 된다. 그러면 태스크마다 부분 집계만 갖게 되어 평균이 틀리게 된다. 그래서 ksqlDB가 `customer_id`를 키로 하는 내부 repartition 토픽에 다시 쓴다. 이 단계를 거치고 나면 같은 고객의 모든 이벤트가 반드시 같은 파티션은 같은 태스크로 간다. 순서 보장과 집계 정확성이 여기서 확보된다.
* `aggregate`
* `changelog` : 상태가 바뀔 때마다 내부 changelog 토픽에 기록한다. 이건 사용자가 보는 토픽이 아니라 순수 복구용이다.
* `sink` : 갱신된 결과를 `MV01_TOPIC`에 발행한다.

#### Mview 생성 시 auto.offset.reset 값에 따른 유의사항

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fdaeb16b-a5e4-4e65-bb46-c1da69ca6b72.png)

* CTAS로 persistent query를 띄우면 내부적으로 컨슈머 그룹이 하나 생기는데 이 그룹은 커밋된 오프셋이 없는 상태로 시작한다. 그 때, 어디서부터 읽을지를 이 값이 결정한다.
* `earliest` : 토픽에 남아 있는 가장 오래된 오프셋. 보관된 전체 이력의 집계가 담긴다.
* `latest` : 쿼리 생성 시점의 마지막 오프셋. 생성 이후 발생한 이벤트만의 집계가 담긴다.

#### Group by 절에 여러 개의 컬럼이 있는 Mview 생성

```sql
create table customer_activity_mv02
with (
KAFKA_TOPIC='customer_activity_mv02_topic', 
KEY_FORMAT = 'KAFKA',
VALUE_FORMAT = 'JSON',
PARTITIONS = 1
)
as
select customer_id, activity_type, count(*) as cnt 
from customer_activity_stream group by customer_id, activity_type;

-- 아래는 KEY_FORMAT을 명시적으로 JSON으로 지정. 
create table customer_activity_mv02
with (
KAFKA_TOPIC='customer_activity_mv02_topic', 
KEY_FORMAT = 'JSON',
VALUE_FORMAT = 'JSON',
PARTITIONS = 1
)
as
select customer_id, activity_type, count(*) as cnt from customer_activity_stream group by customer_id, activity_type;

select * from customer_activity_mv02;

print customer_activity_mv02_topic from beginning;

drop table customer_activity_mv02 delete topic;
````
* 집계 단위가 `(customer_id, region)` 조합으로 바뀌고, 이 두 컬럼이 함께 테이블의 키가 된다.
* `GROUP BY`에 넣은 컬럼은 반드시 `SELECT`에도 있어야 하고, 반대로 `SELECT`의 비집계 컬럼은 전부 `GROUP BY`에 있어야 한다.
* 다중 컬럼 키에서는 `KEY_FORMAT = 'KAFKA'`를 썼는데, 다중 컬럼 키에서는 이게 안 된다.
* `GROUP BY` 컬럼에 NULL이 하나라도 있으면 레코드가 버려진다. 에러 없이 사라져 컬럼이 늘수록 위험이 증가한다.
* 시간 성격 컬럼은 `GROUP BY`에 넣지 않는다.
* 한 번 만들면 나중에 바꿀 수 없다. 즉, `ALTER`가 불가하고 `DROP`후 재생성만 가능하다.

#### as_value()를 이용해 Mview 생성 시 Group by 컬럼을 토픽의 value로 만들기

* Mview 생성 시 `Group by` 컬럼들은 Kafka Topic의 key값으로 생성되며 value로는 생성되지 않는다.
* 하지만 Connect등으로 연동으로 타 시스템에 데이터로 전달 되어야 할 경우에는 주로 value가 사용되는데 이 때, `as_value()`를 적용해서 만들어야 한다.

```sql

create table customer_activity_mv02_asvalue
with (
KAFKA_TOPIC='customer_activity_mv02_asvalue_topic', 
KEY_FORMAT = 'JSON',
VALUE_FORMAT = 'JSON',
PARTITIONS = 1
)
as
select customer_id, activity_type, 
as_value(customer_id) as customer_id_value, as_value(activity_type) as activity_type_value,
count(*) as cnt from customer_activity_stream group by customer_id, activity_type;

select * from customer_activity_mv02_asvalue emit changes;

print customer_activity_mv02_asvalue_topic from beginning;
```

#### Mview CTAS에서 RocksDB 동작 메커니즘

* 다음과 같은 경우 RocksDB가 ksql DB에서 사용된다.
  * Table에 Select 수행
  * Aggregation/Group by 수행
  * Stream과 Table 조인
  * Stream에 기타 Stateful한 처리가 필요한 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d982f08c-e2d9-4119-882e-cb56ee875d9d.png)

```
① consume ─► ② deserialize ─► ③ repartition ─► ④ aggregate
                                                    │
                                          ⑤ record cache (키별 dedup)
                                                    │
                                    ┌───────────────┼───────────────┐
                                    ▼               ▼               ▼
                              ⑥ RocksDB      ⑦ changelog      ⑧ sink topic
                               (memtable)      토픽 write        write
                                                    │
                                              ⑨ offset commit
```
* 읽고 → 더하고 → 쓰기. `get()`이 로컬 디스크(RocksDB)에서 일어나기 때문에 네트워크 왕복이 없고, 그래서 이벤트당 처리가 빠르다. 원격 DB를 상태 저장소로 쓰면 이 지점이 병목이 된다.
* 로컬 RocksDB는 지워도 된다. 디스크가 통째로 날아가더라도 changelog에서 복원되므로 데이터 손실이 아니다. 반대로 changelog 토픽을 지우면 진짜로 잃게 된다.

#### Mview CSAS

```sql
create stream customer_activity_strm_mv01 
with (
      KAFKA_TOPIC = 'customer_activity_strm_mv01_topic', 
      KEY_FORMAT = 'KAFKA', 
      VALUE_FORMAT = 'JSON', 
      PARTITIONS = 3
 )
as
select * from customer_activity_stream where activity_type in ('web_open', 'mobile_open') emit changes;
```
```
CTAS  │ consume ─► repartition ─► aggregate ─► [RocksDB] ─► [changelog] ─► sink
CSAS  │ consume ─► filter ─► sink
```
| | CTAS (Table) | CSAS (Stream) |
|---|---|---|
| 결과 | 키별 최신 값 (upsert) | 조건 통과 이벤트 (append) |
| state store | 있음 | **없음** |
| changelog 토픽 | 있음 | **없음** |
| pull query | 가능 | **불가** |
| 재시작 복구 | changelog 재생 필요 | 오프셋만 있으면 즉시 |
| 스케일 아웃 | 상태 이관 비용 발생 | 거의 무비용 |

#### Mview CSAS 데이터 처리 동작

```
consume → deserialize → WHERE 평가 → SELECT * 복사 → serialize → produce
                            └─ false면 버림 (아무것도 안 나감)
```
* 디스크 접근도 상태 조회도 없는 무상태 1:1 파이프라인이다.
* 병합이 없다 : CTAS는 record cache가 같은 키를 합쳐서 입력보다 출력이 줄지만, CSAS는 state store가 없어 캐시도 없다. 조건 통과 건수 = 출력 건수, 정확히 일치. `commit.interval.ms`, `cache.max.bytes.buffering`을 만져도 출력이 달라지지 않는다.
* 키·순서·타임스탬프가 원본 그대로 : PARTITION BY가 없으니 키가 통과 → 같은 키는 같은 파티션 → 키 단위 순서 보존. 타임스탬프도 발행 시각으로 갱신되지 않고 원본 이벤트 시각이 유지되므로, 이 스트림을 받아 윈도우 집계를 해도 시간이 밀리지 않는다.
* 오프셋은 대응되지 않는다 : 버려진 레코드가 있어 입력 offset ≠ 출력 offset. 오프셋으로 추적 불가하므로, 필요하면 값에 이벤트 ID를 넣어야 한다.
* 병렬도 상한 = 소스 파티션 수 : 상태가 없어 스케일 아웃은 저렴하지만(이관할 RocksDB 없음), 소스가 3파티션이면 서버를 늘려도 태스크는 3개다. 넘어서려면 소스 파티션을 늘려야 한다.
* 역직렬화 실패는 조용히 건너뛴다 : 기본값 `ksql.fail.on.deserialization.error=false`로 쿼리는 안 멈추지만 그 레코드는 사라진다. 유실이 허용 안 되면 `true`로 바꿔 쿼리를 멈춰 세우는 편이 낫다.

#### Group by의 Repartition

* Key 또는 PK가 아닌 컬럼으로 Group by를 수행할 경우 기존 Stream/Table Partition을 해당 Group by 컬럼으로 Repartition 수행해야 한다.

**1. Key 컬럼으로 Group by 수행**
* key 컬럼으로 Group by를 수행할 경우 별도의 Repartition이 필요 없이 파티션 기반의 분산 처리가 가능하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a482fa28-38b9-4eed-81e6-d6a43c83f6ed.png)

**2. Key가 아닌 다른 컬럼으로 Group by 수행**
* key가 아닌 컬럼으로 Group by를 수행할 경우 개별 Stream Task가 모든 파티션을 조회해서 해당 컬럼값으로 데이터를 1차 분류한 뒤에 Group by를 수행해야 한다.
* 보다 효율적인 병렬 분산 처리를 위해서 Group by 컬럼값으로 파티셔닝을 다시 수행하는 Repartition 작업이 필요하다.
* 개별 Stream Task가 모든 파티션을 전부 다 조회해야 한다. 효율적인 분산 병렬 처리가 수행되지 못한다.
* Partition이 네트워크로 연결된 노드별로 분산되어 있을 경우 네트워크 전송 시간 및 부하 소모
* RocksDB는 분산 DB가 아니라 Local DB이므로 분산된 데이터를 기법으로 Stateful 연산이 불가하다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f6784915-2b59-41a5-9029-be0833591996.png)
