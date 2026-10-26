# 전략 패턴 (Strategy Pattern)

> 알고리즘을 각각 클래스로 캡슐화해서, **실행 중에 갈아끼울 수 있게** 만드는 패턴.

할인 정책 계산을 예제로 구현했다.

![전략 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/strategy-pattern.png)

---

## 1. 왜 필요한가

할인 방식이 늘어날 때 이런 코드가 생긴다.

```java
// 전략 패턴을 쓰지 않았을 때
public int finalAmount() {
    if (type == RATE)        return totalAmount - totalAmount * percent / 100;
    else if (type == FIXED)  return totalAmount - discountAmount;
    else if (type == COUPON) return ...;   // 정책이 늘 때마다 이 메서드를 다시 연다
}
```

문제는 **정책이 하나 추가될 때마다 이미 잘 돌아가던 `Order`를 수정해야 한다**는 것이다. 그때마다 주문 전체가 회귀 위험에 노출된다.

전략 패턴은 "깎는 방법"을 인터페이스 뒤로 밀어낸다. 그러면 `Order`는 다시 열 일이 없다.

```java
// 전략 패턴을 썼을 때 — 분기가 사라진다
public int finalAmount() {
    return totalAmount - discountPolicy.discount(totalAmount);
}
```

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Strategy** | 알고리즘의 공통 규격을 선언 | `DiscountPolicy` |
| **ConcreteStrategy** | 알고리즘을 실제로 구현 | `RateDiscountPolicy` · `FixedAmountDiscountPolicy` |
| **Context** | 전략을 보관하고 계산을 위임 | `Order` |
| **Client** | 전략을 골라 Context에 주입 | `Main` |

### Strategy — 교체 가능한 알고리즘의 규격

```java
public interface DiscountPolicy {
    int discount(int amount);
}
```

### ConcreteStrategy — 규격을 각자 다르게 구현

```java
public class RateDiscountPolicy implements DiscountPolicy {
    private final int percent;

    @Override
    public int discount(int amount) {
        return amount * percent / 100;      // 정률
    }
}

public class FixedAmountDiscountPolicy implements DiscountPolicy {
    private final int discountAmount;

    @Override
    public int discount(int amount) {
        return discountAmount;              // 정액
    }
}
```

### Context — 계산을 직접 하지 않고 전략에게 넘긴다

```java
public class Order {

    private final int totalAmount;
    private DiscountPolicy discountPolicy;      // 갈아끼울 것이므로 final 아님

    public void changeDiscountPolicy(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;   // 실행 중 교체
    }

    public int finalAmount() {
        return totalAmount - discountPolicy.discount(totalAmount);   // 위임
    }
}
```

`Order`는 자기가 들고 있는 게 정률인지 정액인지 **모른다.** 이게 이 패턴의 전부다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out strategy.code.Main
```

```
10% 할인  → 18000원
5,000원 할인 → 15000원
```

같은 `Order` 객체인데 전략만 바꾸니 결과가 달라진다.

```java
Order order = new Order(20_000, new RateDiscountPolicy(10));
order.finalAmount();                                          // 18000

order.changeDiscountPolicy(new FixedAmountDiscountPolicy(5_000));
order.finalAmount();                                          // 15000
```

---

## 4. 새 정책을 추가하려면

```java
public class CouponDiscountPolicy implements DiscountPolicy {
    @Override
    public int discount(int amount) {
        return amount > 50_000 ? 10_000 : 0;
    }
}
```

**클래스 파일 하나 추가가 전부다.** `Order`도, 기존 정책들도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

---

## 5. 알아둘 점

### 반환 타입은 반드시 통일되어야 한다

전략마다 반환 타입이 다르면 호출부가 "어떤 구현체인지" 알아야 하므로, **교체 가능성이라는 전제 자체가 깨진다.**

결과가 다양해야 한다면 반환 타입을 하나로 올려야 한다.

```java
public record DiscountResult(int discountAmount, boolean freeShipping, String gift) { }

public interface DiscountPolicy {
    DiscountResult discount(int amount);
}
```

> JDK의 대표적인 전략 패턴인 `Comparator<T>`도 입력은 제네릭이지만 **반환은 항상 `int` 고정**이다.

### 메서드가 하나면 람다도 전략이다

`DiscountPolicy`는 추상 메서드가 하나뿐이라 람다를 그대로 넘길 수 있다.

```java
order.changeDiscountPolicy(amount -> amount / 2);   // 반값
order.changeDiscountPolicy(amount -> 0);            // 할인 없음
```

간단하고 일회성인 규칙은 람다, **도메인 용어로 부를 이름이 있는 규칙**은 클래스로 만든다. 람다에는 이름과 설명이 남지 않기 때문이다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- `Order` 생성자의 `null` 정책 검증 (`Objects.requireNonNull`)
- 금액 값 객체 (`int` 대신 `Money`) — 원 단위 절사, 음수 방지
- 할인액이 주문 금액을 넘는 경우의 상한 처리
- 기본 정책(Null Object) — `null` 대신 "할인 없음" 객체

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 같은 일을 하는 방법이 여러 개고, 앞으로 더 늘어날 것 같을 때
- 실행 중에 방법을 바꿔야 할 때
- `if-else` / `switch`가 조건이 아니라 **"종류"** 로 갈라질 때

**안 쓴다**
- 분기가 2개뿐이고 늘어날 일이 없을 때 — 클래스만 늘고 얻는 게 없다
- 분기 조건이 "종류"가 아니라 진짜 상태 판단일 때
- 반환 타입이나 파라미터가 구현마다 달라야 할 때 — 애초에 전략이 아니다
