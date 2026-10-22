# Apache Kafka - Consumer Mechanism

#### Consumer의 주요 메커니즘 개요

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fb56a836-6a50-474e-8d95-4bb7d906b76a.png)

* Broker Topic의 메시지를 읽는 역할을 수행한다.
* 모든 Consumer들은 고유한 그룹 아이디 `group.id`를 가지는 Consumer Group 내에 소속되어야 한다.
* 개별 Consumer Group 내에서 여러 개의 Consumer들은 토픽 파티션별로 분배된다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d9f80bfc-6886-4a0e-8297-494580dfc941.png)

* Consumer는 `subscribe()`를 호출하여 읽어들이고자하는 토픽을 등록한다.
* Consumer는 `poll()`를 호출하여 주기적으로 브로커 토픽 파티션에서 메시지를 가져온다.
* 메시지를 성공적으로 가져왔으면 `commit`을 통해서 `__consumer_offsets`에 다음에 읽을 offset 위치를 적어둔다.

#### Java 기반에서 Consumer 구현

```java
@Configuration
public class KafkaConsumerConfig {

    private static final String TOPIC_NAME = "my-topic";
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public Properties consumerConfigs() {
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "my-group-id");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(TOPIC_NAME));    // 읽어들이고자하는 토픽을 등록한다.

        while (true) {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                log.info("Received message: key={}, value={}, partition={}, offset={}",
                        consumerRecord.key(), consumerRecord.value(),
                        consumerRecord.partition(), consumerRecord.offset());
            }
            consumer.close();
        }
    }
}
```

#### KafkaConsumer 클래스의 주요 구성 요소 및 poll() 메서드 동작 메커니즘의 이해

```java
while (true) {
    ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
    for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
        log.info("Received message: key={}, value={}, partition={}, offset={}",
            consumerRecord.key(), consumerRecord.value(),
            consumerRecord.partition(), consumerRecord.offset());
        }
    consumer.close();
}
```

* 브로커 Consumer 내부의 Queue에 데이터가 있다면 바로 데이터를 반환한다.
* 그렇지 않을 경우에는 1000ms 동안 데이터 Fetch를 브로커에 계속 수행하고 결과를 반환한다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fcfde2e8-cce2-43d5-b6e5-ec060406e8dd.png)

* `Linked Queue`에 데이터가 있을 경우 `Fetcher`는 데이터를 가져오고 반환하며 `poll()` 메서드를 수행한다.
* `ConsumerNetworkClient`는 비동기로 계속 브로커 메시지를 가져와서 `Linked Queue`에 저장한다.
* `Linked Queue`에 데이터가 없을 경우 1000ms까지 브로커에 메시지 요청 후 `poll()` 메서드를 수행한다.

#### Consumer Fetcher

* 결국 `Fetcher`란, `Linked Queue`에 데이터를 가져오되, `Linked Queue`에 데이터가 없을 경우 `ConsumerNetworkClient`를 통해서 데이터를 브로커로부터 가져올 것을 요청하는 과정이다.

#### Consumer Fetcher 관련 주요 파라미터와 Fetcher 메커니즘의 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/443e35df-b99c-440f-abdd-38e2494464a7.png)

* `fetch.min.bytes` : fetcher가 레코드들을 읽어들이는 최소 bytes. 브로커는 지정된 `fetch.min.bytes` 이상의 새로운 메시지가 쌓일 때까지 전송을 하지 않는다. 기본은 1이다.
* `fetch.max.wait.ms` : 브로커에 `fetch.min.bytes` 이상의 메시지가 쌓일 때까지 최대 대기 시간으로 기본은 500ms이다.
* `fetch.max.bytes` : fetcher가 한 번에 가져올 수 있는 최대 데이터 bytes. 기본은 50MB이다.
* `max.partition.fetch.bytes` : fetcher가 파티션별 한 번에 최대로 가져올 수 있는 bytes.
* `max.poll.records` : fetch가 한 번에 가져올 수 있는 레코드 수로 기본은 500이다.

#### Consumer의 읽기 - commit() 메서드 메커니즘의 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/b3d7b99a-435f-42f2-bcc4-be197c771ea7.png)

* Consumer는 `subscribe()`를 호출하여 읽어들이려는 토픽을 등록한다.
* Consumer는 `poll()`를 호출하여 주기적으로 브로커의 토픽 파티션으로부터 메시지를 가져온다.
* 메시지를 성공적으로 가져왔다면 `commit`을 통해서 `__consumer_offsets`에 다음에 읽어야 할 offset 위치를 기재하게 된다.

#### auto.offset.reset의 내부 동작 메커니즘의 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2cd7cb62-e8ff-4922-abc7-1b359acc160d.png)

* `__consumer_offsets`에 Consumer Group이 해당 Topic의 Partition별로 offset 정보를 가지고 있지 않을 시 Consumer가 최초 접속 시 해당 파티션의 처음 offset(earliest)부터 가져올 것인지 아니면 마지막 offset(latest)부터 가져올 것인지를 설정하는 파라미터가 바로 `auto.offset.reset`이다.
* 동일 Consumer Group으로 Consumer가 새롭게 추가 혹은 변경된다면 `__consumer_offsets`에 있는 offset 정보를 기반으로 메시지를 가져오기 때문에 earliest로 설정해도 0번 offset부터 읽어들이지 않는다.
* Consumer Group의 Consumer가 모두 종료되어도 Consumer Group이 읽어들인 offset 정보들은 7일동안 `__consumer_offsets`에 저장되는데 이 파라미터는 `offsets.retention.minutes`에 의해 결정된다.
* 해당 Topic이 삭제되고 재생성될 경우에는 해당 Topic에 대한 Consumer Group의 offset 정보는 0으로 `__consumer_offsets`에 기록된다.

#### Consumer Group 내에 Consumer 구성 변화가 있을 경우 발생하는 리밸런싱에 대한 이해

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/27de5a25-2a21-400b-a737-edf6396f41fb.png)

* Consumer Group 내에 새로운 Consumer가 추가되거나 기존 Consumer가 종료될 때 혹은 Topic에 새로운 Partition이 추가될 때 Broker의 Group Coordinator는 Consumer Group 내의 Consumer들에게 파티션을 재할당하는 Rebalancing을 수행하도록 지시한다.
* `session.timeout.ms` 이내에 HeartBeat 응답이 없거나 `max.poll.interval.ms`이내에 `poll()` 메서드가 호출되지 않는 경우 Rebalancing이 발생한다.
* Consumer Group 내의 Consumer가 브로커에 최초 접속 요청 시 Group Coordinator기 생성된다.
* 동일 `group.id`로 여러 개의 Consumer가 Broker의 Groop Coordinator에 접속한다.
* Group에 가장 빨리 Join 요청을 한 Consumer에게 Consumer Group 내의 Leader Consumer로 지정한다.
* Leader Consumer는 최종 할당된 파티션 정보를 Group Coordinator에게 전달한다.
* 정보 전달 성공을 공유한 뒤 개별 Consumer들은 할당된 파티션으로부터 메시지를 읽는다.

#### Consumer Group Status

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/18d8b2e5-f594-4f69-9a3b-e51d78ec2f99.png)

* `Empty` : Consumer Group 메타 데이터는 존재하나 현재 연결된 Consumer는 0개
* `Rebalance` : Consumer Group 내의 Consumer 구성 변경으로 파티션 재할당이 필요한 단계
* `Stable` : 파티션 할당이 완료되어 Consumer들이 정상적으로 메시지를 소비하는 단계

#### Static Group Membership

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a3038e47-40af-483d-bf5b-35bbb7a2e8c0.png)

* 많은 Consumer를 가지는 Consumer Group에서 Rebalance가 발생하면 모든 Consumer들이 Rebalance를 수행하므로 많은 시간이 소모되고 대량 데이터 처리 시 Lag가 더 길어질 수 있다.
* 유지보수 차원의 Consumer Restart도 Rebalance를 초래하기 때문에 불필요한 Rebalance를 발생시키지 않도록 하기 위한 방법으로 Static Group Membership이 거론되었다.
* Consumer Group 내의 Consumer들에게 고정된 id를 부여한다.
* Consumer별로 Consumer Group 최초 조인 시, 할당된 파티션을 그대로 유지하고 Consumer가 shutdown되어도 `session.timeout.ms`내에 재기동되면 rebalance가 수행되지 않고, 기존 파티션이 재할당된다.
* 위의 예시에서 Consumer #3가 종료되었지만 rebalance가 일어나지 않으며 Partition #3는 다른 Consumer에 재할당되지 않고 읽혀지지 않는다.
* Consumer #3가 `session.timeout.ms`내에 기동되면 Partition #3는 Consumer #3에 다시 할당되지만 `session.timeout.ms`내에 기동되지 않으면 rebalance가 발생하고 Partition #3가 다른 Consumer에 할당된다.
* Static Group Membership을 적용할 경우 `session.timeout.ms`를 좀 더 큰 값으로 설정할 수 있다.

#### Heartbeat

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/62183e08-0410-42c7-8ab5-cc724c1c2155.png)

* `HeartBeat Thread`는 브로커의 Group Coordinator에 Consumer 상태를 전송한다.

#### Heartbeat 관련 주요 파라미터

* `heartbeat.interval.ms` : `HeartBeat Thread`가 `HeartBeat`을 보내는 간격. `session.timeout.ms`보다 낮게 설정되어야 하며 보통 `session.timeout.ms`의 1/3보다 낮게 설정하는 것을 권장한다. 기본값은 3000ms이다.
* `session.timeout.ms` : 브로커가 Consumer로부터 `HeartBeat`을 기다리는 최대 시간. 브로커는 이 시간동안 `HeartBeat`를 Consumer로부터 받지 못하면 해당 Consumer를 Consumer Group에서 제외하도록 rebalance를 지시한다. 기본값은 45000ms이다.
* `max.poll.interval.ms` : 이전 `poll()` 메서드 호출 후 다음 `poll()`까지 브로커가 기다리는 시간. 해당 시간동안 `poll()` 호출이 Consumer로부터 이뤄지지 않으면 해당 Consumer는 문제가 있는 것으로 판단하고 브로커가 rebalance를 지시한다. 기본값은 300000ms이다.

#### ✅ max.poll.interval.ms 설정값

```java
while (true) {
    ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
    for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
        log.info("Received message: key={}, value={}, partition={}, offset={}",
            consumerRecord.key(), consumerRecord.value(),
            consumerRecord.partition(), consumerRecord.offset());
        }
    consumer.close();
}
```

* `poll()` 호출 간격이 `max.poll.interval.ms`를 넘으면 rebalance가 수행된다는 점을 기억하자.

#### Consumer Rebalance - Eager 모드

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fd3b62cb-4dfe-404e-a6d5-337dea6b6130.png)

* Rebalance 수행 시 기존 Consumer들의 모든 파티션 할당을 취소하고 잠시 메시지를 읽지 않는다. 이후 새롭게 Consumer에 파티션을 다시 할당받고 다시 메시지를 읽는다.
* 모든 Consumer가 잠시 메시지를 읽지 않는 시간으로 인해 Lag가 상대적으로 크게 발생할 가능성이 있다.

#### Consumer Rebalance - Cooperative 모드

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/97442696-f7c1-4304-877e-408571197312.png)

* Rebalance 수행 시 기존 Consumer들의 모든 파티션 할당을 취소하지 않고 대상이 되는 Consumer들에 대해서 파티션에 따라 점진적으로 Consumer를 할당하면서 Rebalance를 수행한다.
* 전체 Consumer가 메시지 읽기를 중지하지 않으며 개별 Consumer가 협력적으로 영향을 받는 파티션만 Rebalance로 재분배한다. 많은 Consumer를 가지는 Consumer Group 내에서 Rebalance 시간이 오래 걸릴 경우에 활용도가 높다.

#### Consumer 파티션 할당 전략 - Round Robin과 Range 비교

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/25649cbe-ace8-4deb-ae8e-9f585878d210.png)

* Round Robin은 토픽들의 파티션별로 순차적으로 Consumer에 할당하므로 파티션 매핑이 Consumer별로 비교적 균일하게 할당된다.
* Range는 서로 다른 토픽들의 동일한 파티션들을 같은 Consumer에 할당한다. 서로 다른 토픽에서 동일한 키 값을 가지는 파티션들은 같은 Consumer에서 처리할 수 있도록 유도하는 전략이다.

#### Round Robin의 Rebalance 후 Partition - Consumer 매핑 과정

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/a2d0dc63-4fc3-4f37-9131-2a7261479a74.png)

* Round Robin의 경우 Rebalance 후에도 토픽들의 파티션과 Consumer들을 균등하게 매핑하려고 하기 때문에 Rebalance 이전의 파티션과 Consumer들의 매핑이 변경되기 쉽다.

#### Sticky의 Rebalance 후 Partition - Consumer 매핑 과정

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/3b51dc1e-910d-4fc1-a729-e4075dd5e50e.png)

* Rebalance 후 기존 토픽들의 파티션과 Consumer 매핑은 최대한 유지하도록 한다. 재할당되어야 하는 파티션들만 Consumer들에 할당된다.
* 하지만 모든 Consumer들의 파티션이 일제히 취소되는 Eager 기반에서만 동작한다.

#### Cooperative Sticky의 Rebalance 후 Partition - Consumer 매핑 과정

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/d68f74f5-fa74-48d2-bc86-e061c43a70fb.png)

* Sticky와 유사하지만 모든 매핑을 다 취소하지 않는다.
* 재할당되어야 할 파티션만 Consumer에 따라 순차적으로 Rebalance를 수행하여 할당을 수행한다.

#### Consumer의 Offset Commit과 중복 읽기 상황

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2f94270c-a3c1-4689-b9a6-6f8fd1c33365.png)

* `__consumer_offsets`에는 Consumer Group이 특정 Topic의 Partition별로 읽기 commit한 offset 정보를 가진다.
* 특정 파티션을 어느 Consumer가 commit 했는지는 알 수 없다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4bcd3c71-6ddb-4fe7-8d4d-296a77d6c9e0.png)

* Consumer #1에서 `poll()`로 읽기만 하고 아직 커밋되지 않은 상태에서 만약 Consumer #1이 다운되어 리밸런싱이 생겼다고 가정하자.
* Consumer #2에서 `poll()`을 호출하게 되면 아직 커밋되지 않은 offset부터 다시 읽어서 처리하게 되기 때문에 중복으로 처리하는 문제가 발생한다.

#### Consumer의 Auto Commit과 중복 읽기 상황

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/202fdcab-5b2f-4ed1-8afb-b4a7d42ed2b8.png)

* Consumer의 파라미터로 `auto.enable.commit=true`인 경우 읽어온 메시지를 브로커에 바로 commit하지 않고 `auto.commit.interval.ms`(기본적으로는 5초)에 의해 정해진 주기마다 Consumer가 자동으로 commit을 수행한다.
* Consumer가 읽어온 메시지보다 브로커의 commit이 오래 되었으므로 Consumer 장애/재기동 시 Rebalance 후 이미 읽어온 메시지를 다시 읽어와서 중복 처리될 우려가 있다.

#### Consumer의 동기 및 비동기 Manual Commit

* Consumer Client는 일정 주기마다 자동으로 commit을 하지 않고 API를 이용해 동기 또는 비동기 commit을 적용할 수 있다.

#### sync

* Consumer 객체의 `commitSync()` 메서드를 사용한다.
* 메시지 배치를 `poll()`을 통해서 읽어오고 해당 메시지들의 마지막 offset을 브로커에 commit을 적용한다.
* 브로커에 commit 적용이 성공적으로 될 때까지 블로킹이 적용된다.
* commit 적용 완료 후에 다시 메시지를 읽어온다.
* 브로커에 commit 적용이 실패하면 다시 commit 적용을 요청한다.
* 비동기 방식 대비해 수행 시간이 좀 더 오래 걸린다.

#### async

* Consumer 객체의 `commitAsync()` 메서드를 사용한다.
* 메시지 배치를 `poll()`을 통해서 읽어오고 해당 메시지들의 마지막 offset을 브로커에 commit을 적용 요청하지만 브로커에 commit 적용이 성공적으로 되었는지를 기다리지 않고(=블로킹을 하지 않는다) 계속 메시지를 읽어온다.
* 브로커에 commit 적용이 실패해도 다시 commit 시도를 하지 않는다. 그렇기 때문에 Consumer Group 내의 Consumer 구성 변화 혹은 장애/재기동 시 Rebalance가 발생하게 되면 메시지 중복 처리가 발생할 수 있다.
* 동기 방식 대비해 수행 시간이 더 빠르다.

#### Consumer에서 토픽의 특정 파티션만 할당하는 방법

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/4c751a8b-b595-480c-afc2-b946b2614c20.png)

* Consumer에 여러 개의 파티션이 있는 Topic에서 특정 파티션만 할당하는 것이 가능하다.
* 배치 처리 시 특정 key 레벨의 파티션을 특정 Consumer에 할당하여 처리할 경우 적용할 수 있다.
* `KafkaConsumer`의 `assign()` 메서드에 `TopicPartition` 객체로 특정 파티션을 인자로 입력하면 할당이 가능하다.
```java
TopicPartition topicPartition = new TopicPartition(topicName, 0);
kafkaConsumer.assign(Arrays.asList(topicPartition));
```

#### Consumer에서 토픽의 특정 파티션의 특정 offset부터 읽어오는 방법

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/2880f38d-0fa0-4a43-9a72-84a478917c99.png)

* 특정 메시지가 누락되었을 경우 해당 메시지를 다시 읽어오기 위해 유지보수 차원에서 일반적으로 사용하는 방법이다.
* `TopicPartition` 객체로 할당할 특정 파티션을 설정하고 `seek()` 메서드로 읽어올 offset을 설정한다.
```java
TopicPartition topicPartition = new TopicPartition(topicName, 1);
kafkaConsumer.assign(Arrays.asList(topicPartition));
kafkaConsumer.seek(topicPartition, 6L);
```
