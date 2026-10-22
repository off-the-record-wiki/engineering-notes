# Apache Kafka - Idempotent Producer & Transactional Producer & Transactional Consumer

#### 멱등성 프로듀서

* 멱등성이란 여러 번 연산을 수행하더라도 동일한 결과를 나타내는 것을 뜻한다. 수학적으로 `f(f(x)) = f(x)`의 개념이다.
* 이런 의미에서 멱등성 프로듀서는 동일한 데이터를 여러 번 전송하더라도 카프카 클러스터에 단 한 번만 저장됨을 의미한다. 기본 프로듀서의 동작 방식은 적어도 한 번 전달(at least once delivery)을 지원한다.
* 적어도 한 번 전달이란 프로듀서가 클러스터에 데이터를 전송해 저장할 때 적어도 한 번 이상 데이터를 적재할 수 있고 데이터가 유실되지 않았음을 뜻한다. 다만, 두 번 이상 적재할 가능성이 있으므로 데이터 중복이 발생할 수 있다.
* **`At least once`** : 적어도 한 번 이상 전달
* **`At most once`** : 최대 한 번 전달
* **`Exactly once`** : 정확히 한 번 전달
* 프로듀서가 보내는 데이터 중복 적재를 막기 위해 0.11.0 버전 이후부터는 프로듀서에서 `enable.idempotence` 옵션을 사용하여 정확히 한 번 전달을 지원하고 있다.
* **`enable.idempotence`** 옵션을 사용하여 정확히 한 번 전달을 지원한다.
* 카프카 3.0.0부터는 **`enable.idempotence`** 옵션값의 기본 값이 true로 변경되므로 신규 버전에서는 프로듀서 동작에 유의해야 한다.

```java
Properties configs = new Properties();
configs.put(BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
configs.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
configs.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
configs.put(ENABLE_IDEMPOTENCE_CONFIG, true);

KafkaProducer<String, String> producer = new KafkaProducer<>(configs);
```

#### 멱등성 프로듀서인 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/de0a7cb6-9394-4137-baaf-b7bd89d65fc8.png)

* 멱등성 프로듀서는 기본 프로듀서와 달리 데이터를 브로커에 전달할 때 프로듀서 PID(Producer unique ID)와 시퀀스 넘버를 함께 전달한다.
* 그러면 브로커는 프로듀서 PID와 시퀀스 넘버를 확인해 동일한 메시지의 적재 요청이 오더라도 단 한 번만 데이터를 적재함으로써 프로듀서의 데이터는 정확히 한 번 브로커에 적재되도록 동작한다.
* **PID(Producer unique ID)** : 프로듀서 고유 ID
* **SID(Sequence ID)** : 레코드 전달 번호 ID

#### 멱등성 프로듀서가 아닌 경우

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/ca6b7cbd-fd76-460a-97db-2b229d8ad277.png)

* 브로커가 `seq` 번호없이 메시지를 수신 → 중복 식별이 불가능해진다.
* ACK 유실 후 재전송 시 동일 메시지가 파티션에 2번 기록이 되고 이로 인해 중복 처리가 발생한다.

#### 멱등성 프로듀서의 한계점

* 멱등성 프로듀서는 동일한 세션에서만 정확히 한 번 전달을 보장한다. 여기서 동일한 세션이란 PID의 생명주기를 말한다.
* 멱등성 프로듀서로 동작하는 프로듀서 애플리케이션에 이슈가 발생해 종료되고 애플리케이션을 재시작하면 PID가 달라지게 된다.
* 동일한 데이터를 보내더라도 PID가 달라지면 브로커 입장에서는 다른 프로듀서 애플리케이션에서 데이터를 보냈다고 판단하기 때문에 멱등성 프로듀서는 장애가 발생하지 않는 경우에만 정확히 한 번 적재하는 것을 보장한다는 점을 인지해야 한다.

#### 멱등성 프로듀서로 설정할 경우 옵션

* 멱등성 프로듀서를 사용하기 위해 **`enable.idempotence`**를 `true`로 설정하면 정확히 한 번 적재하는 로직이 성립되기 위해 프로듀서 일부 옵션들이 강제로 설정된다.
* 프로듀서 데이터 재전송 횟수를 정하는 `retries`는 기본 값으로 `Integer.MAX_VALUE`로 설정되고 `acks` 옵션은 -1로 설정된다.
* 이렇게 설정되는 이유는 프로듀서가 적어도 한 번 이상 브로커에 데이터를 보냄으로써 브로커에 단 한 번만 데이터가 적재되는 것을 보장하기 위해서이다.
* 멱등성 프로듀서는 정확히 한 번 데이터를 브로커에 적재하기 위해 정말로 한 번 전송하는 것이 아니다. 상황에 따라 프로듀서가 여러 번 전송하되 브로커가 여러 번 전송된 데이터를 확인하고 중복된 데이터는 적재하지 않는 것이다.

#### 멱등성 프로듀서 사용 시 오류

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/e30e67b5-b8f7-4a84-b8ab-85faa53c8b02.png)

* 멱등성 프로듀서의 SID는 0부터 시작해 숫자를 1씩 더한 값이 전달된다.
* 브로커에 멱등성 프로듀서가 전송한 데이터의 PID와 SID를 확인하는 과정에서 SID가 일정하지 않은 경우에는 `OutOfOrderSequenceException`이 발생할 수 있다.
* 이 오류는 브로커가 예상한 SID와 다른 번호 데이터의 적재 요청이 왔을 때 발생한다.
* `OutOfOrderSequenceException`이 발생했을 경우에는 SID 역전 현상이 발생할 수 있기 때문에 순서가 중요한 데이터를 전송하는 프로듀서는 해당 Exception이 발생할 경우 대응 방안을 고려해야 한다.

#### 트랜잭션 프로듀서 동작

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/f7bd2aa2-5959-48d7-9fe4-0c1e20edfd36.png)

* 카프카에서 트랜잭션은 다수의 파티션에 데이터를 저장할 경우 모든 데이터에 대해 동일한 원자성을 보장하기 위해 사용된다.
* 원자성을 만족시킨다는 의미는 다수의 데이터를 동일 트랜잭션으로 묶어 전체 데이터를 일관성있게 처리하는 것을 의미한다.
* 트랜잭션 프로듀서는 사용자가 보낸 데이터를 레코드로 파티션에 저장할 뿐만 아니라 트랜잭션의 시작과 끝을 표현하기 위해 트랜잭션 레코드를 한 개 더 보낸다.

#### 트랜잭션 프로듀서 설정

```java
Properties configs = new Properties();
// ...
configs.put(TRANSACTIONAL_ID_CONFIG, UUID.randomUUID());  // 이와 같이 transactional.id를 작성해야 한다. 프로듀서별로 고유한 ID 값을 사용해야하기 때문이다.

KafkaProducer<String, String> producer = new KafkaProducer<>(configs);

////////////////////////// 트랜잭션 범위
producer.initTransactions();            // 트랜잭션 초기화
producer.beginTransaction();            // 트랜잭션 시작
producer.send(new ProducerRecord<>(TOPIC, "전달하고자하는 메시지 값");
producer.commitTransaction();           // 트랜잭션 커밋
//////////////////////////

producer.flush();
producer.close();
```

#### 트랜잭션 컨슈머 동작

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/21d0aad0-b454-4d6b-aca4-09e1a19362df.png)

* 트랜잭션 컨슈머는 파티션에 저장된 트랜잭션 레코드를 보고 트랜잭션이 완료(즉, 커밋)됨을 확인하고 데이터를 가져간다.
* 트랜잭션 레코드는 실질적인 데이터를 가지고 있지 않으며 트랜잭션이 끝난 상태를 표시하는 정보만 가지고 있다.

#### 트랜잭션 컨슈머 설정

* 트랜잭션 컨슈머는 커밋이 완료된 레코드들만 읽기 위해 `isolation.level` 옵션을 `read_committed` 옵션으로 설정해야 한다.
* 기본 값은 `read_uncommitted`로서 트랜잭션 프로듀서가 레코드를 보낸 후 커밋 여부와 상관없이 모두 읽는다.
* `read_committed`로 설정한 컨슈머는 커밋이 완료된 레코드들만 읽어 처리한다.

```java
configs.put(ISOLATION_LEVEL_CONFIG, "read_committed");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(configs);
```
