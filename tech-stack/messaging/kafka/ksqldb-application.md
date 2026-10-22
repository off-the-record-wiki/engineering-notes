# Apache Kafka - KSQLDB Application

#### KSQL 주요 제약 사항 정리

> [KSQLDB Doc](https://docs.confluent.io/platform/current/ksqldb/overview.html)

* KSQL은 RDBMS SQL과 유사하다. 하지만 기능적으로 여러 제약이 존재한다.
* 또한 다양한 함수 지원 등에서 열세이다.
* KSQLDB는 Offset 기반의 데이터 처리를 기반으로 한다. 데이터는 시간이 지나면서 계속 추가되며, Update/Delete는 발생하지 않는 구조이다.
* KSQL은 `subquery`를 지원하지 않는다.
* KSQL은 `inline view`를 지원하지 않고 대신 `materialized view`를 지원한다.
* KSQL은 `order by`를 지원하지 않는다.
* KSQL은 `union/union all`을 지원하지 않는다.
* KSQL은 `windows`절을 가지고 있으며, `windows`절에 기술된 기간만큼만 데이터를 액세스할 수 있다.
* KSQL의 `windows`절은 RDBMS SQL의 window function과 다르게 from절에 `stream/tables` 접근 기간을 정하는데 사용된다.
* KSQL은 `join`대상 `stream/table`들이 서로 같은 파티션 갯수, 파티션 키로 구성되어야 한다.

#### Date, Time, Timestamp 타입 컬럼의 변환 함수

* `DATE` : 시분초를 제외한 월일년만 가지는 `Temporal` 타입 → 2023년 2월 1일
* `TIME` : 월일년을 제외한 시분초만 가지는 `Temporal` 타입 → 11시 59분 59초
* `TIMESTAMP` : 월일년 시분초, 밀리세컨드까지 가지는 `Temporal` 타입 → 2023년 2월 1일 11시 59분 59초 (10ms)

#### Date, Time, Timestamp ⭤ 문자열 변환 함수

||Temporal to String 변환|String to Temporal 변환|
|:---:|:---:|:---:|
|`DATE`|`FORMAT_DATE(Date, 'yyyy-MM-dd)`|`PARSE_DATE('2023-02-01', 'yyyy-MM-dd')`|
|`TIME`|`FORMAT_TIME(Time, 'HH:mm:ss)`|`PARSE_TIME('11:59:59', 'HH:mm:ss')`|
|`TIMESTAMP`|`FORMAT_TIMESTAMP(Timestamp, 'yyyy-MM-dd HH:mm:ss)`|`PARSE_TIMESTAMP('2023-02-11 11:59:59', 'yyyy-MM-dd HH:mm:ss')`|

#### Date, Timestatmp ⭤ Unix epoch 일자/Millisecond값 변환

||Date/Timestamp값을 int/bigint형 Unix epoch일자/Millisecond값 변환|int/bigint형 Unix epoch일자/Millisecond값을 Date/Time값으로 변환|
|:---:|:---:|:---:|
|`DATE`|`UNIX_DATE(Date)`|`FROM_DAYS(Unix Epoch 일자)`|
|`TIMESTAMP`|`UNIX_TIMESTAMP(Timestamp)`|`FROM_UNIXTIME(Unix Epoch Millisecond)`|

#### Date, Time, Timestamp 더하기/빼기 연산

||더하기|빼기|
|:---:|:---:|:---:|
|`DATE`|`DATEADD(Days, 7, Date)`|`DATESUB(Days, 7, Date)`|
|`TIME`|`TIMEADD(Hours, 7, Time)`|`TIMESUB(Hours, 7, Time)`|
|`TIMESTAMP`|`TIMESTAMPADD(Minutes, 10, Timestamp)`|`TIMESTAMPSUB(Minutes, 10, Timestamp)`|

#### Kafka의 Default Timezone

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/65409d1f-3247-4d01-b209-e0b3ac94433d.png)

* Kafka Producer가 전송하는 메시지는 Header에 메시지 전송 시각을 UTC 시각으로 가지고 있다.
* 전송된 메시지는 Kafka Broker의 `log.message.timestamp.type`의 설정값에 따라 Producer 전송 시각을 저장할 수도, Broker에서 Topic에 기록한 시간을 저장할 수도 있다.
