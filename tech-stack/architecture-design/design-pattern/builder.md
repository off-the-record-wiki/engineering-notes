# 빌더 패턴 (Builder Pattern)

> 만드는 일을 별도 객체에 맡겨서, **완성된 것만 바깥으로 나가게** 하는 패턴.

주문서를 예제로 구현했다.

![빌더 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/builder-pattern.png)

> **이름 하나에 서로 다른 두 패턴이 붙어 있다.** 실무에서 "빌더 패턴"이라고 하면 거의 *Effective Java* 아이템 2의 빌더를 말하지만, GoF 책이 말하는 빌더는 다른 문제를 푼다. 이 문서는 **둘 다** 구현하고 차이를 밝힌다.

---

#### 1. 왜 필요한가

주문서에 필드가 늘어난다. 필수 셋(주문번호, 고객명, 금액)에 선택 넷(배송지, 요청사항, 쿠폰, 선물포장).

**점층적 생성자(telescoping constructor)** 로 풀면 이렇게 된다.

```java
// 빌더를 쓰지 않았을 때
public Order(String orderId, String customerName, int totalAmount) { ... }
public Order(String orderId, String customerName, int totalAmount, String address) { ... }
public Order(String orderId, String customerName, int totalAmount, String address,
             String request) { ... }
// ... 선택 항목 조합마다 하나씩
```

호출부는 이렇게 생긴다.

```java
new Order("ORD-1001", "홍길동", 47000, "서울시…", "부재시…", null, true);
```

**`null`과 `true`가 무엇인지 호출부만 봐서는 모른다.** 인자 순서를 외워야 하고, `String` 두 개의 순서가 바뀌어도 컴파일러는 아무 말 안 한다.

자바빈즈 방식(기본 생성자 + 세터)은 읽기는 좋아지지만 다른 걸 잃는다.

```java
Order order = new Order();
order.setOrderId("ORD-1001");
order.setTotalAmount(47000);
// ← 이 시점의 order는 '미완성'이다. 그런데 이미 바깥에 존재한다
order.setAddress("서울시…");
```

**미완성 객체가 돌아다닌다.** 세터를 하나 빠뜨려도 아무도 모르고, 모든 필드에 세터가 있으니 **불변으로 만들 수 없다.**

빌더는 둘 다 해결한다.

```java
Order order = Order.builder("ORD-1001", "홍길동", 47_000)   // 필수는 강제
    .address("서울시 성동구 왕십리로 100")                     // 선택은 이름으로
    .coupon("WELCOME10")
    .giftWrap(true)
    .build();                                              // 여기서 완성 + 검증
```

**미완성 상태는 `Builder` 안에만 있고, 바깥으로는 완성된 불변 객체만 나간다.**

---

#### 2. 구성 요소

#### ① 실무형 빌더 — *Effective Java* 아이템 2

| 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Product** | 완성될 불변 객체 | `Order` |
| **Builder** | 만드는 도중의 변경 가능한 상태 | `Order.Builder` (정적 중첩 클래스) |

참여자가 **둘뿐이다.** Director도, 인터페이스도 없다.

```java
public class Order {

    private final String orderId;       // 전부 final
    // ...

    private Order(Builder builder) { ... }        // ① 생성자가 private

    public static Builder builder(String orderId, String customerName, int totalAmount) {
        return new Builder(orderId, customerName, totalAmount);   // ② 필수값은 여기서
    }

    public static class Builder {
        private final String orderId;             // 필수 — final
        private String address = "";              // 선택 — 기본값

        public Builder address(String address) {
            this.address = address;
            return this;                          // ③ 체이닝
        }

        public Order build() {
            if (giftWrap && address.isBlank()) {  // ④ 조합 검증
                throw new IllegalStateException("선물포장은 배송지가 있어야 한다");
            }
            return new Order(this);
        }
    }
}
```

네 지점 각각에 이유가 있다.

**① 생성자를 `private`으로 닫는다.** 바깥에서 `new Order(...)`를 못 쓰므로 만드는 경로가 빌더 하나로 좁혀진다. 그래야 `build()`의 검증을 우회할 수 없다.

**② 필수값은 빌더 생성자로 받는다.** `build()`에서 `null` 검사를 하는 것보다 낫다. **컴파일러가 강제**해 주기 때문이다. 필수값까지 세터로 받으면 빠뜨려도 컴파일이 통과하고 런타임에야 터진다.

**③ `this`를 반환해 체이닝한다.** 실무형 빌더에서는 이게 사실상 필수다. 호출부의 가독성이 이 패턴을 쓰는 이유의 절반이다.

**④ 여러 필드에 걸친 검증은 `build()`에서만 할 수 있다.** 가장 중요한 지점이다.

```java
.giftWrap(true)     // 이 세터만 봐서는 "배송지가 있는지" 알 수 없다
```

세터는 자기 값만 안다. "선물포장이면 배송지가 있어야 한다" 같은 규칙은 **모든 값이 모인 뒤에야** 판단할 수 있고, 그 유일한 시점이 `build()`다. 생성자 방식으로는 애초에 이런 검증을 걸 자리가 없다.

#### ② GoF 원형 빌더 — 같은 절차, 다른 표현

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Director** | 조립 **절차**를 소유 | `ReceiptDirector` |
| **Builder** | 단계의 규격 | `ReceiptBuilder` |
| **ConcreteBuilder** | 단계를 실제로 구현 | `TextReceiptBuilder` · `HtmlReceiptBuilder` |
| **Product** | 결과물 | 영수증 문자열 |

이쪽이 푸는 문제는 완전히 다르다. **영수증을 만드는 순서는 형식과 무관하게 같다.**

```java
public class ReceiptDirector {

    private final ReceiptBuilder builder;

    public String build(Order order) {
        builder.header(order.orderId(), order.customerName());     // 절차가
        builder.line("배송지", ...);                                //  여기
        if (order.coupon() != null) builder.line("쿠폰", ...);      //   한 곳에만
        builder.total(order.totalAmount());                        //    있다
        builder.footer();
        return builder.result();
    }
}
```

`ReceiptDirector`에는 텍스트도 HTML도 등장하지 않는다. 형식이 늘어도 이 파일은 열리지 않는다.

**반환 타입이 `void`인 것에 주목하자.**

```java
public interface ReceiptBuilder {
    void header(String orderId, String customerName);
    void line(String label, String value);
    ...
}
```

GoF 빌더는 **체이닝이 필수가 아니다.** 부르는 쪽이 Director 하나뿐이라 `this`를 돌려줄 이유가 없다. 실무형 빌더와 갈리는 눈에 띄는 표식이다.

---

#### 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out builder.code.Main
```

```
--- 1. 필수는 생성자로, 선택은 이름으로 ---
주문 생성 완료: ORD-1001

--- 2. 선택 항목을 하나도 안 줘도 된다 ---
주문 생성 완료: ORD-1002 (배송지 없음)

--- 3. build() 가 조합을 검증한다 ---
거절됨: 선물포장은 배송지가 있어야 한다
→ 세터 하나만 봐서는 알 수 없는 규칙이다. 모든 값이 모여야 판단할 수 있다.

--- 4. GoF 빌더 — 같은 절차, 다른 표현 ---
[TextReceiptBuilder]
==============================
 주문 확인서  ORD-1001
 고객: 홍길동
------------------------------
 배송지      서울시 성동구 왕십리로 100
 요청사항     부재 시 경비실에 맡겨주세요
 쿠폰       WELCOME10
 선물포장     적용
------------------------------
 합계     47,000원
==============================

[HtmlReceiptBuilder]
<div class="receipt">
  <h2>주문 확인서 ORD-1001</h2>
  <p>고객: 홍길동</p>
  <ul>
    <li><b>배송지</b>: 서울시 성동구 왕십리로 100</li>
    <li><b>요청사항</b>: 부재 시 경비실에 맡겨주세요</li>
    <li><b>쿠폰</b>: WELCOME10</li>
    <li><b>선물포장</b>: 적용</li>
  </ul>
  <strong>합계 47,000원</strong>
</div>

→ ReceiptDirector 코드는 두 경우가 완전히 같다.
```

같은 `Order`, 같은 절차, 결과물만 다르다. 클라이언트가 바꾼 것은 **생성자에 넘긴 빌더 하나**뿐이다.

```java
new ReceiptDirector(new TextReceiptBuilder()).build(order);
new ReceiptDirector(new HtmlReceiptBuilder()).build(order);
```

---

#### 4. 두 빌더는 이름만 같다

| | ① 실무형 (Effective Java) | ② GoF 원형 |
|---|---|---|
| **목적** | 인자 많은 객체를 안전하게 만들기 | **같은 절차로 다른 표현 만들기** |
| 참여자 | Product + Builder — 2개 | Director + Builder + ConcreteBuilder + Product — 4개 |
| Director | 없다 | **있다 — 절차의 주인** |
| Builder | 보통 정적 중첩 클래스 | 별도 인터페이스 |
| 체이닝 | 필수 (`this` 반환) | 불필요 (`void`여도 된다) |
| 구현체 수 | 대개 하나 | **여럿** (형식마다) |
| 초점 | **무엇을** 넣을지 | **어떻게** 그릴지 |

한 줄로 줄이면 이렇다.

> **실무형** — "인자가 너무 많다"를 푼다
> **GoF 원형** — "절차는 같은데 결과물이 다르다"를 푼다

**공통점은 하나다.** 둘 다 미완성 상태를 빌더 안에 가두고, **완성된 것만 바깥으로 내보낸다.** 그것 말고는 거의 다르다.

면접이나 실무 문서에서 "빌더 패턴을 썼다"고 하면 거의 항상 ①이다. 그런데 GoF 책을 펴면 ②가 나온다. 이 어긋남이 빌더 패턴을 헷갈리게 만드는 유일한 이유다.

---

#### 5. 알아둘 점

#### 필수값을 세터로 받으면 안 된다

가장 흔한 실수다.

```java
// 흔히 보이는 모양 — 필수도 세터로 받고 build()에서 검사
Order.builder()
    .orderId("ORD-1001")
    .totalAmount(47_000)
    .build();          // customerName을 빠뜨렸는데 컴파일은 통과한다
```

```java
// 이 예제 — 필수는 빌더 생성자로
Order.builder("ORD-1001", "홍길동", 47_000)      // 빠뜨리면 컴파일 에러
    .build();
```

**런타임 검증보다 컴파일 타임 강제가 낫다.** 필수 인자가 대여섯 개로 늘어나면 이 방식도 부담스러워지는데, 그때는 필수값을 묶는 값 객체가 필요하다는 신호다.

#### 롬복의 `@Builder`는 필수를 강제하지 못한다

```java
@Builder
public class Order { ... }        // 모든 필드가 선택이 된다
```

편하지만 **필수/선택 구분이 사라진다.** 필수값 강제가 중요하다면 `@Builder`를 필드가 아니라 생성자에 붙이거나(`@Builder` on constructor), 직접 쓰는 게 낫다. 검증도 `@Builder` 만으로는 걸 자리가 없다.

#### `record`가 나온 뒤에도 빌더는 필요한가

자바 16의 `record`는 불변 데이터 객체를 한 줄로 만들어 준다. 하지만 **선택 항목이 많으면 여전히 힘들다.**

```java
public record Order(String orderId, String customerName, int totalAmount,
                    String address, String request, String coupon, boolean giftWrap) { }

new Order("ORD-1001", "홍길동", 47000, "서울시…", "부재시…", null, true);   // 다시 원점
```

`record`는 점층적 생성자 문제를 풀지 않는다. **필드가 4개를 넘고 선택 항목이 섞이면 `record`에도 빌더를 붙이는 게 낫다.** 컴팩트 생성자에서 검증하고, 정적 중첩 빌더를 두면 된다.

#### 빌더는 재사용하지 않는 게 안전하다

```java
Order.Builder builder = Order.builder("ORD-1001", "홍길동", 47_000);
Order a = builder.giftWrap(true).address("서울…").build();
Order b = builder.build();        // a의 설정이 그대로 남아 있다
```

빌더는 **변경 가능한 상태**다. 한 번 쓰고 버리는 것을 전제로 설계된다. 여러 개를 만들어야 한다면 빌더를 매번 새로 얻는 게 맞다.

#### 템플릿 메서드 패턴과의 관계 (GoF 빌더 쪽)

`ReceiptDirector`가 하는 일은 [템플릿 메서드](template-method.md)의 `generate()`와 똑같다. **절차를 한 곳에 고정하고 단계를 위임한다.**

차이는 수단이다.

| | 템플릿 메서드 | GoF 빌더 |
|---|---|---|
| 수단 | **상속** — 자식이 단계를 구현 | **합성** — 빌더를 주입 |
| 절차의 위치 | 부모 클래스의 `final` 메서드 | Director |
| 런타임 교체 | 불가능 | 가능 |
| 단계의 상태 | 부모·자식이 공유 | 빌더가 **자기 상태를 축적**한다 |

마지막 항목이 실질적인 갈림길이다. 빌더는 `StringBuilder`를 들고 단계마다 쌓아 간다. **조립 중인 상태를 들고 있어야 한다면 빌더**, 단계마다 독립적으로 결과를 내면 템플릿 메서드가 맞다.

#### 자바 표준 라이브러리의 빌더

```java
StringBuilder sb = new StringBuilder().append("a").append("b");   // 체이닝
Stream.builder().add(1).add(2).build();
HttpRequest.newBuilder().uri(uri).header("k", "v").GET().build();  // 실무형의 전형
Calendar.Builder / Locale.Builder
```

`HttpRequest.Builder`가 ①의 교과서적인 예다. 필수는 `uri()`, 나머지는 선택, `build()`가 완성한다.

#### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 금액 값 객체 (`int` 대신 `Money`)
- 주문 항목 목록 (`List<OrderItem>`) — 컬렉션 필드는 빌더에서 방어적 복사가 필요하다
- `Order`의 `equals`/`hashCode`/`toString`
- 빌더 재사용 방지 (`build()` 후 플래그로 잠그기)
- HTML 이스케이프 — `HtmlReceiptBuilder`가 값을 그대로 넣는다. 실무라면 XSS 대비가 필요하다

---

#### 6. 언제 쓰고, 언제 안 쓰나

**실무형 빌더를 쓴다**
- 생성자 인자가 **4개를 넘고**, 그중 상당수가 선택일 때
- 같은 타입 인자가 여럿이라 **순서를 바꿔도 컴파일이 통과할** 때
- 결과를 **불변**으로 만들고 싶은데 선택 항목이 많을 때
- 여러 필드에 걸친 **검증 규칙**이 있을 때

**GoF 빌더를 쓴다**
- 조립 **절차는 같은데 결과물 형식이 여럿**일 때 (문서 변환, 쿼리 생성, 파서)
- 그 절차가 복잡해서 한 곳에 모아 둘 값어치가 있을 때

**안 쓴다**
- 필드가 두셋뿐일 때 — 생성자로 충분하다. 빌더는 코드량을 두 배로 늘린다
- 전부 필수일 때 — 선택 항목이 없으면 빌더가 푸는 문제 자체가 없다
- 결과물 형식이 하나뿐일 때 (GoF 쪽) — Director와 인터페이스가 순수한 낭비가 된다

판단은 한 줄이다. **"호출부만 보고 각 인자가 무엇인지 알 수 있는가?"** 없다면 빌더를 볼 때다.
