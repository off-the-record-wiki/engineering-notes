# Apache Kafka - Join

#### KSQLDB Join

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3b0e69b2-3ba7-4c21-9862-adb9d9099fa0.png)

* `customer_id`를 키로 양쪽을 같은 파티션에 정렬(co-partition)해, 짝이 될 레코드가 항상 같은 태스크에 모이게 만든다.
* 각 태스크는 한쪽을 로컬 state store에 붙들어두고, 반대쪽 레코드가 도착하면 로컬 조회 → 컬럼 병합 → 새 토픽에 발행을 이벤트 단위로 반복한다.
* 원격 조회를 로컬 조회로 바꾸는 것. 마주칠 데이터를 미리 같은 자리에 모아두고 기억해두는 구조를 위해 Join을 사용한다.

```sql
drop table simple_user_table;

drop stream simple_user_stream delete topic;

create stream simple_user_stream 
(
	user_id integer key,
	name varchar,
	email varchar
) with (
  KAFKA_TOPIC = 'simple_user_topic',
  KEY_FORMAT = 'KAFKA', 
  VALUE_FORMAT ='JSON',
  PARTITIONS = 3
);

insert into simple_user_stream(user_id, name, email) values (1, 'John', 'test_email_01@test.domain');
insert into simple_user_stream(user_id, name, email) values (2, 'Merry', 'test_email_02@test.domain');
insert into simple_user_stream(user_id, name, email) values (3, 'Elli', 'test_email_03@test.domain');
insert into simple_user_stream(user_id, name, email) values (4, 'Mike', 'test_email_04@test.domain');
insert into simple_user_stream(user_id, name, email) values (5, 'Tom', 'test_email_05@test.domain');
insert into simple_user_stream(user_id, name, email) values (5, 'Tommy', 'test_email_05@test.domain');
insert into simple_user_stream(user_id, name, email) values (6, 'Michell', 'test_email_06@test.domain');
select * from simple_user_stream;
```

#### KSQLDB Join 제약 조건

* 조인에 참여하는 Stream/Table은 동일한 Co-Partitioning이 적용되어야 한다.
  * 조인 Key Partition 개수가 모두 같아야 한다.
  * 조인 Key의 타입이 모두 같아야 한다.
  * 조인 Key의 파티션별 값 분포는 서로 동일해야 한다.(동일한 Partitioning 전략이 적용되어야 한다)
* Stream-Stream 조인, Stream-Table 조인, Table-Table 조인별로 특정 제약 조건이 존재한다.

#### [Stream-Stream 조인](https://docs.confluent.io/platform/current/ksqldb/developer-guide/joins/join-streams-and-tables.html)

* KSQL의 조인은 여러 제약 조건이 있다.
  * 조인키에 대한 제약. 조인 키는 같은 데이터 타입이어야 하며 다른 데이터 타입일 경우 CAST()함수를 이용하여 데이터 타입을 변환한다.
  * 조인키는 Stream의 경우 Key로, Table의 경우 Primary Key로 지정되어야 한다.
  * 조인키의 파티션 갯수는 서로 같아야 한다.
  * 조인키의 파티션별 값 분포는 서로 동일하게 분포되어야 한다.
  * Stream과 Stream 조인은 `within` 절로 window 시간을 정해줘야 한다. 전체 stream에 대한 조인이 되지 않는다.
  * Stream과 Stream, Stream과 table 조인은 pull 쿼리가 지원되지 않으며 Push 쿼리가 지원되지 않는다. 즉, 조인을 하게 되면 이는 rocksdb의 도움이 필요하다. 단, Stream과 Stream 조인을 CSAS로 Mview 생성 후에는 `select * from mview`로 pull 쿼리도 가능하다.

```sql
-- 아래는 pull 쿼리로 수행되지 않음. 
select a.*, b.*
from simple_user_stream a 
inner join user_activity_stream b on a.user= b.user_id;

-- 아래는 push 쿼리지만 within 절이 없어서 오류
select a.*, b.*
from simple_user_stream a 
inner join user_activity_stream b on a.user= b.user_id emit changes;

select a.user_id, a.name, b.*
from simple_user_stream a 
inner join user_activity_stream b within 2 hours on a.user_id= b.user_id emit changes;
```

#### [Stream-Table 조인](https://docs.confluent.io/platform/current/ksqldb/developer-guide/joins/join-streams-and-tables.html#stream-table-joins)

* 스트림이 왼쪽, 테이블이 오른쪽. 뒤집으면 조인이 안 된다.
* 스트림 레코드가 오면 결과 발행, 테이블 레코드가 오면 내부 상태만 갱신되고 출력은 없다.
* 해당 키 삭제. 이것도 조인을 일으키지 않고, 다음 스트림 이벤트가 왔을 때 짝이 없는 형태로 드러난다.
* 테이블이 나중에 바뀌어도 이미 나간 결과는 그대로 나간다.
* 테이블은 사건이 아니라 상태라 비교할 시각이 없다. 즉, `WITHIN` 사용이 불가능하다.

```sql
select b.user_id, b.name, a.* 
from user_activity_stream a
inner join simple_user_table b on a.user_id= b.user_id emit changes;

-- 아래는 table을 기준으로 stream을 조인하므로 수행되지 않음. 
select a.user_id, a.name, b.*
from simple_user_table a
inner join user_activity_stream b on a.user_id= b.user_id emit changes;

-- 아래는 stream-table 조인 시 within 절을 적용하면 수행되지 않음.  
select b.user_id, b.name, a.* 
from user_activity_stream a
inner join simple_user_table b within 2 hours on a.user_id= b.user_id emit changes;
```

#### Stream-Table 조인 시 Event 생성 시점에 따른 조인 처리

* KSQLDB는 시간의 흐름에 따른 Event Stream 데이터 처리에 기반한다.
* 조인 데이터는 조인 대상인 Stream 또는 Table의 서로 다른 데이터 생성 시점을 감안해 생성된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/56c868ed-d137-4a47-8043-c0c669e2722d.png)

#### 조인 시 Co-Partitioning 제약

* 조인되는 두 Stream/Table은 동일한 파티션 개수, 동일한 조인 키 타입, 동일한 파티션 분배 방식을 가져야 정상적인 조인이 가능하다.
* 조인 컬럼이 키가 아닌 경우 스트림은 자동 repartition으로 보정되지만 테이블은 직접 재생성해야 한다.
* 파티션 개수 불일치는 에러로 드러나는 반면, 파티셔너 불일치는 에러 없이 조인이 누락되므로 특히 주의해야 한다.

#### 조인 시 조인 key 컬럼에 cast 함수 적용 시 유의사항

* 한쪽 키는 INT, 다른 쪽은 VARCHAR인 상황에서 다음과 같이 쓰고 싶어진다.

```sql
FROM order_stream o
JOIN customer_table c
  ON CAST(o.customer_id AS VARCHAR) = c.customer_id
```
* 타입은 맞춰졌으니 될 것 같지만, 파티션 배치는 그대로이다.
* 파티션 번호는 레코드가 토픽에 적재될 때 `hash(원본 키 바이트) % 파티션 수`로 이미 확정되어 디스크에 놓여 있다. `CAST`는 그걸 읽어온 뒤 메모리에서 변환하는 연산이라, 이미 정해진 물리적 위치에는 아무 영향이 없다.
* 같은 "1"인데 애초에 다른 파티션에 저장되어 있다. CAST로 논리적 값을 맞춰도 마주칠 자리에 있지 않다는 문제는 그대로 남아있다.
  * 스트림 쪽에 CAST를 걸면 동작하지만 비용이 붙는다 : 조인 키가 "키 컬럼"이 아니라 "표현식"이 되므로, ksqlDB가 이를 감지해 자동으로 repartition 토픽을 끼워 넣는다. 변환된 값을 키로 다시 써서 배치를 새로 만드는 것이고, 그래서 조인이 정상 동작한다. 카프카에 한 번 더 쓰고 다시 읽기에 지연이 늘고 토픽이 하나 더 생기며, 그게 쿼리가 사는 동안 영구적으로 유지된다.
  * 테이블 쪽에 CAST를 걸면 대부분 거부된다 : ksqlDB는 조인의 테이블 변에 대해 조인 표현식이 그 테이블의 키 컬럼 그 자체일 것을 요구한다. 함수나 CAST를 씌우면 키가 아닌 것이 되고, 테이블은 자동 repatition 대상이 아니라서 해결할 방법이 없어 에러로 막는다.

#### [Table-Table 조인](https://docs.confluent.io/platform/current/ksqldb/developer-guide/joins/join-streams-and-tables.html#stream-table-joins)

* Table-Table 조인은 빈번하게 사용되지 않는다. 1 : 1 조인만 사용을 권장하며, M : 1 조인 사용 시에는 매우 주의가 필요하다.
* M : 1 조인 시 KEY_FORMAT이 M쪽 KEY가 JSON 포맷으로 되면서 조인 대상 테이블의 조인 키별 파티션 분배가 서로 달라지고 조인 결과가 제대로 생성되지 않는 문제가 발생한다.
* Table-Table 조인 결과는 어느 한쪽 테이블의 데이터가 추가되더라도 이를 반영하여 조인 데이터가 생성된다.
* Group by CTAS로 생성된 테이블과 Master성 테이블 조인을 하는 경우 Stream과 Table 조인 후, Group by로 변경하는 것이 더 효율적이다.

#### 파티션 key가 아닌 컬럼을 조인 key로 사용하여 조인

* 리파티셔닝이 발생해서 코파티셔닝 조건을 충족시킨다.
  * 스트림 쪽 : 자동으로 리파티셔닝이 끼어들고 조인이 정상 동작한다.
  * 테이블 쪽 : 리파티셔닝이 일어나지 않고 에러가 발생한다.

| 항목 | 영향 |
|---|---|
| 지연 | 쓰기 + 읽기 한 번씩 추가 |
| 처리량 | 네트워크·디스크 I/O 증가 |
| 토픽 | 내부 토픽 하나 추가 생성 및 유지 |
| 순서 | `customer_id` 기준으로 재정렬 — 기존 `order_id` 단위 순서는 의미 없어짐 |
