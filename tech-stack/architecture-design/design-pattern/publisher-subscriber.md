# 발행-구독 (Publisher-Subscriber)

> 발행자와 구독자 사이에 **브로커를 세워서**, 둘이 서로를 모른 채 통신하게 만드는 패턴.

주문 이벤트를 예제로 구현했다.

![발행-구독 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/pub-sub-pattern.png)

> **GoF 패턴이 아니다.** 옵저버는 GoF 23개 중 하나지만, 발행-구독은 메시징 아키텍처에서 자라난 패턴이다. 구조가 비슷해 자주 같은 것으로 취급되지만 **결정적인 차이가 하나 있다.** 이 문서는 그 차이를 중심으로 쓴다. → [옵저버 패턴](observer.md)

---

## 1. 왜 필요한가

[옵저버 패턴](observer.md)으로 이미 `Order`와 알림을 떼어냈다. 그런데 `Order`에는 여전히 이런 필드가 남아 있다.

```java
public class Order implements Subject {
    private final List<OrderObserver> observers = new ArrayList<>();   // ← 이것
}
```

`Order`는 구독자가 누구인지는 모르지만, **구독자가 존재한다는 사실은 안다.** 목록을 자기가 들고 있으니 당연하다. 여기서 세 가지가 따라온다.

- **`OrderObserver` 타입에 의존한다.** 알림 규격이 바뀌면 `Order`도 영향을 받는다
- **주문마다 목록이 따로다.** `Order` 인스턴스가 100개면 구독 등록도 100번이다
- **같은 프로세스 안에서만 된다.** 목록이 자바 객체 참조이므로 프로세스 밖으로 나갈 수 없다

발행-구독은 **그 목록마저 밖으로 내보낸다.**

```java
public class OrderService {
    private final MessageBroker broker;        // 구독자 목록이 없다

    public void pay(String orderId, int amount) {
        broker.publish(new Message("order.paid", orderId));
    }
}
```

`OrderService`가 아는 것은 **브로커와 토픽 문자열**이 전부다. `Subscriber` 타입조차 등장하지 않는다. 구독자가 셋인지 하나도 없는지 알 방법이 없고, 알 필요도 없다.

---

## 2. 구성 요소

| 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Publisher** | 사건을 발행한다. 받는 쪽을 모른다 | `OrderService` |
| **Broker** | 구독을 관리하고 라우팅한다 | `MessageBroker` |
| **Subscriber** | 관심 있는 토픽을 구독한다 | `EmailSubscriber` · `InventorySubscriber` · `AuditLogSubscriber` |
| **Message** | 토픽 + 내용 | `Message` |
| **Client** | 누가 무엇을 구독할지 조립 | `Main` |

옵저버와 참여자 수는 비슷한데 **하나가 추가됐다. 브로커다.** 그리고 그 하나가 패턴 전체를 바꾼다.

### Broker — 이 패턴의 주인공

```java
public class MessageBroker {

    private final Map<String, List<Subscriber>> subscriptions = new LinkedHashMap<>();

    public void subscribe(String topicPattern, Subscriber subscriber) {
        subscriptions.computeIfAbsent(topicPattern, key -> new ArrayList<>()).add(subscriber);
    }

    public void publish(Message message) {
        for (Subscriber subscriber : routeOf(message.topic())) {
            try {
                subscriber.onMessage(message);
            } catch (RuntimeException e) {
                // 구독자의 실패는 여기서 끊는다. 발행자에게 가지 않는다.
            }
        }
    }
}
```

브로커가 하는 일은 셋이다.

- **구독 관리** — 누가 어떤 토픽을 듣는지 기억한다
- **라우팅** — 발행된 토픽에 맞는 구독자를 골라낸다
- **격리** — 구독자 하나의 실패가 다른 구독자나 발행자에게 번지지 않게 막는다

옵저버 패턴에서는 이 세 가지를 **Subject가 직접** 했다. 그걸 통째로 떼어내 별도 객체로 만든 것이 브로커다.

### `publish()`의 반환 타입이 `void`인 것

사소해 보이지만 이게 패턴의 선언이다.

```java
public void publish(Message message) { ... }     // 구독자 수도, 성공 여부도 안 돌려준다
```

구독자 수를 돌려주는 순간 발행자가 그 값을 보기 시작하고, "0이면 어떡하지"를 신경 쓰게 된다. 그 순간 **분리한 의미가 사라진다.** 발행자는 "이런 일이 있었다"고 말할 뿐, 그 뒤는 관여하지 않는다.

### 토픽 기반 라우팅

옵저버에서는 모든 옵저버가 모든 통지를 받고 각자 `switch`로 걸렀다. 여기서는 **브로커가 미리 거른다.**

```java
broker.subscribe("order.paid", email);
broker.subscribe("order.paid", inventory);
broker.subscribe("order.cancelled", email);
broker.subscribe("order.*", audit);            // 와일드카드
```

| 토픽 | 받는 구독자 |
|---|---|
| `order.paid` | 메일, 재고, 감사 |
| `order.cancelled` | 메일, 감사 |
| `order.refunded` (나중에 생겨도) | 감사 |

`InventorySubscriber`는 취소 메시지를 **아예 받지 않는다.** 관심 없는 메시지를 걸러내는 코드가 구독자 쪽에 없어도 된다.

그리고 이 표가 **N:M**이다. 한 토픽에 구독자 여럿, 한 구독자가 토픽 여럿. 옵저버의 1:N과 다른 지점이다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out pub_sub.code.Main
```

```
--- 0. 구독 등록 ---
  [브로커] 구독 등록 'order.paid' ← EmailSubscriber
  [브로커] 구독 등록 'order.paid' ← InventorySubscriber
  [브로커] 구독 등록 'order.cancelled' ← EmailSubscriber
  [브로커] 구독 등록 'order.*' ← AuditLogSubscriber

--- 1. 한 번의 발행이 여러 곳으로 라우팅된다 ---
주문 ORD-1001 결제 완료 (25,000원)
  [브로커] 'order.paid' 라우팅 → 구독자 3곳
    [메일] order.paid 안내 발송 (ORD-1001)
    [재고] ORD-1001 상품 차감
    [감사] order.paid / ORD-1001

--- 2. 구독자가 없어도 발행은 성공한다 ---
  [브로커] 'payment.failed' 라우팅 → 구독자 0곳
발행자는 아무 일도 안 일어났다는 사실조차 모른다

--- 3. 구독자 하나가 실패해도 브로커가 끊는다 ---
주문 ORD-1001 취소
  [브로커] 'order.cancelled' 라우팅 → 구독자 2곳
  [브로커] EmailSubscriber 처리 실패 — 메일 서버 응답 없음 (발행자는 이 사실을 모른다)
    [감사] order.cancelled / ORD-1001

--- 4. 실행 중에 구독을 뗀다 ---
  [브로커] 구독 해지 'order.*' ← AuditLogSubscriber
주문 ORD-1002 결제 완료 (40,000원)
  [브로커] 'order.paid' 라우팅 → 구독자 2곳
    [메일] order.paid 안내 발송 (ORD-1002)
    [재고] ORD-1002 상품 차감
```

**2번이 이 패턴의 성격을 가장 잘 보여 준다.** 아무도 듣지 않는 토픽에 발행했는데 아무 일도 일어나지 않고, 발행자는 그 사실조차 모른 채 정상 종료한다. 옵저버였다면 최소한 "목록이 비어 있다"는 걸 Subject가 알 수 있었다.

---

## 4. 옵저버 패턴과의 차이

| | 옵저버 | 발행-구독 |
|---|---|---|
| **목록의 주인** | Subject 자신 | **브로커** |
| 발행자가 아는 것 | `Observer` 인터페이스 + 목록 | 브로커 + 토픽 문자열 |
| 구독자가 아는 것 | Subject (등록하려면 필요) | 브로커 + 토픽 |
| 관계 | 1 : N | **N : M** |
| 라우팅 | 없다 — 전부에게 | 토픽으로 골라서 |
| 실행 위치 | 같은 프로세스 | 프로세스 밖도 가능 |
| 시점 | 대개 동기 | 대개 비동기 |
| 전달 보장 | 메서드 호출이므로 확실 | 브로커 정책에 따름 |
| 실패가 보이는 곳 | Subject가 본다 | 브로커에서 끊긴다 |

한 줄로 줄이면 이렇다.

> **옵저버** — "내가 알릴 사람들의 명단을 내가 갖고 있다"
> **발행-구독** — "명단은 우체국이 갖고 있다. 나는 편지를 부칠 뿐이다"

`Order`가 `List<OrderObserver>`를 갖고 있느냐 아니냐 — 그 필드 하나가 나머지 차이를 전부 만든다.

---

## 5. 알아둘 점

### 결합이 사라진 게 아니라 옮겨간다

가장 오해하기 쉬운 부분이다. 발행자와 구독자가 서로를 모르게 됐지만, 대신 **둘 다 토픽 문자열에 묶였다.**

```java
broker.publish(new Message("order.paid", orderId));   // 발행자
broker.subscribe("order.paid", inventory);            // 구독자
```

이 두 문자열이 어긋나면 **아무 일도 일어나지 않는다.** 그런데 컴파일러는 아무 말도 안 해 준다.

```java
broker.subscribe("order.payed", inventory);   // 오타. 컴파일 성공. 재고가 조용히 안 깎인다
```

옵저버였다면 `register()`에 잘못된 타입을 넣을 수 없었다. **타입 검사를 문자열 매칭과 맞바꾼 것이다.** 그래서 실무에서는 토픽을 상수나 enum으로 관리하고, 이벤트 클래스 타입 자체를 토픽으로 쓰기도 한다(스프링 방식).

### 발행자는 결과를 알 수 없다

```java
broker.publish(new Message("order.paid", orderId));
// 이 줄 다음에서 알 수 있는 것: 없다
```

몇 곳이 받았는지, 처리에 성공했는지, 아무것도 모른다. **이건 버그가 아니라 설계다.** 그래서 판단 기준이 하나 나온다.

> **발행자가 결과를 알아야 하는 일이면 이 패턴이 아니다.**

"결제가 끝나면 재고를 반드시 깎아야 하고, 못 깎으면 결제를 취소해야 한다" — 이건 하나의 트랜잭션이지 발행-구독이 아니다. 발행-구독으로 뺄 수 있는 건 **실패해도 주 업무가 성립하는 일**이다. 메일, 로그, 통계, 추천 갱신 같은 것들.

### 이 예제의 브로커는 진짜 브로커가 아니다

인메모리·동기·단일 프로세스다. 학습용으로 일부러 그렇게 만들었다. 실제 브로커(Kafka, RabbitMQ, Redis Pub/Sub)와 다른 점은 이렇다.

| | 이 예제 | 실제 브로커 |
|---|---|---|
| 위치 | 같은 JVM 안 | 별도 프로세스·서버 |
| 시점 | 동기 — `publish()`가 전달까지 마친다 | 비동기 — 발행하면 바로 반환 |
| 저장 | 없음 | 디스크에 남는다 (Kafka) |
| 구독자가 죽어 있으면 | 그냥 못 받는다 | 살아나면 밀린 걸 받는다 |
| 전달 보장 | 없음 | at-least-once / exactly-once 등 선택 |
| 재시도·DLQ | 없음 | 있다 |

**시점이 갈리는 순간 난이도가 달라진다.** 동기일 때는 예외를 잡으면 그만이지만, 비동기가 되면 순서 뒤바뀜·중복 수신·유실을 전부 설계해야 한다. 그래서 실무의 구독자는 대개 **멱등(idempotent)** 하게 짠다. 같은 메시지를 두 번 받아도 결과가 같도록.

### 스프링의 이벤트는 어느 쪽인가

```java
publisher.publishEvent(new OrderPaidEvent(orderId));

@EventListener
public void on(OrderPaidEvent event) { ... }
```

**구조는 발행-구독이다.** `ApplicationEventMulticaster`가 브로커 역할을 하고, 발행자는 리스너 목록을 갖지 않는다. 토픽 대신 **이벤트 타입**으로 라우팅하므로 위의 "문자열 오타" 문제도 없다.

다만 성격은 이 예제와 같다. **같은 프로세스, 기본 동기**다. 프로세스 밖으로 나가려면 Kafka 같은 진짜 브로커가 필요하고, 그때부터 위 표의 오른쪽 열이 전부 숙제가 된다.

한 가지 더 — 스프링은 리스너 예외를 **기본적으로 발행자에게 전파한다.** 이 예제의 브로커가 `try/catch`로 끊는 것과 다르다. 격리하려면 멀티캐스터에 `errorHandler`를 지정해야 한다. "발행-구독이니까 알아서 격리되겠지"가 통하지 않는 지점이다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 비동기 전달 (`ExecutorService`) 과 그에 따르는 순서·중복·유실 처리
- 토픽 상수화 — `"order.paid"` 대신 `Topics.ORDER_PAID`
- 스레드 안전 — `subscriptions`를 `ConcurrentHashMap` + `CopyOnWriteArrayList`로
- 재시도와 DLQ(처리 실패한 메시지를 모아 두는 큐)
- 구독자별 멱등 처리 (중복 수신 대비)
- 메시지 직렬화 — 프로세스 밖으로 나가려면 payload가 문자열이 아니라 JSON·바이트가 된다

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 사건 하나에 반응할 곳이 많고, **누가 반응하는지 발행자가 몰라도 될 때**
- 반응이 실패해도 **주 업무가 성립할 때** (알림, 로그, 통계)
- 발행자와 구독자의 **배포 주기가 다를 때** — 새 구독자를 붙이는 데 발행자를 건드리지 않는다
- 나중에 프로세스 밖으로 꺼낼 가능성이 있을 때 (같은 코드 모양을 유지한 채 브로커만 교체)

**안 쓴다**
- 반응이 **하나뿐**일 때 — 브로커를 세울 이유가 없다. 그냥 호출한다
- 발행자가 **결과를 알아야** 할 때 — `void`가 답이 아니면 이 패턴이 아니다
- **순서가 중요할 때** — 브로커는 순서를 보장하지 않는다
- **전부 성공하거나 전부 실패해야** 할 때 — 트랜잭션으로 묶을 일이다
- 참여자가 두셋뿐이고 늘 일이 없을 때 — 옵저버로 충분하다. 브로커는 그 자체로 관리 대상이다

판단은 순서대로 하면 된다. **반응할 곳이 하나인가?** → 직접 호출. **여럿인데 발행자가 목록을 알아도 되는가?** → 옵저버. **그것마저 몰라야 하는가?** → 발행-구독.
