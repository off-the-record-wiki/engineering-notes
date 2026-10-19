# Apache Kafka - KSQLDB Stream

#### Stream 개요

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/76b832fa-6a31-4cf3-90b7-3743e35b8ee7.png)

* `Stream`은 메시지 Streaming 처리를 위해서 추상화한 Layer를 말한다.
* 변경이 불가능하며 append만 가능한 Collection이다.
* 연속된 Historical 정보를 표현한다.
* Insert된 Row는 변경 불가능하며 새로 Insert된 Row는 Stream의 마지막에 추가된다.
* `Stream`은 Key를 가질수도 그렇지 않을수도 있으며 동일한 Key값을 가진 Row는 동일한 파티션에 저장된다.

#### 연속적인 Stream/Table 처리

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a873fd92-3953-47cf-846e-9419d29ac0d1.png)

* `Stream`와 `Table`은 Topic을 Source로 등록할 수 있지만, 다른 `Stream` 또는 `Table`을 입력받아 연속적인 `Stream/Table`의 Processing이 가능하다.

#### Stream과 Table의 차이

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b07d6bc1-bec5-42cb-ae54-b7c2ea5425a3.png)

* `Stream`은 Key값의 중복 여부와 상관없이 연속적으로 메시지(이벤트)를 처리하지만, `Table`은 PK로 중복을 허용하지 않으며 메시지가 입력될 경우 해당 값을 업데이트한다.

#### KSQLDB Serialization Format

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/4a593627-de67-4512-adba-b1923ec4f60e.png)

```sql
create stream simple_user_stream
(
    id int KEY,
    name varchar,
    email varchar
)
with
(
    KAFKA_TOPIC = 'simple_user_stream',
    KEY_FORMAT = ‘KAFKA’,
    VALUE_FORMAT ='JSON‘,
    PARTITIONS = 1
); 
```

* KSQLDB는 여러가지 직렬화 포맷(Serialization Format)으로 Topic 메시지를 읽고 쓸 수 있다.
* Kafka Core는 기본적으로 Primitive Type만 지원한다.
* KSQLDB는 컬럼 정보를 인식하기 위해 Json, Avro 등의 직렬화 포맷 방식을 지원한다.
* 직렬화 포맷은 KEY, VALUE 각각에 별도로 지정할 수 있으며 `KEY_FORMAT`은 Key에 대한 포맷, `VALUE_FORMAT`은 Value에 대한 포맷 형식을 지정할 수 있다.
* Key와 Value의 포맷이 동일하면 `FORMAT`으로 포맷 형식 지정이 가능하다.

#### KSQLDB Serialization Format 유형

* `KAFKA` : Number와 String 등의 Primitive Type 직렬화에 사용되며 주로 Key값의 직렬화 포맷으로 적용한다.
* `JSON` : Json 포맷 직렬화. Schema Registry 적용없이 사용 가능하다.
* `AVRO` : Avro 포맷 직렬화. Schema Registry를 필요로 한다.
* `DELIMITED` : CSV 형태의 직렬화
* `PROTOBUF` : Protocol Buffer 형태의 직렬화

#### Key를 가지지 않는 Stream과 Key를 가지는 Stream

```sql
# 기존 토픽이 있는 상태에서 Stream을 생성한다.
create stream simple_user_stream
(
    id int,
    name varchar,
    email varchar
)
with
(
    KAFKA_TOPIC = 'simple_user_stream',
    KEY_FORMAT = ‘KAFKA’,
    VALUE_FORMAT ='JSON'
);

# 신규 토픽을 먼저 생성하고 Stream을 생성한다.
create stream simple_user_stream
(
    id int,
    name varchar,
    email varchar
)
with
(
    KAFKA_TOPIC = 'simple_user_stream',
    KEY_FORMAT = ‘KAFKA’,
    VALUE_FORMAT ='JSON‘,
    PARTITIONS = 1
);
```

```sql
# Key를 가지는 Stream을 생성한다.
create stream simple_user_stream
(
    id int KEY,
    name varchar,
    email varchar
)
with
(
    KAFKA_TOPIC = 'simple_user_stream',
    KEY_FORMAT = ‘KAFKA’,
    VALUE_FORMAT ='JSON‘,
    PARTITIONS = 1
);
```

#### Pull 쿼리와 Push 쿼리

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/7e49e0cf-2827-454d-870d-9f2cdabb9986.png)

* `Pull`쿼리는 쿼리를 실행한 시점에 쿼리 조건에 맞는 데이터를 KSQLDB에서 가져온다. → 조회 시점의 데이터를 추출한다.
* 한 번 수행 후 쿼리가 종료된다. 

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/748d62fb-6617-43ad-aecf-5f10ecd5c49d.png)

* `Push`쿼리는 쿼리 실행 이후 지속적으로 `Stream/Table`을 모니터링하면서 쿼리 조건에 맞는 데이터가 추가(변경)되었을 때마다 KSQLDB에서 추가(변경)된 데이터를 계속 가져온다.
* 이 때, 종료 시그널을 주지 않는 이상 쿼리가 종료되지 않는다. → 지속적으로 변경 데이터를 추출한다.
* `Stream`에 `Group By` 적용 시 `Pull`쿼리 수행이 불가하며 `Topic`을 `Source`로 하는 `Table`에 `Pull`쿼리 수행이 불가능하다.

#### Pull 쿼리와 Push 쿼리에서 Consumer 동작

* `Pull`쿼리는 수행 시마다 새로운 Consumer Group과 Consumer가 생성되고 종료된다.
* `Push`쿼리는 쿼리가 유지되는 동안 계속 Consumer Group과 Consumer가 유지된다.(강제로 종료하지 않는 이상 유지된다.)

#### KSQLDB Query 오브젝트

* KSQLDB의 Query는 Query의 수행 상태(Running)을 가진다.
* 동일한 Query라고 하더라도 CLI에서 `Select/Insert`로 수행되는 Query는 서로 다른 ID를 가진다.
* `Mview`에서 사용되는 CSAS, CTAS Query는 동일한 Query일 경우 동일한 ID를 가진다.

#### Data 타입과 Alter DDL 구문 정리

> [ksqlDB를 활용한 증권사의 실시간 데이터 처리하기](https://toss.tech/article/ksqldb-realtime-data)

* KSQLDB는 컬럼의 변경, 삭제를 허용하지 않고 오직 컬럼의 추가(ADD)만 허용한다.
* KSQLDB의 컬럼은 무조건 Nullable이며 Not Null 제약조건을 부여할 수 없다.

```sql
create table simple_user_table
(
    id int PRIMARY KEY,
    name varchar,
    email varchar
)
with
(
    KAFKA_TOPIC = 'simple_user_table',
    KEY_FORMAT = ‘KAFKA’,
    VALUE_FORMAT ='JSON‘,
    PARTITIONS = 1
); 
```
