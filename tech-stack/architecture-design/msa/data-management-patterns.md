# MicroService Architecture - Data Management Patterns

#### Polyglot Persistence

* 마이크로서비스는 서로 간의 DB를 공유하지 않는다.
* 마이크로서비스에 따라 다른 DB 선택이 가능하다.
* 이렇게 되면 중복/분할된 데이터가 발생할 수 있는데 데이터 무결성과 데이터 일관성에 대한 문제 해결이 필요하며 `Strict Consistency` 또는 `Eventual Consistency` 적용이 필요하다.

#### Strict Consistency(엄격한 일관성)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/daf26d3e-ff7f-47d6-a531-5a07a9e6ad44.png)

* 강한 일관성은 데이터가 변경되는 즉시 모든 사본에 동일하게 반영되어, 어떤 시점에 어떤 사용자가 데이터를 읽더라도 항상 동일한 값을 보장하는 방식이다. 즉, 데이터가 한 번 갱신되면 그 직후부터는 어디에서 읽든 같은 결과가 나와야 한다.
* 쉽게 말하면, 어떤 사용자가 값을 변경했다면 다른 사용자는 절대로 “이전 값”을 보아서는 안 된다. 강한 일관성에서는 최신 데이터가 모든 시스템에 반영되기 전까지 읽기나 쓰기 과정이 제어토록 만든다.
* 강한 일관성의 가장 큰 장점은 신뢰성이다.
* 사용자는 언제 어디서 데이터를 읽더라도 같은 결과를 기대할 수 있고, 개발자 역시 시스템의 상태를 더 명확하게 이해할 수 있다. 그만큼 비즈니스 로직도 단순해지고, “어느 서버에서는 아직 반영되지 않았을 수 있다” 같은 예외 상황을 덜 고려해도 된다.
* 다만 강한 일관성은 그만큼 비용이 크다. 모든 사본이 같은 상태가 될 때까지 기다려야 하므로 응답 속도가 느려질 수 있고, 네트워크 지연이나 일부 노드 장애가 전체 처리에 영향을 줄 수 있다. 즉, 강한 일관성은 정확성과 신뢰성을 높여 주지만, 성능과 가용성 측면에서는 더 많은 희생을 요구하는 방식이기도 하다.

#### Eventual Consistency(최종적 일관성)

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2f74912b-1e9d-4266-a3c5-3c9c7ac8671d.png)

* 최종 일관성은 데이터를 어떻게 저장하든, 최종적으로는 해당 데이터를 갖고 있는 모든 데이터베이스가 동일한 상태로 수렴하면 된다는 개념이다.
* 즉, 데이터가 변경된 직후에는 잠시 동안 각 사본의 상태가 서로 다를 수 있지만, 시간이 지나면 결국 같은 값으로 맞춰진다는 것을 의미한다.
* 최종 일관성은 시스템의 처리 속도와 가용성을 높이기 위해 자주 사용된다.
* 모든 데이터 변경을 모든 서버에 즉시 반영하려고 하면 응답 시간이 느려질 수 있고, 일부 서버에 문제가 생겼을 때 전체 서비스가 영향을 받을 수도 있다.
* 반면 최종 일관성은 일시적인 불일치를 허용하는 대신, 더 빠르게 응답하고 더 유연하게 시스템을 운영할 수 있게 해준다.
* 즉, 최종 일관성은 데이터가 변경된 직후 짧은 시간 동안 사본 간 데이터 불일치 상태를 허용하지만, 결과적으로는 모든 사본이 동일한 상태로 수렴하는 것을 보장하는 개념이다.
* 이는 “지금 이 순간 완전히 같아야 한다”보다 “조금 늦더라도 결국 맞춰진다”에 더 초점을 둔 방식이라고 볼 수 있다.
* 최종 일관성은 유튜브 조회수, 인스타그램 좋아요 수, 게시글 조회 수처럼 잠시 값이 어긋나더라도 사용자에게 치명적인 문제가 되지 않는 서비스에서 유용하다.
* 이런 데이터는 몇 초 정도 늦게 반영되더라도 사용자가 큰 불편을 느끼지 않는 경우가 많다.
* 오히려 즉각적인 완전 일치를 위해 시스템 성능을 희생하는 것이 더 비효율적일 수 있다.
* 또한 SNS 피드, 댓글 수, 추천 수처럼 대규모 트래픽이 몰리는 서비스에서도 최종 일관성은 실용적이다.
* 수많은 사용자가 동시에 데이터를 읽고 쓰는 상황에서 모든 요청에 대해 즉시 완전한 동기화를 보장하려고 하면 시스템 비용이 급격히 커질 수 있기 때문이다. 이럴 때 최종 일관성은 현실적인 타협점이 된다.
* 즉, 최종 일관성은 사용자 경험에 큰 문제가 없는 범위 내에서만 효과적인 전략이다.
* 데이터의 약간의 지연이 허용되는 영역에서는 강력하지만, 그 지연이 곧바로 손해나 서비스 신뢰도 저하로 이어지는 영역에서는 신중하게 사용해야 한다.

#### Database per service Pattern

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/973e1559-e274-43f1-b32c-1e97664af089.png)

* 장점
  * 마이크로 서비스 - 느슨하게 결합, 확장 가능, 독립적
  * 분산 데이터 모델 - 특정 마이크로서비스를 위한 여러 개의 작은 DB로 구성된다.
  * Database Schema 변경 - 다른 마이크로서비스에 영향을 미치지 않고 수행이 가능하며 개별 DB의 변경은 다른 서비스에 영향을 주지 않는다.
  * 애플리케이션에 단일 장애 지점(SPOF)이 없다 - 애플리케이션이 탄력적이며 개별 DB로 1개의 마이크로서비스만 독립적으로 확장 가능하도록 구성할 수 있다.
  * DB 분리 - 마이크로서비스에 가장 최적화된 DB 선택이 가능하고 서비스 요구사항 및 기능에 따라 가장 효율적인 DB 사용이 가능하다.
  * DB 선택 - 관계형, NoSQL
* 단점
  * 서비스에는 데이터 교환, 서비스 간 통신을 위한 방법이 필요 - 각 서비스는 명확한 API 제공이 필요하며 Communications Resilience로 재시도 및 회로 차단기 패턴을 필요로 한다.
  * 마이크로서비스 간의 분산 트랜잭션 - Consistency + Atomicity 관점에서 부정적 영향을 미치며 복잡한 쿼리, 여러 데이터 저장소에서 조인 쿼리를 실행하기 어렵다.<br>

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/9abe1562-d404-4d2f-98ec-f44043c43a5f.png)

* Database per service 패턴을 따르지 않고 여러 마이크로서비스가 공유 데이터베이스를 사용한다.
* 공유 데이터베이스의 사용 - 마이크로서비스는 확장성, 복원성, 독립성이라는 속성을 잃게 된다. 또한 공유 데이터베이스는 단일 장애 지점으로 인해 마이크로서비스가 차단될 수 있다.

#### RDB와 NoSQL

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/19bf155b-314c-4e2e-8446-a65bffe062af.png)

* 관계형 데이터베이스 테이블
  * 고정된 스키마
  * SQL을 사용하여 데이터를 관리
  * ACID 원칙에 따라 트랜잭션 지원
  * 실제 데이터를 저장하기 위해 열과 행을 사용
* 키(Key)
  * PK : 각 테이블마다 고유한 값을 가져야 하는 열
  * FK : 한 테이블의 기본 키가 다른 테이블에서 사용되는 경우
  * UK : 중복된 값을 가질 수 없는 키

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/853c990c-40c1-4058-aef9-bb91602c667c.png)

* Non-Relational Database
* 구조화되지 않은 데이터를 저장
* 사용 편의성, 확장성, 복원성 및 가용성 특성
* NoSQL은 Key-Value 혹은 Documents(JSON) 타입으로 저장 - 구조화되지 않은 데이터를 저장한다.
* 다양한 유형의 저장된 데이터와 데이터 모델 - Document, Key-Value, Graph, Column
* ACID를 보장하지 않기 때문에 트랜잭션 관리가 필요하다.
* NoSQL - Document
  * JSON 기반 문서에 데이터를 저장하고 쿼리 - 데이터와 메타데이터는 계층적으로 저장한다.
  * 객체는 애플리케이션 코드에 매핑 - 컨텐츠 관리 및 카탈로그 저장
  * 확장성에 탁월하다.
  * MongoDB, Cloudant
* NoSQL - Key-Value
  * 데이터는 키-값 쌍의 컬렉션으로 저장한다.
  * 세션 지향 애플리케이션에 가장 적합한 선택이다.
  * Redis, Amazon DynamoDB, Azure CosmosDB, Oracle NoSQL
* NoSQL - Column-Based
  * 데이터는 열에 저장한다.
  * Column별로 독립적으로 확장 가능하다.
  * 빅 데이터 처리를 위한 Data Warehouse를 구축
  * Apache Cassandra, Apache HBase
* NoSQL - Graph
  * 데이터를 그래프 구조로 노드, 엣지, 데이터 속성에 저장
  * 그래프 관계를 저장하고 탐색한다.

#### Data Partitioning

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/10376bdd-029b-417f-9d15-e67f89e0209a.png)

* 수평 분할은 샤드를 기준으로 하는 방식이며 이 샤드는 데이터의 특정 하위 집합을 보유하게 된다.
* 파티션 키를 기준으로 테이블 데이터를 수평으로 분할한다.
* 각 파티션은 별도의 데이터 저장소이지만 모든 파티션은 동일한 스키마를 가지고 있다.
* 파티션 키는 모든 파티션에 데이터를 분배하는 데 제공된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/50ddb6d3-3264-4f33-a0fc-669ec4748622.png)

* 수직 분할은 행을 기준으로 하는 방식이며 각 분할은 데이터베이스 테이블에 대한 열의 하위 집합을 보유하게 된다.
* 열에 따라 분할된 데이터는 가장 많이 방문한 열과 다른 서버의 다른 열로 나눌 수 있다.
* 주로 거의 변경되지 않는 열과 자주 변경되는 열로 다른 서버에 나눈다.

#### Data Sharding

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/abbaa31d-a088-4af6-99d3-5cecb1823ca2.png)

* 데이터를 샤드 또는 청크라고 하는 고유한 작은 데이터베이스 조각으로 분리한다.
* 각 샤드는 동일한 스키마를 가지고 있으며 고유한 데이터 하위 집합을 보유한다.
* 확장성 증가 - 하드웨어 제한에 대한 걱정없이 시스템 확장이 가능하다.
* 가용성 향상 - 단일 장애 지점으로부터 시스템을 보호한다.
* 성능 향상 전체 - 데이터베이스를 쿼리하는 대신 시스템은 더 작은 구성요소만 쿼리한다.
* 보안 향상 - 민감한 데이터와 민감하지 않은 데이터를 다른 파티션에 저장한다.
* 데이터 관리 향상 - 테이블과 인덱스와 같은 작은 단위에 대해 관리/유지가 더 쉬워진다.

#### MySQL 기반 Sharding 정리

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/6b4e9f75-072b-4407-8edc-ed892766d302.png)

```java
// UUID 문자열을 받아 해시 기반 샤딩 키(int)로 변환
public class ShardingKeyUtil {

    public static int uuidToShardKey(String uuid, int shardCount) {
        return Math.abs(uuid.hashCode()) % shardCount;
    }
}
```
```java
// ThreadLocal로 현재 요청의 샤드 키 저장
// Spring의 AbstractRoutingDataSource를 상속해서 요청마다 어떤 DataSource를 쓸지 동적으로 결정한다.
public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<Integer> shardKey = new ThreadLocal<>();

    public static void setShardKey(int userId) {
        shardKey.set(userId % 2); // 예: 2개 샤드
    }

    public static void clear() {
        shardKey.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return shardKey.get();
    }
```
```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("shard1DataSource") DataSource shard1,
            @Qualifier("shard2DataSource") DataSource shard2) {

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(0, shard1);
        targetDataSources.put(1, shard2);

        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(shard1);
        return routingDataSource;
    }

    @Bean("shard1DataSource")
    public DataSource shard1() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3307/shard1")
                .username("root")
                .password("test1357")
                .build();
    }

    @Bean("shard2DataSource")
    public DataSource shard2() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3307/shard2")
                .username("root")
                .password("test1357")
                .build();
    }
}
```

* 두 샤드가 물리적으로 같은 MySQL 컨테이너, 논리적으로는 다른 DB가 된다.
* ThreadLocal 덕분에 멀티쓰레드 환경에서 요청별로 샤드가 독립적으로 관리가 된다.
* 조회 후 `RoutingDataSource.clear()`로 ThreadLocal이 정리되며 메모리 누수를 방지한다.
