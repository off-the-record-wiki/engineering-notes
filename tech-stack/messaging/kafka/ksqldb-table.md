# Apache Kafka - KSQLDB Table

#### Topic을 소스로 가지는 Table의 RocksDB 동작 메커니즘

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/fae784d3-5d67-4d55-bde1-f7d083775f29.png)

* `Push`쿼리 수행 시 `RocksDB`가 기동되어 `Stateful` SQL을 수행한다.
* `Change Log`는 `Push`쿼리 수행중에 데이터 변경 사항을 내부 토픽으로 가지며, `RocksDB` 복구 시 사용된다.
* `RocksDB` 기본 Storage 디렉터리는 `/tmp/kafka-streams`이며 KSQLDB 환경 설정으로 변경 가능하다.

#### 여러 개의 컬럼들을 PK로 가지는 Table

* `Table`은 여러 개의 컬럼들을 PK로 가질 수 있다. PK가 여러 컬럼을 가질 때, `KEY_FORMAT`은 `KAFKA`가 될 수 없으며, Json 또는 Avro와 같은 포맷이어야 한다,

```sql
create table test
( 
    cust_id integer primary key,
    cust_seq integer primary key,
    name varchar,
    email varchar
)
with
(
    KAFKA_TOPIC = 'topic',
    KEY_FORMAT = 'JSON',
    VALUE_FORMAT = 'JSON',
    PARTITIONS = 1
);
```
