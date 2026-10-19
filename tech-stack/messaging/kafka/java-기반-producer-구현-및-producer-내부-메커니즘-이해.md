# Apache Kafka - Producer Mechanism

#### Java 기반 Producer 구현

```java
@Configuration
public class KafkaProducerConfig {

    private static final String TOPIC = "simple-topic";

    @Bean
    public Properties kafkaProperties() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, "Hello, Kafka!");
        producer.send(record);
        producer.flush();
        producer.close();
        return properties;
    }
}
```

#### Producer 메시지 비동기화 전송 방식과 동기화 전송 방식 비교

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/0c12ea56-4ccf-43ef-9215-653836b91941.png)

#### Sync(동기 방식)

* Producer는 브로커로부터 해당 메시지를 성공적으로 받았다는 `Ack` 메시지를 받은 후 다음 메시지를 전송한다.
* `KafkaProducer.send().get()`을 호출하여 브로커로부터 `Ack` 메시지를 받을 때까지 대기한다.

#### Async(비동기 방식)

* Producer는 브로커로부터 해당 메시지를 성공적으로 받았다는 `Ack` 메시지를 기다리지 않고 전송한다.
* 브로커로부터 `Ack` 메시지를 비동기로 Producer에 받기 위해 Callback을 적용한다.
* `send()` 메서드 호출 시에 callback 객체를 인자로 입력해 `Ack` 메시지를 Producer로 전달받을 수 있다.
* `KafkaProducer.send()`는 기본적으로 비동기 호출 방식으로 적용된다.
* 동기식으로 전환하려면 `Future` 객체의 `get()`을 호출하여 브로커로부터 메시지 `Ack` 응답을 받을 때까지 블로킹시키는 방식으로 동기화를 적용해야한다.

#### Callback을 이용한 Producer와 브로커와의 메시지 전송/재전송

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/ae7a16a7-7abf-4985-8cda-51e2cb6e1edd.png)

* `Ack`로 응답을 보낼 때, 내부에 실행 결과 혹은 Exception이 담기게 된다.

#### Key값을 가지는 메시지 전송 구현 방법

```java
@Configuration
public class KafkaProducerConfig {

    private static final String TOPIC = "simple-topic";

    @Bean
    public Properties kafkaProperties() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return properties;
    }

    @Bean
    public CommandLineRunner commandLineRunner(Properties kafkaProperties) {
        return args -> {
            KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProperties);
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, "my-key", "Hello, Kafka with key!");
            producer.send(record);
            producer.flush();
            producer.close();
        };
    }
}
```

#### Key값을 가지는 메시지 전송 구현 방법(feat. Custom Callback 구현)

```java
@Configuration
public class KafkaProducerConfig {

    private static final String TOPIC = "simple-topic";

    @Bean
    public Properties kafkaProperties() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return properties;
    }

    @Bean
    public CommandLineRunner commandLineRunner(Properties kafkaProperties) {
        return args -> {
            KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProperties);
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, "my-key", "Hello, Kafka with implemented callback!");

            // Callback 인터페이스를 직접 구현한 익명 클래스 사용
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null) {
                        // 성공 처리
                        System.out.println("Message sent successfully to topic " + metadata.topic() +
                                " partition " + metadata.partition() +
                                " with offset " + metadata.offset());
                    } else {
                        // 실패 처리
                        System.err.println("Failed to send message: " + exception.getMessage());
                    }
                }
            });

            producer.flush();
            producer.close();
        };
    }
}
```

#### Acks = 0인 경우

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3ab82ce1-ef4e-4c71-b3f6-82080946bf8f.png)

* Producer는 Broker가 메시지를 정상적으로 받았는가에 대한 `Ack` 메시지를 받지 않고 다음 메시지를 바로 전송한다.
* 메시지가 제대로 전송되었는지 브로커로부터 확인을 받지 않기 때문에 메시지가 브로커에 기록되지 않더라도 재전송되지 않는다.
* 메시지 손실의 우려가 가장 크지만 가장 빠르게 전송할 수 있을 때 많이 사용하는 옵션이다. 즉, 데이터 손실에 민감하지 않은 데이터 전송에 활용할 수 있다.

#### Acks = 1인 경우

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/89e9f547-4fe4-48dd-a4cc-b23063b1699f.png)

* Producer는 Broker가 메시지를 정상적으로 받았는가에 대한 `Acks` 메시지를 받고 다음 메시지를 전송한다. 만약 오류 메시지를 브로커로부터 받았다면 이전에 보냈던 메시지를 재전송한다.
* 메시지가 모든 Replication에 복제되었는가의 여부는 확인하지 않고 메시지를 전송한다.
* 만약 Leader Broker가 메시지를 복제하는 과정 중에서 다운될 경우 다음 Leader Broker가 될 브로커에는 메시지가 없을 수 있기 때문에 메시지가 소실될 우려가 있다.

#### Acks = all인 경우

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c68eb738-22ad-4fcd-b5ae-7e817438803c.png)

* Producer는 Leader Broker가 메시지를 정상적으로 받은 뒤 `min.insync.replicas` 개수 만큼의 Replication에 복제를 수행한 뒤에 보내는 `Ack` 메시지를 받은 후 다음 메시지를 바로 전송한다.
* 이 때, 오류 메시지를 브로커로부터 받았다면 처음에 보낸 메시지를 재전송한다.
* 메시지가 모든 Replication에 복제가 되었는가의 여부까지 확인한 후 다음 메시지를 전송한다.
* 메시지 손실이 되지 않도록 모든 장애 상황을 감안한 전송 모드이지만 `Acks`를 오래 기다려야 하기 때문에 상대적으로 전송 속도가 느리다.

```java
@Configuration
public class KafkaProducerConfig {

    private static final String TOPIC = "simple-topic";

    @Bean
    public Properties kafkaProperties() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        return properties;
    }
}
```

#### Producer 메시지 배치 전송 내부 메커니즘

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b45819b1-1048-4f93-ae16-b5d24e97c1bf.png)

* `KafkaProducer` 객체의 `send()` 메서드는 호출 시마다 하나의 `ProducerRecord`를 입력하지만 바로 전송되지않고 내부 메모리에서 단일 메시지를 토픽 파티션에 `Record Batch`단위로 묶인 뒤 전송된다.
* 메시지들은 `Producer Client`의 내부 메모리에 여러 개의 Batch들로 `buffer.memory` 설정 사이즈만큼 보관될 수 있으며 여러 개의 Batch들로 한꺼번에 전송될 수 있다.

```java
// 카프카에게 보낼 키/값 쌍. 이는 레코드가 전송되는 주제 이름, 선택적 파티션 번호, 그리고 선택적 키와 값으로 구성됩니다.
// 유효한 파티션 번호가 지정되면 레코드를 보낼 때 해당 파티션이 사용됩니다. 파티션이 지정되지 않았지만 키가 존재한다면, 키의 해시를 사용해 파티션을 선택합니다. 키와 파티션이 모두 없으면 라운드로빈 방식으로 파티션이 할당됩니다. 파티션 번호는 0-인덱스임을 유의하세요.
// 레코드에는 타임스탬프도 함께 붙어 있습니다. 사용자가 타임스탬프를 제공하지 않았다면, 제작자는 현재 시간을 기록에 스탬핑합니다. 카프카가 최종적으로 사용하는 타임스탬프는 해당 주제에 맞게 설정된 타임스탬프 유형에 따라 달라집니다.
// CreateTime설정되어 있다면, 프로듀서 레코드의 타임스탬프는 브로커가 사용합니다.
// LogAppendTime설정되어 있다면, 프로듀서 레코드의 타임스탬프는 브로커가 메시지를 로그에 추가할 때 브로커의 현지 시간으로 덮어쓰게 됩니다.
public class ProducerRecord<K, V> {

    private final String topic;
    private final Integer partition;
    private final Headers headers;
    private final K key;
    private final V value;
    private final Long timestamp;

    // ...
}
```

#### Record Batch와 Record Accumulator 이해

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/a0bce108-0a0a-4ff8-93b5-ac1103c518bb.png)

* `Record Accumulator`는 `Partitioner`에 의해서 메시지 배치가 전송이 될 토픽과 파티션에 따라 저장되는 KafkaProducer 메모리 영역이다.
* `Sender Thread`는 `Record Accumulator`에 누적된 메시지 배치를 꺼내서 브로커로 전송한다.
* `Main Thread`는 `send()` 메서드를 호출하고 `Record Accumulator`에 데이터를 저장하고 `Sender Thread`는 별개로 데이터를 브로커에 전송한다.

#### linger.ms와 batch.size 파라미터 이해

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5e8c7f08-5b6d-44d5-9f18-98b1f2e1d79a.png)

* `Sender Thread`는 기본적으로 전송할 준비가 되어 있다면 `Record Accumulator`에서 1개의 Batch를 가져갈수도, 여러 개의 Batch를 가져갈 수도 있다. 또한 Batch에 메시지가 다 차지 않더라도 가져갈 수 있다.
* `linger.ms`를 0보다 크게 설정하면 `Sender Thread`가 하나의 Batch Record를 가져갈 때까지 일정 시간 대기하여 `Record Batch`에 메시지를 많이 채울 수 있도록 설정할 수 있다.
* `linger.ms`는 보통 20ms 이하로 설정을 권장하며, 전송이 느리다고 판단된다면 `linger.ms`를 높여서 메시지가 배치로 적용될 수 있는 확률을 높이는 시도를 할 수 있다.

#### 전송/재전송 내부 메커니즘

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/db40264e-b7eb-4952-aa4e-746412e57d9f.png)

* `retries`와 `delivery.timeout.ms`를 이용해 재전송 횟수를 조정한다.
* `retries`는 재전송 횟수를 설정한다.
* `delivery.timeout.ms`는 메시지 재전송을 멈출 때까지의 시간이다.
* `retries` 설정 횟수만큼 재전송을 시도하다가 `delivery.timeout.ms`가 되면 재전송을 중지한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/bd3494b8-34a1-4fd5-8d6e-d5ca16aabe94.png)

* `retry.backoff.ms`는 재전송 주기 시간을 설정한다.
* `request.timeout.ms`만큼 기다린 후 재전송하기 전 30ms 이후 재전송을 시도한다. 이와 같은 방식으로 총 10회를 시도하고 더 이상 retry를 하지 않는다.
* 만약 10회 이내에 `delivery.timeout.ms`에 도달하면 더 이상 retry를 하지 않는다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/c73125be-8e09-45d5-93c5-6fa7d040bf05.png)

* `max.in.flight.requests.per.connection`이란 브로커 서버 응답없이 Producer의 `Sender Thread`가 한 번에 보낼 수 있는 메시지 배치의 개수로 기본값은 5이다.

#### 최대 한 번 전송(at most once)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/b8923153-9db4-4350-a7c8-ad53a4c0fbd6.png)

* Producer는 Broker로부터 `Ack` 또는 에러 메시지 없이 다음 메시지를 연속적으로 보낸다.
* 메시지가 소실될 수 있지만 중복 전송은 하지 않는다.

#### 적어도 한 번 전송(at least once)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/eea38ae3-e619-465b-8444-e9bb5f602b97.png)

* Producer는 Broker로부터 `Ack`를 받은 다음에 다음 메시지를 전송한다.
* 메시지 소실은 없지만 중복 전송을 할 수 있다.

#### 정확히 한 번 전송(exactly once)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3fd173f1-98c9-44c0-ad52-4bd7988dd51b.png)

* Producer는 Broker로부터 `Ack`를 받은 다음에 다음 메시지를 전송하되, Producer ID와 메시지 Sequence를 Header에 저장하여 전송한다.
* 메시지 Sequence는 메시지 고유 Sequence 번호를 말하며 0부터 시작해 순차적으로 증가한다.
* Producer ID는 Producer가 기동시마다 새롭게 생성된다.
* 브로커에서 메시지 Sequence가 중복될 경우 이를 메시지 로그에 기록하지 않고 `Ack`만 전송된다.
* 브로커는 Producer가 보낸 메시지의 Sequence가 브로커가 가지고 있는 메시지의 Sequence보다 1만큼 큰 경우에만 브로커에 저장한다.

#### Idempotence(중복 없이 전송) 설정 및 설정 시 유의사항

* Kafka 3.0 버전부터는 Producer의 기본 설정이 Idempotence이다.
* 그러나 기본 설정 중에 `enable.idempotence=true`를 제외하고 다른 파라미터들을 잘못 설정하면 Producer는 정상적으로 메시지를 보내지만 Idempotence로는 동작하지 않는다.
* 명시적으로 `enable.idempotence=true`를 설정한 뒤 다른 파라미터들을 잘못 설정하면 Config 오류가 발생하면서 Producer가 기동되지 않게 된다.

#### Idempotence 기반에서 메시지 전송 순서 유지

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/9720e4a5-3103-41a0-bae3-ce081e99d954.png)

* 위의 예시를 본다면 B0이 가장 먼저, B1, B2 순에서 Producer에서 생성된 메시지 배치이다.
* Idempotence 기반에서 `max.in.flight.requests.per.connection`만큼 여러 개의 배치들이 Broker에 전송된다.
* Broker는 메시지 배치를 처리 시에 write된 배치의 마지막 메시지의 Sequence에 +1만큼 더한 값이 아닌 배치 메시지가 올 경우 `OutOfOrderSequenceException`을 생성하며 Producer에 오류를 전달한다.

#### Partitioner 이해

```java
@Component
public class CustomPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (keyBytes == null) {
            // 키가 없는 경우, 라운드 로빈 방식으로 파티션 선택 (DefaultPartitioner 동작 유사)
            return Utils.toPositive(Utils.murmur2(valueBytes)) % numPartitions;
        }

        if ("special-key".equals(key)) {
            return numPartitions - 1;
        }

        // 그 외의 모든 메시지는 키의 해시값을 기반으로 파티션을 결정합니다. (DefaultPartitioner 동작)
        return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
    }

    @Override
    public void close() {
        // 리소스 정리
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 설정 초기화
    }
}
```

* `KafkaProducer`는 기본적으로 `DefaultPartitioner` 클래스를 이용해 메시지 전송 시 도착할 `Partitioner`을 지정한다.
* `DefaultPartitioner`는 키를 가지는 메시지의 경우라면 키 값을 해싱하여 키 값에 따라 파티션별로 균일하게 전송한다.
* Custom Partitioning을 구현하려면 `Partitioner` 인터페이스를 직접 구현해야 하며 `partition()` 메서드에 Custom Partitioning 로직을 직접 구현해야 한다.
