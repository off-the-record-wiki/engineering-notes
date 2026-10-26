# 옵저버 패턴 (Observer Pattern)

> 한 객체의 상태가 바뀌면 **거기에 의존하는 객체들에게 자동으로 통지**되게 만드는 패턴.

주문 상태 알림을 예제로 구현했다.

![옵저버 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/observer-pattern.png)

---

## 1. 왜 필요한가

주문 상태가 바뀔 때 해야 할 일이 늘어나면 이런 코드가 생긴다.

```java
// 옵저버 패턴을 쓰지 않았을 때
public class Order {

    private final EmailNotifier emailNotifier;
    private final InventoryUpdater inventoryUpdater;
    private final SalesStatistics salesStatistics;
    // 요구사항이 늘 때마다 필드도 늘어난다

    public void changeStatus(OrderStatus next) {
        this.status = next;

        emailNotifier.send(this);          // 메일 보내고
        inventoryUpdater.update(this);     // 재고 깎고
        salesStatistics.add(this);         // 통계 쌓고
        // "슬랙 알림도 보내주세요" → 또 여기를 연다
    }
}
```

문제는 **`Order`가 알림 대상을 전부 알고 있다**는 것이다.

- 알림이 하나 늘 때마다 잘 돌아가던 `Order`를 다시 연다
- `Order`가 메일 서버, 재고 시스템, 통계 모듈에 전부 의존한다. `Order` 하나 테스트하려면 셋 다 필요하다
- 실행 중에 알림을 켜고 끌 수 없다. 컴파일 시점에 박혀 있다

옵저버 패턴은 방향을 뒤집는다. **`Order`가 대상을 아는 게 아니라, 관심 있는 쪽이 스스로 등록한다.**

```java
// 옵저버 패턴을 썼을 때 — Order는 목록에 대고 알릴 뿐이다
public void changeStatus(OrderStatus next) {
    this.status = next;
    notifyObservers(new OrderEvent(orderId, next, amount));
}
```

`Order`가 아는 것은 `OrderObserver` 목록뿐이다. **메일·재고·통계라는 단어가 `Order`에 한 번도 나오지 않는다.**

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Subject** | 등록·해제·통지의 규격 | `Subject` |
| **ConcreteSubject** | 상태를 갖고, 바뀌면 통지 | `Order` |
| **Observer** | 통지를 받는 규격 | `OrderObserver` |
| **ConcreteObserver** | 통지에 실제로 반응 | `EmailNotifier` · `InventoryUpdater` · `SalesStatistics` |
| **Client** | 누가 무엇을 들을지 조립 | `Main` |

전략 패턴과 참여자 수는 비슷하지만 **관계의 수가 다르다.** 전략은 Context 하나가 전략 하나를 들고 있고, 옵저버는 Subject 하나가 **여러 개를** 들고 있다. 그래서 "하나가 바뀌면 여럿이 반응한다"가 성립한다.

### Observer — 메서드 하나짜리 규격

```java
public interface OrderObserver {
    void onOrderChanged(OrderEvent event);
}
```

메서드가 하나뿐인 게 중요하다. Subject는 이것만 알고, 상대가 메일을 보내는지 재고를 깎는지 **모른다.** 그리고 메서드가 하나이므로 **람다도 그대로 옵저버가 된다.**

```java
order.register(event -> log.info("감사로그 {} {}", event.orderId(), event.status()));
```

### ConcreteSubject — 목록을 들고 알린다

```java
public class Order implements Subject {

    private final List<OrderObserver> observers = new ArrayList<>();

    @Override public void register(OrderObserver o)   { observers.add(o); }
    @Override public void unregister(OrderObserver o) { observers.remove(o); }

    public void changeStatus(OrderStatus next) {
        this.status = next;
        notifyObservers(new OrderEvent(orderId, next, amount));
    }
}
```

`unregister()`가 규격에 들어 있는 게 중요하다. **구독을 끊을 방법이 없으면 Subject가 살아 있는 동안 옵저버도 계속 살아남는다.** 자바에서 옵저버 패턴이 메모리 누수의 흔한 원인으로 꼽히는 이유다.

### 통지에 실리는 데이터 — push와 pull

이 예제는 **push** 방식이다. 옵저버가 필요로 할 값을 Subject가 담아서 보낸다.

```java
public record OrderEvent(String orderId, OrderStatus status, int amount) { }
```

반대로 Subject 자신을 넘기고 옵저버가 꺼내 가게 하는 방식을 **pull**이라 한다.

| | push | pull |
|---|---|---|
| 시그니처 | `onOrderChanged(OrderEvent e)` | `onOrderChanged(Order order)` |
| 옵저버가 아는 것 | 이벤트 데이터만 | **Subject 전체** |
| 결합도 | 낮다 | 높다 |
| 아쉬운 점 | 무엇을 실을지 미리 정해야 함 | 옵저버가 Subject에 묶임 |

**기본은 push다.** 옵저버가 Subject 타입을 몰라도 되므로 재사용과 테스트가 쉽다. 이벤트에 실을 항목이 계속 늘어난다면 그건 "옵저버가 너무 많은 걸 알려고 한다"는 신호일 때가 많다.

> `OrderEvent`가 `record`인 것도 의도적이다. 여러 옵저버가 **같은 객체를 받으므로**, 하나가 값을 고치면 뒤에 받는 옵저버가 다른 값을 보게 된다. 이벤트는 불변이어야 한다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out observer.code.Main
```

```
--- 1. 상태가 바뀌면 세 곳이 각자 반응한다 ---
주문 ORD-1001 상태 변경 → PAID
  [메일] PAID 안내 메일 발송 (ORD-1001)
  [재고] 상품 차감
  [통계] 누적 매출 25,000원
주문 ORD-1001 상태 변경 → SHIPPED
  [메일] SHIPPED 안내 메일 발송 (ORD-1001)
  [재고] 출고 완료 — 더 들을 일이 없다

--- 2. 옵저버 하나가 실패해도 나머지는 계속된다 ---
주문 ORD-1002 상태 변경 → PAID
  [메일] PAID 안내 메일 발송 (ORD-1002)
  [재고] 상품 차감
  [통계] 누적 매출 65,000원
주문 ORD-1002 상태 변경 → CANCELLED
  [경고] EmailNotifier 처리 실패 — 메일 서버 응답 없음 (나머지는 계속된다)
  [재고] 차감분 복구
  [통계] 누적 매출 25,000원

--- 3. 옵저버는 실행 중에 늘릴 수 있다 ---
주문 ORD-1002 상태 변경 → PAID
  [메일] PAID 안내 메일 발송 (ORD-1002)
  [재고] 상품 차감
  [통계] 누적 매출 65,000원
  [감사로그] ORD-1002 PAID
```

`changeStatus()`를 한 번 불렀을 뿐인데 세 곳이 반응한다. 그리고 **`Order` 코드는 한 줄도 바뀌지 않은 채** 감사로그가 나중에 끼어들었다.

---

## 4. 새 알림을 추가하려면

```java
public class SlackNotifier implements OrderObserver {
    @Override
    public void onOrderChanged(OrderEvent event) {
        if (event.status() == OrderStatus.CANCELLED) {
            slack.send("#cs", event.orderId() + " 취소됨");
        }
    }
}
```

```java
order.register(new SlackNotifier());   // 조립하는 곳에서 한 줄
```

**클래스 하나 추가 + 등록 한 줄이다.** `Order`도, 기존 옵저버도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

전략 패턴과 다른 점은 **기존 것을 대체하는 게 아니라 얹는다**는 것이다. 전략은 하나를 골라 바꾸고, 옵저버는 목록에 더한다.

---

## 5. 알아둘 점

### 통지 메서드에 함정이 두 개 있다

```java
@Override
public void notifyObservers(OrderEvent event) {
    for (OrderObserver observer : new ArrayList<>(observers)) {   // ①
        try {
            observer.onOrderChanged(event);
        } catch (RuntimeException e) {                            // ②
            log("경고: " + observer.getClass().getSimpleName() + " 실패");
        }
    }
}
```

**① 복사본을 순회한다.** 통지를 받은 옵저버가 그 자리에서 구독을 해지하면 원본 리스트가 바뀐다. 원본을 그대로 돌면 `ConcurrentModificationException`이 난다. 이 예제의 `InventoryUpdater`가 출고 후 스스로 `unregister()`하면서 실제로 그 상황을 만든다.

```java
case SHIPPED -> {
    System.out.println("  [재고] 출고 완료");
    subject.unregister(this);          // 통지 도중에 목록을 건드린다
}
```

**② 옵저버마다 예외를 가둔다.** 하나가 터졌다고 나머지가 통지를 못 받으면 안 된다. 옵저버들은 서로 모르는 사이이고, **서로의 실패에 영향받을 이유가 없다.** 실행 결과에서 메일이 터졌는데도 재고 복구와 통계 정정이 정상적으로 이뤄진 게 이것이다.

> 다만 예외를 삼키는 게 항상 옳지는 않다. "재고 차감이 실패했는데 주문은 성공"이 말이 안 되는 도메인이라면, 그건 옵저버로 뺄 일이 아니라 트랜잭션 안에서 처리할 일이다. **실패해도 되는 일만 옵저버로 뺀다.**

### 통지 순서에 기대면 안 된다

출력에서 메일 → 재고 → 통계 순으로 나오는 건 등록 순서 때문이지 **보장된 규칙이 아니다.** 등록 순서를 바꾸거나 나중에 비동기로 돌리면 그대로 깨진다.

옵저버 B가 "옵저버 A가 먼저 처리했을 것"을 전제로 동작하는 순간, 그 둘은 사실 하나의 절차다. **순서가 중요하다면 옵저버로 나눌 게 아니라 한 곳에 순서대로 쓰는 게 맞다.**

### 동기 통지의 함정

이 예제의 통지는 **같은 스레드에서 순서대로** 일어난다. 옵저버 하나가 3초 걸리면 `changeStatus()`도 3초 걸린다. 주문 처리가 알림 때문에 느려지는 것이다.

실무에서 이 문제는 **통지와 처리를 분리**해서 푼다. 큐에 넣고 반환하거나, 스프링이라면 `@Async` / `@TransactionalEventListener`를 쓴다. 다만 그 순간 "실패하면 어떻게 되는가"를 다시 설계해야 한다. 동기일 때는 예외를 잡으면 그만이지만, 비동기는 재시도와 유실 처리가 따라온다.

### `java.util.Observer`는 쓰지 않는다

자바에 내장된 `java.util.Observable` / `Observer`는 **Java 9에서 deprecated 됐다.**

- `Observable`이 **인터페이스가 아니라 클래스**다. 상속을 강요하므로 이미 부모가 있는 클래스는 Subject가 될 수 없다
- 통지 데이터가 `Object`라 타입 안전하지 않다
- 통지 순서와 스레드 안전성이 명세에 없다

**직접 인터페이스를 정의하는 편이 낫다.** 이 예제처럼 20줄이면 끝난다.

### 스프링을 쓴다면 이미 들어 있다

```java
@Component
public class EmailNotifier {

    @EventListener
    public void on(OrderEvent event) { ... }   // 등록 코드가 없다
}
```

```java
publisher.publishEvent(new OrderEvent(orderId, next, amount));
```

`ApplicationEventPublisher`가 Subject고, `@EventListener`가 등록이다. **컨테이너가 옵저버 목록을 대신 관리한다.** 그래도 패턴을 알아야 하는 이유는, 순서·예외·동기 여부 같은 위의 질문이 스프링을 쓴다고 사라지지 않기 때문이다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 스레드 안전 — `observers`를 `CopyOnWriteArrayList`로 두면 복사본 순회가 불필요해진다
- 이벤트 타입 분리 — 상태별로 다른 이벤트, `OrderPaidEvent` / `OrderCancelledEvent`
- 관심 있는 이벤트만 걸러 받기 (지금은 모든 옵저버가 모든 통지를 받고 각자 `switch`한다)
- 비동기 통지와 실패 재시도
- 약한 참조(`WeakReference`) 기반 구독 — 해지를 깜빡해도 누수되지 않게

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 한 사건에 반응해야 할 곳이 **여럿이고, 앞으로 더 늘어날 것 같을 때**
- 반응하는 쪽이 **서로 몰라도 될 때** (메일과 재고는 서로 알 필요가 없다)
- 실행 중에 대상을 켜고 끄고 싶을 때
- 핵심 로직과 부가 처리를 떼어내고 싶을 때 (주문 처리 vs 알림)

**안 쓴다**
- 반응하는 곳이 하나뿐이고 늘어날 일이 없을 때 — 그냥 호출하면 된다
- **순서가 중요할 때** — 옵저버는 순서를 보장하지 않는다
- 반응 중 하나라도 실패하면 전체를 되돌려야 할 때 — 그건 하나의 트랜잭션이지 통지가 아니다
- 옵저버가 다시 Subject를 바꾸는 구조일 때 — 통지가 통지를 부르는 순환에 빠지기 쉽다

이 패턴의 진짜 대가는 **흐름이 코드에 드러나지 않는다**는 것이다. `changeStatus()`만 읽어서는 무슨 일이 일어나는지 알 수 없고, 등록하는 곳을 찾아가야 안다. 얻는 것은 결합도, 잃는 것은 추적 가능성이다.
