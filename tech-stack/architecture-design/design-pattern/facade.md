# 퍼사드 패턴 (Facade Pattern)

> 복잡한 서브시스템 여러 개 앞에 **창구 하나를 세워서**, 클라이언트가 그 하나만 알면 되게 만드는 패턴.

주문 처리를 예제로 구현했다.

![퍼사드 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/facade-pattern.png)

---

#### 1. 왜 필요한가

주문 하나를 넣으려면 다섯 곳을 거쳐야 한다. 재고 예약 → 결제 → 배송 접수 → 알림 → 이력 기록.

파사드 없이 쓰면 호출부가 이렇게 된다.

```java
// 파사드를 쓰지 않았을 때
public void order(...) {
    if (!inventory.reserve(sku, qty)) return 실패;

    String approvalNo;
    try {
        approvalNo = payment.pay(orderId, amount);
    } catch (RuntimeException e) {
        inventory.release(sku, qty);      // ← 이걸 빠뜨리면 재고가 조용히 샌다
        return 실패;
    }

    String trackingNo = shipping.schedule(orderId, address);
    notification.send(email, "주문 완료 — 송장 " + trackingNo);
    auditLog.record(orderId + " 주문 완료");
}
```

문제는 서브시스템이 다섯이라는 게 아니다. **클라이언트가 서브시스템들의 사용 규약을 알아야 한다**는 것이다.

- **호출 순서** — 결제 전에 재고를 잡아야 하고, 배송은 결제 후여야 한다
- **되돌리기** — 결제가 깨지면 예약한 재고를 반드시 반환해야 한다
- **실패 규약이 제각각** — 재고는 `boolean`, 결제는 **예외**로 실패를 알린다

이 지식이 **주문을 넣는 모든 자리에 복사된다.** 관리자 화면, 배치 작업, API 컨트롤러가 각자 같은 순서를 다시 쓴다. 그리고 한 곳이라도 `release()`를 빠뜨리면 재고가 새는데, 그 버그는 한참 뒤에야 발견된다.

파사드는 그 규약을 **한 곳에 가둔다.**

```java
// 파사드를 썼을 때
OrderResult result = orderFacade.placeOrder(request);
```

---

#### 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Facade** | 서브시스템 앞의 창구 | `OrderFacade` |
| **Subsystem** | 각자 자기 일만 하는 것들 | `InventoryService` · `PaymentService` · `ShippingService` · `NotificationService` · `AuditLog` |
| **Client** | 파사드만 안다 | `Main` |

참여자가 **둘뿐이다.** 인터페이스도, 상속 계층도 없다. GoF 패턴 중 구조가 가장 단순하다.

#### 결합은 한 방향이다

```java
public class InventoryService {
    // OrderFacade 라는 단어가 이 파일에 없다
}
```

파사드는 서브시스템을 알지만, **서브시스템은 파사드를 모른다.** 이 방향이 지켜져야 서브시스템을 파사드 없이도 쓸 수 있고, 다른 파사드를 하나 더 세울 수도 있다.

#### 파사드가 흡수하는 것은 기능이 아니라 규약이다

```java
public OrderResult placeOrder(OrderRequest request) {
    auditLog.record(request.orderId() + " 주문 접수");

    if (!inventory.reserve(request.sku(), request.quantity())) {     // ① boolean 실패
        auditLog.record(request.orderId() + " 재고 부족으로 중단");
        return OrderResult.failed("재고가 부족합니다");
    }

    String approvalNo;
    try {
        approvalNo = payment.pay(request.orderId(), request.totalAmount());
    } catch (RuntimeException e) {                                   // ② 예외 실패
        inventory.release(request.sku(), request.quantity());        //    되돌리기
        auditLog.record(request.orderId() + " 결제 실패로 취소");
        return OrderResult.failed("결제에 실패했습니다 — " + e.getMessage());
    }

    String trackingNo = shipping.schedule(request.orderId(), request.address());
    notification.send(request.customerEmail(), "주문 완료 — 송장 " + trackingNo);
    auditLog.record(request.orderId() + " 주문 완료");

    return OrderResult.succeeded(approvalNo, trackingNo);
}
```

이 메서드에 **새로운 기능은 하나도 없다.** 재고를 깎는 것도, 결제하는 것도 전부 서브시스템이 한다. 파사드가 아는 것은 **부르는 순서**와 **실패했을 때 무엇을 되돌려야 하는지**뿐이다.

그리고 `boolean`과 예외로 갈리던 실패 규약이 `OrderResult` 하나로 모인다. 클라이언트는 `result.success()`만 본다.

---

#### 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out facade.code.Main
```

```
--- 1. 한 줄로 다섯 서브시스템이 움직인다 ---
    [이력] ORD-1001 주문 접수
    [재고] COFFEE-01 2개 예약 (잔여 8)
    [결제] 36,000원 승인 (PAY-5001)
    [배송] 서울시 성동구 왕십리로 100 접수 (TRK-1001)
    [알림] user@example.com ← 주문 ORD-1001 완료 — 송장 TRK-1001
    [이력] ORD-1001 주문 완료
  결과: 성공 (승인 PAY-5001, 송장 TRK-1001)

--- 2. 재고가 없으면 결제까지 가지 않는다 ---
    [이력] ORD-1002 주문 접수
    [재고] MUG-02 부족 — 요청 3, 보유 1
    [이력] ORD-1002 재고 부족으로 중단
  결과: 실패 (재고가 부족합니다)

--- 3. 결제가 실패하면 예약한 재고를 되돌린다 ---
    [이력] ORD-1003 주문 접수
    [재고] COFFEE-01 3개 예약 (잔여 5)
    [결제] 1,500,000원 한도 초과 — 거절
    [재고] COFFEE-01 3개 반환 (잔여 8)
    [이력] ORD-1003 결제 실패로 취소
  결과: 실패 (결제에 실패했습니다 — 결제 한도 초과)
→ [재고] 반환이 찍혔다. 이 규약을 아는 것은 파사드뿐이다.

--- 4. 파사드는 서브시스템을 감추지 않는다 ---
    [재고] COFFEE-01 1개 예약 (잔여 7)
→ 파사드는 강제가 아니라 편의다. 서브시스템은 여전히 public 이다.
```

**3번이 이 패턴의 값어치다.** 잔여 8 → 예약 후 5 → 반환 후 다시 8. 클라이언트는 `placeOrder()`를 한 번 불렀을 뿐인데 되돌리기가 일어났다.

**4번은 오해를 막기 위한 것이다.** 재고만 확인하는 배치 작업이라면 `InventoryService`를 직접 써도 된다. 파사드는 그 길을 막지 않는다.

---

#### 4. 새 단계를 추가하려면

포인트 적립이 생겼다고 하자.

```java
public OrderResult placeOrder(OrderRequest request) {
    ...
    String trackingNo = shipping.schedule(...);
    pointService.earn(request.customerEmail(), request.totalAmount());   // ← 한 줄
    notification.send(...);
    ...
}
```

**파사드 한 곳만 고친다.** 주문을 넣는 컨트롤러도, 배치도, 관리자 화면도 열지 않는다.

파사드가 없었다면 `placeOrder`를 호출하던 **모든 자리**를 찾아다녀야 했고, 한 곳을 빠뜨리면 그 경로로 들어온 주문만 포인트가 안 쌓인다.

---

#### 5. 알아둘 점

#### 감추는 게 아니라 가려 주는 것이다

가장 중요한 원칙이다.

```java
// 이러면 파사드가 아니다
public class OrderFacade {
    private InventoryService inventory;      // 서브시스템을 못 쓰게 막는다
    // 재고만 확인하고 싶은 사람은 방법이 없다
}
```

파사드는 **선택적 편의**다. 대부분의 클라이언트가 편해지는 길을 열어 줄 뿐, 서브시스템을 직접 쓰는 길을 막지 않는다. 막는 순간 파사드가 모든 요구를 흡수해야 하고, 메서드가 끝없이 늘어난다.

> GoF도 이 점을 명시한다. "파사드는 서브시스템 사용을 쉽게 하지만, 필요한 클라이언트가 서브시스템에 직접 접근하는 것을 막지 않는다."

#### 파사드에 비즈니스 로직을 넣지 않는다

```java
// 이러면 파사드가 썩기 시작한 것이다
public OrderResult placeOrder(OrderRequest request) {
    long amount = request.totalAmount();
    if (customer.grade() == VIP) amount = amount * 90 / 100;   // ← 할인 정책
    if (amount > 500_000) amount -= 10_000;                    // ← 또 정책
    ...
}
```

할인 계산은 도메인의 일이다. 파사드가 판단을 시작하면 **아무도 읽지 않는 God Object**가 된다.

**썩고 있다는 신호 세 가지**

- 파사드에 **비즈니스 판단**이 쌓인다 (할인 계산, 등급 판정, 세금 계산)
- 서브시스템을 `private`으로 **감추기 시작**한다
- 메서드가 계속 늘어 **수십 개**가 된다

판단 기준은 [어댑터](adapter.md)와 같다. **"서브시스템을 다른 것으로 바꿔도 이 코드가 필요한가?"** 필요하다면 그건 파사드의 일이 아니다.

#### 어댑터와의 차이

둘 다 "감싸서 쓰기 편하게 한다"고 요약되기 때문에 자주 헷갈린다. **기준이 어디 있느냐**가 다르다.

| | [어댑터](adapter.md) | 파사드 |
|---|---|---|
| 맞출 규격 | **있다** — 기존 인터페이스에 맞춘다 | **없다** — 새로 발명한다 |
| 감싸는 대상 | 보통 1개 | **여러 개** |
| 목적 | 안 맞는 것을 맞물리게 | 복잡한 걸 단순하게 |
| 인터페이스 개수 | 대상과 1:1 | 여러 개 → 1개 |

어댑터는 `PaymentGateway`라는 **정해진 규격**이 먼저 있고 거기 맞춘다. 파사드는 맞출 규격이 없다. `placeOrder()`라는 이름도, 시그니처도 우리가 만든 것이다.

> 한 문장 — **어댑터는 남의 규격에 맞추고, 파사드는 우리 규격을 만든다.**

#### 데코레이터·프록시와도 다르다

이 저장소에서 지금까지 나온 "감싸는 패턴"을 한 표에 모으면 이렇다.

| 패턴 | 인터페이스 | 감싸는 개수 | 목적 |
|---|---|---|---|
| [어댑터](adapter.md) | **바꾼다** | 1 | 맞물리게 |
| [데코레이터](decorator.md) | 유지한다 | 1 | 기능을 더하려고 |
| 프록시 | 유지한다 | 1 | 접근을 통제하려고 |
| **파사드** | **발명한다** | **여러 개** | 단순하게 |

파사드만 **여러 개를 감싼다.** 이게 가장 빠른 구분법이다.

#### 트랜잭션이 필요하면 파사드로는 부족하다

이 예제의 되돌리기는 손으로 짠 보상 로직이다.

```java
catch (RuntimeException e) {
    inventory.release(request.sku(), request.quantity());   // 손으로 되돌린다
}
```

배송 접수까지 성공한 뒤 알림에서 터지면? 배송은 되돌리지 않는다. 단계가 늘수록 보상 조합이 폭발한다.

**같은 DB 안이라면** `@Transactional`이 답이다. 파사드는 스프링에서 대개 `@Service` + `@Transactional`로 나타난다.

**서로 다른 시스템에 걸쳐 있다면** 파사드로 감당할 수 없다. Saga 패턴처럼 보상 트랜잭션을 정식으로 설계해야 한다. 파사드는 **"실패해도 되돌릴 게 한둘"** 인 규모까지가 적정선이다.

#### 스프링에서 파사드는 어디에 있나

```java
@Service
@RequiredArgsConstructor
public class OrderService {          // 사실상 파사드다

    private final InventoryService inventory;
    private final PaymentService payment;
    ...

    @Transactional
    public OrderResult placeOrder(OrderRequest request) { ... }
}
```

이름이 `~Facade`가 아닐 뿐, 여러 도메인 서비스를 조합하는 **애플리케이션 서비스**가 파사드다. 컨트롤러가 도메인 서비스를 직접 여러 개 호출하고 있다면, 그 순서 지식이 컨트롤러로 새고 있다는 뜻이다.

> 최근에는 도메인 서비스와 구분하려고 명시적으로 `~Facade`라고 이름 붙이는 팀도 많다. 이름보다 중요한 것은 **"이 클래스가 순서만 아는가, 판단도 하는가"** 다.

#### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 트랜잭션 경계 (`@Transactional`)
- 배송 접수 이후 단계의 실패 처리 — 지금은 결제 실패만 되돌린다
- 알림 실패가 주문을 막지 않게 하기 — [옵저버](observer.md)나 [발행-구독](publisher-subscriber.md)으로 뺄 자리다
- 동시성 — `InventoryService`의 재고 맵이 스레드 안전하지 않다
- 파사드 인터페이스 추출 — 테스트에서 파사드를 대체해야 한다면 필요하다

---

#### 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 하나의 작업에 **서브시스템 여러 개**가 정해진 순서로 얽힐 때
- 그 **순서와 되돌리기 규약**이 호출부마다 복사되고 있을 때
- 클라이언트(컨트롤러, 배치)가 서브시스템의 내부 사정을 알 이유가 없을 때
- 레거시나 외부 라이브러리를 감싸 **팀의 언어로 된 진입점**을 만들고 싶을 때

**안 쓴다**
- 서브시스템이 하나뿐일 때 — 감쌀 복잡함이 없다. [어댑터](adapter.md)이거나 그냥 낭비다
- 호출부가 서브시스템을 **한두 개만** 쓸 때 — 파사드를 거치면 오히려 멀어진다
- 파사드에 **판단이 들어가야** 할 때 — 그건 도메인 서비스다
- 서브시스템 조합이 호출부마다 **크게 다를** 때 — 파사드 메서드가 조합 수만큼 늘어난다

판단은 한 줄이다. **"이 호출 순서를 아는 곳이 몇 군데인가?"** 둘 이상이면 파사드를 세울 때다.
