# Apache Kafka - Time & Windows

#### KSQLDB에서 Time 역할과 Rowtime

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/54cdeb18-049e-41ae-8552-107ed46911ce.png)

* ROWTIME = 카프카 레코드의 타임스탬프. ksqlDB 모든 행에 자동으로 붙는 시스템 컬럼. 값 컬럼이 아니라 메타데이터이다.
* 모든 시간 연산의 기준은 ROWTIME : 윈도우·WITHIN·조인 판정이 전부 이 값으로 이뤄진다. 서버 시계가 아니다.
* 기본값은 프로듀서가 찍은 시각 : 토픽의 message.timestamp.type에 따라 브로커 도착 시각으로 바뀔 수도 있다.
* 원하면 값 컬럼으로 지정 가능 : `WITH (TIMESTAMP='order_time', TIMESTAMP_FORMAT=...)`
* 파생 스트림에 그대로 상속 : CSAS를 몇 단계 거쳐도 원본 시각이 유지되어 윈도우가 밀리지 않는다.

> **KSQLDB ROWTIME**
* KSQLDB의 Stream과 Table은 메타 속성으로 ROWTIME 컬럼을 가지고 있다.
* ROWTIME값은 Stream/Table의 메타 Timestamp field 를 별도로 지정하지 않으면 Producer의 메시지 전송 시각 또는 Topic에 메시지가 저장되는 시각으로 기록된다.
* ROWTIME값은 Stream/Table 생성 시 특정 컬럼을 메타 Timestamp field로 설정할 경우 해당 값으로 저장된다.
* ROWTIME은 기본적으로 Unix Epoch Time 형태로 저장된다.
* ROWTIME은 Stream/Table의 Window 연산의 기본 단위가 된다.

#### Event time, Ingestion time, Processing time

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/88e6e70f-f71e-49da-b775-f26bd62649dd.png)

* Event Time : 사건이 실제로 발생한 시각. 프로듀서가 찍고 레코드에 박혀 있어 재처리해도 같다.
* Ingestion Time : 브로커가 토픽에 기록한 시각. 발생이 아니라 도착 시점이다.
* Processing Time : 쿼리가 그 레코드를 읽은 시각. 저장되지 않고 재처리하면 매번 달라진다.

#### log.message.timestamp.type 설정값에 따른 Rowtime

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9ca8b557-c090-443f-be0d-5ebfc8ceee28.png)

* Producer는 자동으로 메시지의 생성 시점을 Timestamp 값으로 메시지에 추가하여 전송한다.
* Kafka Broker는 Producer로부터 전송된 메시지의 Timestamp 값을 Topic 저장 시 그대로 사용하거나 Broker가 Topic에 메시지를 저장한 시각을 기록할 수도 있다.
* Broker의 `log.message.timestamp.type` 또는 topic의 `message.timestamp.type` 속성값은 CreateTime(Producer 메시지 전송 시각), LogAppendTime(Broker Topic 저장 시각)으로 이들을 설정할 수 있다.

#### 커스텀 Rowtime 값 설정

```sql
CREATE STREAM device_status_stream (
    device_id   BIGINT,
    create_ts   VARCHAR,
    temperature DOUBLE,
    power_watt  INT
) WITH (
    KAFKA_TOPIC      = 'device_status_stream',
    PARTITIONS       = 3,
    KEY_FORMAT       = 'JSON',
    VALUE_FORMAT     = 'JSON',
    TIMESTAMP        = 'CREATE_TS',
    TIMESTAMP_FORMAT = 'yyyy-MM-dd''T''HH:mm:ss.SSS'
);
```

* Stream/Table의 Rowtime은 Create Stream/Table 생성 시 with절의 Timestamp 속성을 지정하여 설정할 수 있으며 이를 지정하지 않으면 기본적으로 레코드가 생성되는 시점의 Timestamp를 가진다.

#### Window 개요

* KSQLDB는 특정 시간 간격으로 Aggregation 연산을 편리하게 수행할 수 있도록 Window 기능을 제공한다.
* Window는 반드시 Group by와 함께 사용되어야 한다.
* Window절을 사용할 경우 자동으로 WINDOWSTART, WINDOWEND 컬럼이 할당되어 Group by 레벨로 사용될 수 있다.
* Window도 Kafka 메시지와 마찬가지로 시간상으로 한 방향으로 이동할 수 있다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/deed603a-e0e5-474a-9901-f065ecca5d35.png)

* Window절은 Stream/Table의 Rowtime을 기반으로 한다.
* Rowtime은 기본적으로 UTC 시간이다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a72fbbae-2c68-4a09-9afd-9f05e8f631e4.png)

* `Tumbling` : Window가 겹치지 않는다. 하나의 레코드는 단 하나의 Window에 소속된다.
* `Hopping` : Window가 서로 겹칠 수 있다. 하나의 레코드는 여러 개의 Window에 소속될 수 있다.
* `Session` : 데이터가 입력되지 않는 기간을 설정하며, Window가 데이터에 따라 동적으로 변경된다.

#### Tumbling Window

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9886aab8-d15d-4103-8bdd-ed4c592c2244.png)

* 고정 크기로 시간을 잘라, 겹치지도 비지도 않게 나누는 윈도우. 한 이벤트는 정확히 하나의 윈도우에만 속한다.

```sql
SELECT device_id, WINDOWSTART, WINDOWEND, COUNT(*) AS cnt
FROM device_status_stream
WINDOW TUMBLING (SIZE 1 MINUTES, RETENTION 7 DAYS, GRACE PERIOD 10 SECONDS)
GROUP BY device_id
EMIT CHANGES;
```

#### Hopping Window

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/8463fbad-a12f-4637-a664-35e25349c8c1.png)

* 고정 크기 윈도우를 일정 간격으로 밀면서 만드는 윈도우. 크기보다 간격이 작으면 윈도우끼리 겹치고, 한 이벤트가 여러 윈도우에 속한다.

```sql
SELECT device_id, WINDOWSTART, WINDOWEND, COUNT(*) AS cnt
FROM device_status_stream
WINDOW HOPPING (SIZE 5 MINUTES, ADVANCE BY 1 MINUTES, RETENTION 7 DAYS, GRACE PERIOD 10 SECONDS)
GROUP BY device_id
EMIT CHANGES;
```

#### Session Window

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/177166cb-5fbd-44e6-9c44-feee0ef9d5a7.png)

* 경계를 시각이 아니라 데이터가 정하는 윈도우. 이벤트가 계속 들어오면 세션이 계속 늘어나고, inactivity gap보다 긴 공백이 생기면 거기서 끊긴다.

```sql
SELECT user_id, WINDOWSTART, WINDOWEND, COUNT(*) AS cnt
FROM click_stream
WINDOW SESSION (5 MINUTES, RETENTION 7 DAYS, GRACE PERIOD 30 SECONDS)
GROUP BY user_id
EMIT CHANGES;
```

#### Window의 Grace Period란?

* 연속된 시간 흐름상에서 가장 최근 Window 시작 ~ 종료 기간과 맞지 않는 데이터가 입력될 경우 Window 연산이 적용되지 않도록 Grace Period 설정. Default Grace Period는 24시간이다.
* 가장 최근의 Window 기간보다 과거 입력 데이터가 들어오더라도 Grace Period을 감안한 기간으로 윈도우 기반 연산을 수행한다.

#### Window가 적용된 Table 생성 및 활용 시 제약 사항

* Window가 적용된 테이블의 key 컬럼은 Window 값을 반영하여 생성되며 Json/Avro format형으로 지정을 권장한다.
* Window절은 반드시 Group by와 함께 사용해야 한다. 만약 Window 레벨로 Group by를 하고자 한다면 Dummy값을 Group by절에 사용하여 적용 가능하다.
* Window가 적용된 테이블에 다시 Permanent Query를 수행할 수 없다.

#### Stream-Stream Window 조인

* Stream과 Stream은 Window 조인만 가능하며 `WITHIN`절을 사용하여 Window 조인을 수행한다.
* Stream과 Stream 조인은 대부분 조인 Key 값으로 M:M이 발생하기 쉽다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f93df96c-13fd-443c-a530-d3d678ca77c9.png)

#### 조인 Mview의 Rowtime

* `Stream-Stream` 조인 : 조인되는 2개의 데이터 중 가장 최근 Rowtime을 가진다.
* `Stream-Table` 조인 : 조인되는 2개의 데이터 중 Stream의 Rowtime을 가진다.
* `Table-Table` 조인 : 조인되는 2개의 데이터 중 가장 최근 Rowtime을 가진다.
