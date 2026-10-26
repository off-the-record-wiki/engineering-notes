# 추상 팩토리 패턴 (Abstract Factory Pattern)

> 서로 어울려야 하는 **제품 한 벌을 통째로** 만들어 주는 패턴. 팩토리를 하나 고르면 제품군 전체가 함께 결정된다.

국가별 체크아웃(결제 + 영수증)을 예제로 구현했다.

![추상 팩토리 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/abstract-factory-pattern.png)

---

## 1. 왜 필요한가

나라별로 결제기와 영수증이 갈릴 때 이런 코드가 생긴다.

```java
// 추상 팩토리를 쓰지 않았을 때
public class CheckoutService {
    public String checkout(String region, long amount) {
        PaymentGateway gateway;
        ReceiptPrinter printer;

        if (region.equals("KR")) {
            gateway = new KoreaPaymentGateway();
            printer = new KoreaReceiptPrinter();
        } else {
            gateway = new UsPaymentGateway();
            printer = new UsReceiptPrinter();   // 여기서 Korea를 써도 컴파일은 된다
        }
        ...
    }
}
```

문제는 **분기가 늘어난다**는 게 아니다. 진짜 문제는 **짝을 잘못 맞출 수 있다**는 것이다.

```java
gateway = new KoreaPaymentGateway();   // 금액을 '원'으로, 부가세 포함으로 해석
printer = new UsReceiptPrinter();      // 금액을 '센트'로, 판매세 별도로 해석
```

두 줄 다 타입은 맞다. 컴파일도 되고 테스트도 통과할 수 있다. 그런데 20,000원을 결제하고 영수증에는 `$200.00`이 찍힌다. **금액 버그는 조용히 새고, 발견은 정산 때 된다.**

여기서 `PaymentGateway`와 `ReceiptPrinter`는 독립적인 두 부품이 아니다. **통화 단위와 세금 규칙이라는 전제를 공유하는 한 벌**이다. 그런데 그 사실이 코드 어디에도 표현되어 있지 않다. 사람의 주의력만이 짝을 지켜 준다.

추상 팩토리는 그 짝을 **타입으로 못 박는다.**

```java
// 추상 팩토리를 썼을 때 — 짝이 어긋날 수 없다
public CheckoutService(CheckoutFactory factory) {
    this.paymentGateway = factory.createPaymentGateway();
    this.receiptPrinter = factory.createReceiptPrinter();
}
```

`KoreaCheckoutFactory`를 넘기면 두 제품이 **함께** 한국 것이 된다. 한국 결제기와 미국 영수증을 짝지을 방법이 아예 없다.

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **AbstractFactory** | 제품 한 벌을 만드는 규격 | `CheckoutFactory` |
| **ConcreteFactory** | 특정 제품군을 실제로 조립 | `KoreaCheckoutFactory` · `UsCheckoutFactory` |
| **AbstractProduct** | 제품 각각의 규격 | `PaymentGateway` · `ReceiptPrinter` |
| **ConcreteProduct** | 제품군에 속한 실제 구현 | `KoreaPaymentGateway` · `KoreaReceiptPrinter` · `UsPaymentGateway` · `UsReceiptPrinter` |
| **Client** | 팩토리를 골라 제품을 받아 쓴다 | `CheckoutService` · `Main` |

참여자가 다섯이라 앞선 두 패턴보다 클래스가 많다. 대신 **격자로 보면 단순하다.**

|  | PaymentGateway | ReceiptPrinter |
|---|---|---|
| **한국** | `KoreaPaymentGateway` | `KoreaReceiptPrinter` |
| **미국** | `UsPaymentGateway` | `UsReceiptPrinter` |

**가로 한 줄이 제품군(family)이고, 세로 한 칸이 팩토리 메서드 하나다.** 이 격자를 머릿속에 그리면 나머지는 다 따라온다.

### AbstractFactory — 메서드가 여러 개인 게 핵심이다

```java
public interface CheckoutFactory {
    PaymentGateway createPaymentGateway();
    ReceiptPrinter createReceiptPrinter();
}
```

메서드 개수가 곧 제품군의 크기다. 이 둘이 **한 인터페이스에 묶여 있다는 사실 자체**가 "같이 써야 한다"는 규칙의 표현이다.

### ConcreteFactory — 한 벌을 묶는 것 말고는 하는 일이 없다

```java
public class KoreaCheckoutFactory implements CheckoutFactory {
    @Override public PaymentGateway createPaymentGateway() { return new KoreaPaymentGateway(); }
    @Override public ReceiptPrinter createReceiptPrinter() { return new KoreaReceiptPrinter(); }
}
```

로직이 없다. 그래도 이 클래스가 필요한 이유는, **"한국 제품군이란 이 둘"이라는 지식을 담을 자리가 여기밖에 없기 때문**이다. 이 클래스가 없으면 그 지식은 다시 클라이언트 코드에 흩어진다.

### Client — 구체 클래스 이름을 모른다

```java
public class CheckoutService {

    private final PaymentGateway paymentGateway;
    private final ReceiptPrinter receiptPrinter;

    public CheckoutService(CheckoutFactory factory) {
        this.paymentGateway = factory.createPaymentGateway();
        this.receiptPrinter = factory.createReceiptPrinter();
    }

    public String checkout(long amount) {
        return paymentGateway.approve(amount) + "\n" + receiptPrinter.print(amount);
    }
}
```

`CheckoutService`에는 `Korea`, `Us` 라는 글자가 **한 번도 나오지 않는다.** 아는 타입은 추상 타입 세 개뿐이다.

구체 클래스 이름이 등장하는 곳은 `Main` 한 군데다.

```java
private static CheckoutFactory factoryOf(String region) {
    return switch (region) {
        case "KR" -> new KoreaCheckoutFactory();
        case "US" -> new UsCheckoutFactory();
        default -> throw new IllegalArgumentException("지원하지 않는 지역: " + region);
    };
}
```

분기가 사라진 게 아니라 **한 곳으로 모인 것**이다. 이게 추상 팩토리가 실제로 하는 일이다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out abstract_factory.code.Main
```

```
[국내 카드사] 20,000원 승인
------------------------------
 합계            20,000원
 부가세 포함      1,818원
------------------------------

[Stripe] $25.99 charged
------------------------------
 Subtotal     $    25.99
 Sales tax    added at checkout
------------------------------
```

`checkout()` 코드는 두 경우가 **완전히 같다.** 통화도 세금 표기도 다른데, 그 차이는 전부 제품군 안에 들어 있다.

---

## 4. 새 지역을 추가하려면

```java
public class JapanPaymentGateway implements PaymentGateway { ... }
public class JapanReceiptPrinter implements ReceiptPrinter { ... }

public class JapanCheckoutFactory implements CheckoutFactory {
    @Override public PaymentGateway createPaymentGateway() { return new JapanPaymentGateway(); }
    @Override public ReceiptPrinter createReceiptPrinter() { return new JapanReceiptPrinter(); }
}
```

**클래스 세 개 추가 + `factoryOf()`에 한 줄.** `CheckoutService`도, 한국·미국 코드도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

격자로 보면 **행이 하나 늘어난 것**이다. 추상 팩토리는 행 추가에 강하다.

---

## 5. 알아둘 점

### 제품이 늘어나면 모든 팩토리를 고쳐야 한다

이 패턴의 가장 유명한 단점이고, 쓸지 말지의 판단 기준이기도 하다.

```java
public interface CheckoutFactory {
    PaymentGateway createPaymentGateway();
    ReceiptPrinter createReceiptPrinter();
    RefundPolicy   createRefundPolicy();     // ← 한 줄 추가하면
}
```

이 순간 `KoreaCheckoutFactory`, `UsCheckoutFactory`, 앞으로 추가될 모든 팩토리가 **전부 컴파일 에러**가 난다. 격자에서 **열이 늘어났기 때문**이다.

| | 추가 비용 |
|---|---|
| **행 추가** (새 지역) | 쉽다 — 새 클래스만 만든다 |
| **열 추가** (새 제품 종류) | 비싸다 — 기존 팩토리를 전부 연다 |

그래서 판단은 이 한 줄이다. **"행은 자주 늘고 열은 잘 안 느는가?"** 지역·테마·DB 벤더는 대체로 그렇다. 그렇지 않다면 이 패턴은 부담만 된다.

> 컴파일 에러가 나는 건 사실 **안전장치**다. 새 제품을 각 제품군이 어떻게 채울지 강제로 답하게 만든다. 조용히 빠뜨리는 것보다 낫다. `default` 메서드로 막아 버리면 그 안전장치를 스스로 끄는 셈이다.

### 팩토리 메서드 패턴과의 차이

이름이 비슷해서 자주 섞이지만 다루는 단위가 다르다.

| | [팩토리 메서드](factory-method.md) | 추상 팩토리 |
|---|---|---|
| 만드는 것 | 제품 **하나** | 서로 어울리는 제품 **한 벌** |
| 생성 메서드 개수 | 1개 | 제품 종류만큼 (N개) |
| 수단 | 주로 **상속** (하위 클래스가 결정) | 주로 **합성** (팩토리를 주입) |
| 막아 주는 것 | 구체 클래스 직접 생성 | **제품군이 섞이는 것** |

`CheckoutFactory`의 메서드를 하나로 줄이면 그건 그냥 팩토리 메서드다. **"짝이 어긋나면 안 되는 제품이 둘 이상인가?"** — 여기서 갈린다.

### 전략 패턴과 헷갈릴 때

`PaymentGateway` 하나만 보면 전략 패턴과 똑같이 생겼다. 실제로 각 제품은 전략처럼 쓰인다. 차이는 **"고르는 단위"** 다.

- **전략** — 알고리즘 **하나**를 고른다. 정률이든 정액이든 서로 독립이다.
- **추상 팩토리** — 알고리즘 **여러 개를 한꺼번에** 고른다. 따로 고르면 어긋나기 때문이다.

전략을 여러 개 주입하는데 **"이 조합은 안 된다"는 규칙이 생기는 순간**, 그 조합을 묶어 줄 팩토리가 필요해진 것이다.

### 팩토리를 필드로 들고 있을 필요는 없다

```java
public CheckoutService(CheckoutFactory factory) {
    this.paymentGateway = factory.createPaymentGateway();   // 생성 시점에 한 번
    this.receiptPrinter = factory.createReceiptPrinter();
}
```

제품군은 한 번 정해지면 바뀌지 않으므로, 팩토리는 **생성자에서 쓰고 버린다.** 팩토리를 필드로 두고 매번 `create...()`를 부르면, 호출할 때마다 객체가 새로 생기고 "지금 이 제품군이 맞나"를 계속 신경 써야 한다.

실행 중에 지역이 바뀌어야 한다면 `CheckoutService`를 새로 만드는 게 맞다. **제품군을 갈아끼우는 건 객체 하나를 바꾸는 일이 아니라 한 벌을 통째로 바꾸는 일이다.**

### ConcreteFactory는 보통 상태가 없다

로직도 상태도 없으므로 매번 `new` 할 이유가 없다. 실무에서는 싱글턴이나 스프링 빈으로 하나만 둔다. 여기서는 패턴 구조를 흐리지 않으려고 그냥 `new` 했다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 금액 값 객체 (`long` 대신 `Money` — 통화 단위를 타입으로 못 박으면 섞임 사고가 한 겹 더 막힌다)
- 지역 코드 `String` 대신 `enum Region` (오타 방지, `switch` 완전성 검사)
- 결제 실패·타임아웃 처리와 재시도
- 팩토리 인스턴스 재사용 (싱글턴 / DI 컨테이너 등록)

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 제품 여러 개가 **한 벌로 움직여야** 하고, 짝이 어긋나면 버그가 될 때
- 그 한 벌이 지역·테마·벤더 같은 **축 하나**로 통째로 갈릴 때
- 클라이언트가 구체 클래스를 몰라야 할 때 (라이브러리·플러그인 경계)

**안 쓴다**
- 제품이 하나뿐일 때 — 팩토리 메서드로 충분하다. 인터페이스만 늘어난다
- 제품들이 서로 독립적일 때 — 묶을 이유가 없다. 각각 주입하면 된다
- 제품 종류가 자주 늘어날 때 — 늘 때마다 모든 팩토리를 열어야 한다
- 조합을 자유롭게 섞어야 할 때 — 이 패턴은 **섞지 못하게 하는 게 목적**이라 정반대다
