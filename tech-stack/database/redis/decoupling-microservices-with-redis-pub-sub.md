# Redis - Decoupling microservices with Redis Pub/Sub

#### Loose Coupling을 위한 Redis Pub/Sub Messaging Pattern

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/312d9264-bfe5-444c-b17a-98b79638849d.png)

**1. 구성 요소의 역할**

* Publisher : 메시지를 보내는 주체. 특정 채널(Channel)에 메시지를 던지기만 할 뿐, 누가 이 메시지를 받는지는 전혀 신경쓰지 않는다.
* Channel : 메시지가 전달되는 통로. 발행자는 채널에 메시지를 넣고 구독자는 채널을 듣게 된다.
  * 채널은 미리 생성하는 개념이 아니다. `PUBLISH` 하는 순간 이름만으로 성립하며, 구독자가 없으면 메시지는 그대로 버려진다.
* Subscriber : 특정 채널을 지켜보고 있다가 메시지가 들어오면 즉시 수신하는 주체. 여러 구독자가 하나의 채널을 동시에 구독할 수 있다.

**2. 핵심 메커니즘 : 서로 모름(Decoupling)**

* 발행자는 구독자를 모른다
* 구독자는 발행자를 모른다
* 그래서 구독자를 추가·제거해도 발행자 코드는 바뀌지 않는다. 마이크로서비스 간 결합을 끊는 것이 이 패턴의 목적이다.

**3. 실시간 전달의 특징(Fire and Forget)**

* Push 방식 : 구독자가 데이터를 달라고 요청하는 것이 아니라 Redis 서버가 구독자에게 데이터를 밀어넣는 방식으로 매우 빠르며 실시간성이 높다.
* 비영속성(휘발성) : Redis의 Pub/Sub은 메시지를 저장하지 않는다. 발행자가 메시지를 보낸 순간 구독자가 오프라인 상태라면 그 메시지는 영원히 사라진다.
  * 구독자가 잠깐 재시작하는 사이에 발행된 메시지도 마찬가지로 사라진다. **재연결 후 놓친 메시지를 따라잡는 방법이 없다.**
* 만약 메시지 보관이 필요하다면 Redis의 List나 Stream 자료구조를 사용해야 한다.

#### Redis Pub/Sub - 구독과 발행 명령어

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/429cffc5-a1cd-4152-b503-c576dd6b302b.png)

**1. 일반 구독(SUBSCRIBE) : 정확한 타겟팅**

* 동작 : 클라이언트가 정확한 채널 이름을 지정해 구독한다.
* `SUBSCRIBE chat.room.1`이라고 명령하면 정확히 `chat.room.1`이라는 채널로 발행된 메시지만 수신한다.

**2. 패턴 구독(PSUBSCRIBE) : 광범위한 수집**

* 동작 : 와일드카드를 사용하여 여러 채널을 동시에 구독하는 방식이다.
* `PSUBSCRIBE chat.room.*`이라고 명령하면 `chat.room.1`, `chat.room.2` 등 해당 패턴에 매칭되는 모든 채널의 메시지를 한꺼번에 수신한다.
* 같은 클라이언트가 `chat.room.1`을 일반 구독하면서 `chat.room.*`도 패턴 구독하고 있다면 **같은 메시지를 두 번 받는다.** 중복 수신을 막으려면 둘 중 하나만 쓰거나 애플리케이션에서 걸러야 한다.

**3. 메시지 발행(PUBLISH) : 실시간 브로드캐스팅**

* 동작 : 특정 채널명과 메시지 내용을 함께 보낸다.
* Redis 서버는 해당 채널을 일반 구독 중인 클라이언트를 찾아서 메시지를 보낸다.
* 동시에 해당 채널명과 매칭되는 패턴 구독 중인 클라이언트도 찾아 메시지를 보낸다.
* 이 과정은 메모리 상에서 즉시 일어난다.
* **반환값은 메시지를 받은 구독자 수**다. 0이 반환되면 아무도 듣고 있지 않았다는 뜻이며, 이 경우 메시지는 그대로 사라진다.

**4. 알아두면 좋은 제약과 도구**

* 구독 중인 클라이언트는 **제한된 명령만 실행할 수 있다.** RESP2 프로토콜에서는 `SUBSCRIBE` / `UNSUBSCRIBE` / `PSUBSCRIBE` / `PUNSUBSCRIBE` / `PING` / `QUIT`만 허용되므로, **구독 전용 커넥션을 따로 두어야 한다.**(RESP3에서는 이 제약이 없다)
* 현재 상태를 확인하는 내성(introspection) 명령
  * `PUBSUB CHANNELS [pattern]` : 구독자가 하나라도 있는 활성 채널 목록
  * `PUBSUB NUMSUB channel [channel ...]` : 채널별 구독자 수
  * `PUBSUB NUMPAT` : 등록된 패턴 개수

#### Redis Pub/Sub - 내부 자료구조

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/fd269e63-02b9-426b-b234-79574a1f6357.png)

**1. pubsub_channels(Dictionary 구조)**

* Redis는 내부적으로 해시 테이블을 사용한다.
* 동작 : `PUBLISH news "Hello"` 명령이 들어오면, Redis는 딕셔너리에서 `news`라는 키를 O(1)의 속도로 즉시 찾고 그 후 연결된 리스트를 순회하며 대기 중인 구독자들에게 메시지를 쏴준다.
* 채널 이름을 정확히 알고 있을 때 매우 빠르게 대상자를 찾을 수 있다.

**2. pubsub_patterns(List 구조)**

* 딕셔너리와 달리 리스트 형태로 관리된다.
* 동작 : 메시지가 특정 채널로 발행되면 Redis는 pubsub_patterns 리스트를 처음부터 끝까지 O(N)의 시간복잡도로 순회해 각 패턴이 현재 발행된 채널명과 매칭되는지 일일이 검사해 일치하는 경우 해당 구독자에게 메시지를 보낸다.
* 와일드카드 매칭이 필요하기 때문에 해시 테이블 사용이 어렵고 리스트 전체 스캔이 불가피하다.

**3. PUBLISH 한 번의 총 비용**

* 정리하면 `PUBLISH`의 시간 복잡도는 **O(N + M)** 이다.
  * N : 해당 채널을 구독 중인 클라이언트 수(메시지를 실제로 보내야 하는 대상)
  * M : 서버에 등록된 **전체 패턴 개수**(하나하나 매칭 검사)
* 즉 **패턴 구독은 쓰는 쪽이 아니라 발행하는 쪽에 비용을 지운다.** 패턴이 많아질수록 모든 `PUBLISH`가 함께 느려지므로, 채널 이름 설계로 해결할 수 있다면 일반 구독을 쓰는 편이 낫다.
* Redis는 싱글 스레드이므로, 구독자가 매우 많은 채널에 대량 발행을 하면 그 시간 동안 다른 요청이 대기한다.

#### Redis Pub/Sub - 메시지 전달 보장 수준

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/cde3093a-02a0-41cb-acce-5291df69b63e.png)

**1. At-Most-Once(최대 1번 전달)**

* 동작 : 메시지를 보내고 나면 성공 여부를 확인하지 않는다.(=Fire and Forget)
* 특징 : 네트워크 장애가 나면 메시지가 유실될 순 있지만 절대 중복되진 않는다.
* 처리 속도가 빠르고 시스템 부하가 적다. 데이터가 조금 누락되어도 전체 흐름에 큰 지장이 없는 경우에 적합하다.

**2. At-Least-Once(최소 1번 전달)**

* 동작 : 메시지를 보낸 후 수신자로부터 ACK를 기다린다. 일정 시간 동안 ACK가 오지 않으면 메시지를 다시 보낸다.
* 특징 : 메시지 유실은 없지만 네트워크 지연 등으로 인해 이미 받은 메시지를 또 받는 중복 문제가 발생할 수 있다.
* 데이터 손실을 방지할 수 있어 신뢰도가 높다. 단 수신 측에서 멱등성(Idempotency)을 보장하도록 설계해야 한다.(=똑같은 메시지 2번 받아도 결과가 같아야 함)

**3. Exactly-Once(정확히 1번 전달)**

* 동작 : 메시지 전송과 수신, 그리고 처리를 하나의 트랜잭션으로 묶거나 시스템적으로 중복을 원천 차단한다.
* 특징 : 유실도 중복도 없다. 사용자에게 완벽한 신뢰를 제공한다.
* 구현 난이도가 매우 높고 성능이 다른 방식에 비해 상대적으로 떨어질 수 있다.

> ❗**Redis Pub/Sub이 제공하는 것은 세 가지 중 At-Most-Once 하나뿐이다.**
>
> ACK도, 재전송도, 저장도 없기 때문에 구조적으로 그 이상을 보장할 수 없다.<br>
> 유실이 허용되지 않는다면 Pub/Sub이 아니라 다른 수단을 선택해야 한다.<br>
> * **Redis Streams** : 메시지를 저장하고, 컨슈머 그룹·ACK·재처리(`XPENDING`/`XCLAIM`)를 지원한다. Redis 안에서 At-Least-Once가 필요할 때의 답이다.<br>
> * **Kafka·RabbitMQ 등 전용 메시지 브로커** : 대규모 처리량과 장기 보관, 정교한 재처리가 필요할 때.<br>

* **느린 구독자(Slow Consumer) 문제** : 구독자가 메시지를 받아 처리하는 속도보다 발행 속도가 빠르면 서버 측 출력 버퍼가 쌓인다. `client-output-buffer-limit pubsub` 한도를 넘으면 **Redis가 해당 클라이언트의 연결을 강제로 끊는다.** 이 순간에도 메시지는 유실되며, 이것이 실무에서 Pub/Sub 메시지가 사라지는 흔한 원인이다.

#### Redis Pub/Sub - 클러스터 환경에서의 동작

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/743514a6-c035-4303-a789-5ade70da4f10.png)

**1. 동작 흐름 : 전사적 방송(Broadcasting)**

* 메시지 발행 : 특정 클라이언트가 Node A에 접속하여 메시지를 발행한다.
* 클러스터 버스 전파 : Node A는 해당 메시지를 클러스터 내부 전용 통신망인 클러스터 버스에 태운다.
* 전체 노드 수신 : 버스를 통해 Node B, Node C를 포함한 클러스터 내의 모든 노드가 해당 메시지를 전달받는다.
* 구독자에게 전달 : 각 노드(A, B, C)는 자신에게 연결된 구독자들 중 해당 채널을 듣고 있는 유저가 있다면 즉시 메시지를 전달한다.

> 레디스 클러스터의 Pub/Sub는 어느 노드에 접속하든지간에 똑같은 결과를 얻어야 한다는 원칙이 있다. 구독자가 자신이 Node A에 붙어있든 Node C에 붙어있든 상관없이 클러스터 어디선가 발행된 메시지를 실시간으로 받을 수 있다.

**2. 이 방식의 확장성 문제**

* 채널명은 해시 슬롯과 무관하므로, 어느 노드가 그 채널의 구독자를 갖고 있는지 알 수 없다. 그래서 **모든 노드에 무조건 브로드캐스트**한다.
* 결과적으로 구독자가 한 명도 없는 노드까지 메시지를 받게 되고, **노드 수가 늘어날수록 클러스터 버스 트래픽이 함께 늘어난다.** 대규모 클러스터에서 Pub/Sub을 많이 쓰면 버스가 병목이 된다.

**3. 해결책 : Sharded Pub/Sub (Redis 7.0+)**

* `SPUBLISH` / `SSUBSCRIBE` / `SUNSUBSCRIBE` 명령이 추가되었다.
* 채널명을 **일반 키처럼 해시 슬롯에 매핑**해서, 그 슬롯을 담당하는 노드(와 그 복제본)에만 메시지를 전달한다. 전체 브로드캐스트가 사라지므로 노드를 늘려도 부하가 비례해 늘지 않는다.
* 대신 **구독자가 해당 슬롯을 담당하는 노드에 접속해 있어야 한다.** "어느 노드에 붙어도 똑같이 받는다"는 기존 성질을 포기하는 대신 확장성을 얻는 방식이다.
* 클러스터에서 Pub/Sub을 본격적으로 쓴다면 기본 Pub/Sub보다 Sharded Pub/Sub을 우선 검토하는 것이 좋다.

#### Redis Pub/Sub Messaging - 구독자 설정(Spring)

```java
@Bean
public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                   NotificationSubscriber notificationSubscriber,
                                                                   NewsPatternSubscriber newsPatternSubscriber) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(notificationSubscriber, new ChannelTopic("test:notification"));
    container.addMessageListener(newsPatternSubscriber, new PatternTopic("news:*"));
    return container;
}
```

* `RedisMessageListenerContainer`는 **구독 전용 커넥션을 하나 점유**한다. 앞서 설명한 대로 구독 상태의 커넥션으로는 일반 명령을 보낼 수 없기 때문이며, 일반 조회·저장용 `RedisTemplate`과는 커넥션이 분리된다.
* 별도 설정이 없으면 메시지를 처리할 스레드를 컨테이너가 알아서 만든다. 처리 로직이 무겁다면 `setTaskExecutor(...)`로 스레드풀을 명시해 두는 것이 안전하다. **한 리스너가 오래 붙잡고 있으면 뒤따르는 메시지 처리가 밀리고, 심하면 앞서 말한 느린 구독자 문제로 연결이 끊긴다.**

#### Redis Pub/Sub Messaging - 리스너 구현

```java
@Component
public class NewsPatternSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(NewsPatternSubscriber.class);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String matchedPattern = pattern != null ? new String(pattern) : "unknown";
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        log.info("Pattern message received from pattern {}, channel {}: {}", matchedPattern, channel, body);
    }
}
```
```java
@Component
public class NotificationSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationSubscriber.class);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        log.info("Message received from channel {}: {}", channel, body);
    }
}
```

* 패턴 구독 리스너에서는 `pattern`(매칭된 패턴)과 `message.getChannel()`(실제 발행된 채널)이 서로 다르다. 위 예제처럼 둘을 함께 로깅해두면 디버깅에 유용하다.
* **`onMessage` 안에서 예외가 발생하면 그 메시지는 그대로 사라진다.** 재시도도, 데드레터도 없다. 처리 로직 전체를 `try-catch`로 감싸고 실패를 로그로 남겨야 최소한 사후 추적이 가능하다.

#### Redis Pub/Sub Messaging - 메시지 발행

```java
@Service
public class NotificationPublisher {

    private final StringRedisTemplate redisTemplate;

    public NotificationPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String message) {
        // 반환값은 메시지를 수신한 구독자 수 (0이면 아무도 듣고 있지 않았다는 뜻)
        redisTemplate.convertAndSend("test:notification", message);
    }
}
```

#### Redis Pub/Sub - 언제 쓰고 언제 쓰지 말아야 하는가

|적합한 경우|이유|
|:---|:---|
|실시간 채팅 · 알림 브로드캐스트|지금 접속한 사람에게만 전달하면 되고, 놓친 메시지는 의미가 없다|
|서버 간 캐시 무효화 신호|모든 인스턴스가 동시에 받아야 하고, 놓쳐도 TTL로 복구된다|
|WebSocket 서버 간 메시지 중계|여러 대의 WebSocket 서버가 같은 이벤트를 공유해야 할 때|
|설정 변경 · 기능 플래그 전파|즉시성이 중요하고 영속성은 불필요|

|부적합한 경우|대안|
|:---|:---|
|주문·결제 등 유실되면 안 되는 이벤트|Redis Streams · Kafka · RabbitMQ|
|구독자가 잠시 꺼져도 나중에 처리해야 하는 작업 큐|Redis Streams(컨슈머 그룹) · List(BLPOP)|
|메시지 이력 조회·재처리가 필요한 경우|Redis Streams · Kafka|

* 한 줄로 정리하면 — **Pub/Sub은 "지금 듣고 있는 사람에게만, 빠르게" 전달하는 도구다.** 그 이상이 필요하면 도구를 바꿔야지 Pub/Sub 위에 재전송·저장을 직접 얹는 것은 좋은 선택이 아니다.
* 참고로 Redis의 **Keyspace Notification**(키 만료·변경 이벤트 통지)도 이 Pub/Sub 위에서 동작한다. 따라서 **만료 이벤트 역시 유실될 수 있으므로**, 이를 근거로 중요한 비즈니스 로직을 처리하면 안 된다.
