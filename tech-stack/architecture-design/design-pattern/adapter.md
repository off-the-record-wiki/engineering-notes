# 어댑터 패턴 (Adapter Pattern)

> 고칠 수 없는 외부 시스템을 **우리가 원하는 규격으로 번역**해서, 서로 맞물리게 만드는 패턴.

결제 연동을 예제로 구현했다.

![어댑터 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/adapter-pattern.png)

---

## 1. 왜 필요한가

결제 수단이 둘이다. 하나는 사내 레거시, 하나는 해외 PG SDK. **둘 다 우리가 고칠 수 없다.**

```java
// 레거시 — 금액과 가맹점 번호를 받고, 결과를 정수 코드로 준다
int code = legacyApi.requestApproval(25_000, "M-2024");
String approvalNo = legacyApi.getLastApprovalNo();   // 승인번호는 두 번째 호출로 따로

// 해외 PG — 센트 단위, 실패는 예외로 던진다
String raw = overseasApi.charge("USD", 1_785);       // "ok:CH-90001"
```

어댑터 없이 쓰면 이렇게 된다.

```java
// 어댑터를 쓰지 않았을 때
public void checkout(String orderId, long amount, String pgType) {
    if (pgType.equals("LEGACY")) {
        int code = legacyApi.requestApproval(amount, "M-2024");
        if (code != 0) { ... }
        String no = legacyApi.getLastApprovalNo();
        ...
    } else {
        try {
            String raw = overseasApi.charge("USD", amount * 100 / 1400);
            String no = raw.substring(3);
            ...
        } catch (OverseasPayApi.DeclinedException e) { ... }
    }
}
```

문제는 분기가 생긴 게 아니다. **외부 시스템의 사정이 우리 코드 안으로 새어 들어온 것**이다.

- 우리 결제 코드가 **센트 단위**와 **정수 코드 51**을 안다
- SDK의 **예외 타입**이 우리 도메인 코드에 등장한다
- 레거시의 **"두 번 호출해야 한다"는 규약**을 클라이언트가 지켜야 한다
- PG사를 하나 더 붙이면 이 메서드를 또 연다

어댑터는 그 사정을 **경계에서 막는다.**

```java
// 어댑터를 썼을 때 — 클라이언트는 우리 언어만 쓴다
PaymentResult result = gateway.pay(orderId, amountInWon);
if (result.success()) { ... }
```

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Target** | 우리가 원하는 규격 | `PaymentGateway` |
| **Adaptee** | 고칠 수 없는 외부 시스템 | `LegacyCardApi` · `OverseasPayApi` |
| **Adapter** | 둘 사이를 번역 | `LegacyCardAdapter` · `OverseasPayAdapter` |
| **Client** | Target만 안다 | `CheckoutService` · `Main` |

### Target을 먼저 정한다 — 순서가 중요하다

```java
public interface PaymentGateway {
    PaymentResult pay(String orderId, long amountInWon);
}
```

이 규격은 SDK 모양을 보고 정한 게 아니다. **우리 도메인의 언어**로 정했다. 주문번호와 원화 금액.

순서가 뒤바뀌면 패턴이 무너진다. SDK를 먼저 보고 "센트를 받으니까 우리도 센트로 할까" 하는 순간, **외부 SDK가 우리 도메인을 지배하기 시작한다.** 그러면 SDK를 바꿀 때 도메인 전체가 흔들린다.

### Adapter — 하는 일은 번역뿐이다

```java
public class LegacyCardAdapter implements PaymentGateway {

    private final LegacyCardApi api;        // 합성 — 어댑티를 필드로 가진다
    private final String merchantId;

    @Override
    public PaymentResult pay(String orderId, long amountInWon) {
        int code = api.requestApproval(amountInWon, merchantId);        // ① 이름·파라미터

        if (code != LegacyCardApi.OK) {
            return PaymentResult.declined("레거시 승인 거절 (코드 " + code + ")");   // ② 결과 규약
        }
        return PaymentResult.approved(api.getLastApprovalNo());         // ③ 호출 규약
    }
}
```

```java
public class OverseasPayAdapter implements PaymentGateway {

    @Override
    public PaymentResult pay(String orderId, long amountInWon) {
        long cents = amountInWon * 100 / WON_PER_USD;                   // ④ 단위

        try {
            String raw = api.charge(CURRENCY, cents);
            return PaymentResult.approved(raw.substring("ok:".length()));  // ⑤ 응답 형식

        } catch (OverseasPayApi.DeclinedException e) {
            return PaymentResult.declined("해외 PG 거절 (" + e.getMessage() + ")");  // ⑥ 예외
        }
    }
}
```

두 어댑터가 **하는 변환이 서로 전혀 다르다.** 어댑티가 어긋나는 방식이 다르기 때문이다. 그래도 바깥에서 보면 둘은 똑같은 `PaymentGateway`다.

### 어댑터가 하는 다섯 가지 번역

| 종류 | 예 |
|---|---|
| **이름 · 파라미터** | `requestApproval(won, mid)` → `pay(orderId, won)` |
| **단위** | 센트 → 원 |
| **결과 규약** | `int` 코드 → `PaymentResult` |
| **실패 표현** | `DeclinedException` → 반환값 |
| **호출 규약** | 두 번 호출 → 한 번 호출 |

이 중 **실패 표현의 통일**이 실무에서 가장 값어치 있다. SDK 예외 타입이 우리 코드 바깥으로 새어 나가지 않기 때문이다. 나중에 SDK를 교체해도 `catch` 블록을 찾아다닐 일이 없다.

### 어댑터가 버리는 것도 있다

```java
public PaymentResult pay(String orderId, long amountInWon) {
    int code = api.requestApproval(amountInWon, merchantId);   // orderId를 넣을 자리가 없다
```

레거시는 주문번호를 받지 못한다. 어댑터가 **버린다.** 규격이 완벽히 겹치는 일은 드물고, 이런 손실을 어디서 흡수할지 결정하는 것도 어댑터의 일이다.

> 반대로 어댑티가 요구하는데 Target에 없는 값(`merchantId`)은 **어댑터가 생성자로 받아 들고 있는다.** 클라이언트가 알 필요 없는 정보이기 때문이다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out adapter.code.Main
```

```
--- 1. 서로 다른 두 시스템을 같은 코드로 쓴다 ---
[국내 레거시]
  ORD-1001 25,000원 → 승인 (KR-10001)
[해외 PG]
  ORD-1002 25,000원 → 승인 (CH-90001)

--- 2. 실패도 같은 모양으로 돌아온다 ---
[국내 레거시] 원래는 정수 코드로 알린다
  ORD-1003 2,000,000원 → 실패 (레거시 승인 거절 (코드 51))
[해외 PG] 원래는 예외를 던진다
  ORD-1004 2,000,000원 → 실패 (해외 PG 거절 (insufficient funds))

--- 3. 클라이언트 코드는 완전히 동일하다 ---
  ORD-1005 50,000원 → 승인 (KR-10002)
  ORD-1005 50,000원 → 승인 (CH-90002)
```

**2번이 핵심이다.** 한쪽은 정수 코드로, 한쪽은 예외로 실패를 알렸는데 클라이언트가 보는 것은 `result.success()` 하나뿐이다.

3번의 반복문에는 분기가 없다.

```java
for (PaymentGateway gateway : new PaymentGateway[] {domestic, overseas}) {
    new CheckoutService(gateway).checkout("ORD-1005", 50_000);
}
```

---

## 4. 새 PG사를 추가하려면

```java
public class NewPgAdapter implements PaymentGateway {

    private final NewPgSdk sdk;

    @Override
    public PaymentResult pay(String orderId, long amountInWon) {
        NewPgSdk.Response res = sdk.execute(orderId, amountInWon);
        return res.isOk()
            ? PaymentResult.approved(res.getTid())
            : PaymentResult.declined(res.getReason());
    }
}
```

**어댑터 클래스 하나 추가 + 조립하는 곳 한 줄.** `CheckoutService`도, 기존 어댑터도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

---

## 5. 알아둘 점

### 객체 어댑터와 클래스 어댑터

GoF는 두 가지를 제시한다.

```java
// 객체 어댑터 — 어댑티를 '가진다' (합성)
public class LegacyCardAdapter implements PaymentGateway {
    private final LegacyCardApi api;
}

// 클래스 어댑터 — 어댑티를 '상속한다'
public class LegacyCardAdapter extends LegacyCardApi implements PaymentGateway {
}
```

**자바에서는 객체 어댑터가 사실상 유일한 선택지다.**

- 자바는 다중 상속이 안 되므로, 어댑티를 상속하면 다른 부모를 가질 수 없다
- 어댑티가 `final`이면 상속 자체가 불가능하다
- 상속하면 어댑티의 `public` 메서드가 **어댑터 바깥으로 그대로 노출된다.** 감추려고 만든 어댑터인데 `requestApproval()`을 클라이언트가 직접 부를 수 있게 된다

마지막 항목이 결정적이다. 클래스 어댑터는 **캡슐화를 스스로 깬다.**

### 어댑터에 로직을 넣지 않는다

가장 흔한 실수다. 어댑터는 번역만 해야 한다.

```java
// 이러면 어댑터가 아니다
public PaymentResult pay(String orderId, long amountInWon) {
    if (amountInWon > 500_000) {                 // ← 결제 정책
        return PaymentResult.declined("한도 초과");
    }
    for (int i = 0; i < 3; i++) { ... }          // ← 재시도
    auditLog.write(orderId);                     // ← 로깅
}
```

한도 정책은 도메인의 일이고, 재시도와 로깅은 [데코레이터](decorator.md)의 일이다. 어댑터가 판단을 시작하는 순간 **테스트하기 어려운 잡탕**이 된다.

> 판단 기준: **"어댑티를 다른 것으로 바꿔도 이 코드가 필요한가?"** 필요하다면 그건 어댑터의 일이 아니다.

### 데코레이터·프록시·파사드와의 차이

넷 다 "감싼다"는 점이 같아서 구조만 보면 구분이 안 된다. **인터페이스를 어떻게 하느냐**로 갈린다.

| 패턴 | 인터페이스 | 목적 |
|---|---|---|
| **어댑터** | **바꾼다** | 안 맞는 것을 맞물리게 |
| 데코레이터 | 유지한다 | 기능을 더하려고 |
| 프록시 | 유지한다 | 접근을 통제하려고 (지연 생성, 권한, 캐싱) |
| 파사드 | 발명한다 | 복잡한 여러 개를 단순하게 |

어댑터와 파사드가 특히 헷갈린다. 둘 다 "쓰기 편한 인터페이스"를 준다. 차이는 **기준이 어디 있느냐**다.

- **어댑터** — 맞춰야 할 **기존 규격이 있다.** `PaymentGateway`가 먼저 존재하고 거기 맞춘다
- **파사드** — 맞출 규격이 **없다.** 복잡한 서브시스템을 보고 편한 인터페이스를 새로 만든다

### 자바 표준 라이브러리의 어댑터

```java
Reader reader = new InputStreamReader(inputStream);   // 바이트 스트림 → 문자 스트림
List<String> list = Arrays.asList(array);             // 배열 → List
Enumeration<String> e = Collections.enumeration(list);// Collection → 레거시 Enumeration
```

`InputStreamReader`가 교과서적이다. `InputStream`(바이트)과 `Reader`(문자)는 **애초에 다른 규격**인데, 이 클래스가 사이에 서서 번역한다. [데코레이터](decorator.md)인 `BufferedInputStream`과 나란히 놓고 보면 차이가 분명하다 — 버퍼드는 `InputStream`을 받아 `InputStream`을 내놓고(유지), 리더는 `InputStream`을 받아 `Reader`를 내놓는다(변경).

### 어댑터는 테스트 경계이기도 하다

외부 SDK는 테스트하기 어렵다. 네트워크가 필요하고, 실패 상황을 만들기도 힘들다. 어댑터가 있으면 **경계가 명확해진다.**

- `CheckoutService` 테스트 — `PaymentGateway`를 가짜로 주입하면 끝난다. SDK가 아예 필요 없다
- 어댑터 테스트 — "정수 코드 51이 오면 `declined`가 되는가" 같은 **번역 규칙**만 검증한다

어댑터에 로직을 넣지 말라는 이유가 여기서도 나온다. 로직이 섞이면 이 두 테스트가 다시 엉킨다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 환율을 상수로 박아 둔 것 (`WON_PER_USD = 1_400`) — 실제로는 조회해야 하고, 그 순간 어댑터가 다른 의존성을 갖는다
- 금액 값 객체 (`long` 대신 `Money`) — 원/센트를 타입으로 구분하면 단위 사고가 막힌다
- 예상 못 한 예외 처리 — 지금은 `DeclinedException`만 잡는다. 네트워크 오류는 그대로 올라간다
- 레거시의 "두 번 호출" 규약에 대한 동시성 보호 — 이 예제는 단일 스레드를 가정한다
- 타임아웃·재시도 — 어댑터가 아니라 데코레이터로 얹을 일이다

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- **고칠 수 없는 코드**를 써야 할 때 (외부 SDK, 레거시, 다른 팀 모듈)
- 같은 역할의 외부 시스템이 **여럿이고 규격이 제각각**일 때
- 외부 시스템이 바뀔 가능성이 있을 때 — 고칠 파일을 어댑터 하나로 좁혀 둔다
- 테스트에서 외부 의존을 끊어내야 할 때

**안 쓴다**
- 양쪽 다 **우리가 고칠 수 있을** 때 — 그냥 규격을 맞추면 된다. 어댑터는 그때 순수한 낭비다
- 외부 시스템이 하나뿐이고 바뀔 일도 없을 때 — 직접 쓰는 게 읽기 쉽다
- 번역이 아니라 **기능을 더하고 싶을** 때 — 데코레이터다
- 맞출 규격이 없을 때 — 파사드다

이 패턴이 실무에서 값어치를 하는 순간은 하나로 요약된다. **외부 시스템이 바뀌었을 때 고쳐야 할 파일이 어댑터 하나뿐인 것.**
