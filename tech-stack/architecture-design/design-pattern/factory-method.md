# 팩토리 메서드 패턴 (Factory Method Pattern)

> 객체를 만드는 일을 메서드로 열어 두고, **무엇을 만들지는 하위 클래스가 답하게** 하는 패턴.

알림 발송을 예제로 구현했다.

![팩토리 메서드 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/factory-method-pattern.png)

---

## 1. 왜 필요한가

주문 알림을 이메일·문자·푸시로 보내야 한다. 채널이 늘어날 때 이런 코드가 생긴다.

```java
// 팩토리 메서드를 쓰지 않았을 때
public void send(String type, String to, String message) {
    if (to.isBlank()) throw new IllegalArgumentException("수신자가 없다");

    NotificationChannel channel;
    if (type.equals("EMAIL"))      channel = new EmailChannel("shop@example.com");
    else if (type.equals("SMS"))   channel = new SmsChannel("1588-0000");
    else if (type.equals("PUSH"))  channel = new PushChannel("APP-KEY-77");
    else throw new IllegalArgumentException(type);

    String body = fit(message, channel.maxLength());
    channel.send(to, body);
    log(channel.name());
}
```

문제는 **채널이 하나 늘 때마다 잘 돌아가던 `send()`를 다시 연다**는 것이다. 검증도 길이 맞춤도 기록도 그대로인데, `new` 하는 부분 때문에 메서드 전체가 회귀 위험에 노출된다.

그리고 이 메서드는 **모든 채널의 생성 방법을 안다.** 발신 주소, 발신 번호, 앱 키까지 전부 여기 모인다.

팩토리 메서드는 그 `new`만 떼어내 **구멍을 뚫는다.**

```java
// 팩토리 메서드를 썼을 때 — 분기가 사라진다
public final void send(String to, String message) {
    if (to == null || to.isBlank()) throw new IllegalArgumentException("수신자가 없다");

    NotificationChannel channel = createChannel();   // ← 무엇이 오는지 모른다

    String body = fit(message, channel.maxLength());
    channel.send(to, body);
    log(channel.name());
}

protected abstract NotificationChannel createChannel();
```

`if`가 사라진 게 아니라 **하위 클래스로 옮겨간 것**이다. 그리고 옮겨간 자리에서는 분기가 아니라 그냥 `return`이 된다.

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Product** | 팩토리 메서드가 돌려주는 타입 | `NotificationChannel` |
| **ConcreteProduct** | 실제 제품 | `EmailChannel` · `SmsChannel` · `PushChannel` |
| **Creator** | 제품을 **쓰는 로직** + 팩토리 메서드 선언 | `NotificationSender` |
| **ConcreteCreator** | 팩토리 메서드를 재정의 | `EmailNotificationSender` · `SmsNotificationSender` · `PushNotificationSender` |

**두 계층이 짝을 이룬다.** Creator 하나에 Product 하나. 이 평행 구조가 이 패턴의 생김새다.

### Creator는 "공장 클래스"가 아니다

이 패턴에서 가장 오해받는 자리다.

```java
public abstract class NotificationSender {

    protected abstract NotificationChannel createChannel();     // 팩토리 메서드

    public final void send(String to, String message) {         // ← 본업은 이쪽이다
        ...
        NotificationChannel channel = createChannel();
        ...
    }
}
```

`NotificationSender`의 본업은 **알림을 보내는 것**이다. 객체를 만들어 주는 게 아니다. 보내는 과정에 채널이 필요하니까 하위 클래스에게 물어볼 뿐이다.

> Creator에 팩토리 메서드밖에 없다면, 그건 팩토리 메서드 패턴이 아니라 그냥 **팩토리 클래스**다. 제품을 쓰는 로직이 있어야 이 패턴이 성립한다.

그래서 팩토리 메서드는 대개 혼자 있지 않고 **[템플릿 메서드](template-method.md) 안에서 호출된다.** GoF 책도 그렇게 설명한다. 이 예제의 `send()`가 정확히 템플릿 메서드다 — 절차를 `final`로 고정하고, 그 안의 한 단계를 하위 클래스에게 맡긴다.

### ConcreteCreator는 짧다

```java
public class SmsNotificationSender extends NotificationSender {

    private final String senderNumber;

    public SmsNotificationSender(String senderNumber) {
        this.senderNumber = senderNumber;
    }

    @Override
    protected NotificationChannel createChannel() {
        return new SmsChannel(senderNumber);
    }
}
```

하는 일은 **팩토리 메서드를 재정의하는 것뿐**이다. 이게 빈약해 보인다면 정상이다. 발송 절차는 전부 부모에게 물려받았기 때문이다.

제품을 만드는 데 필요한 설정(발신 번호)을 ConcreteCreator가 들고 있는 것도 자연스럽다. **클라이언트는 이 값의 존재를 알 필요가 없다.**

### 팩토리 메서드는 `protected`다

```java
protected abstract NotificationChannel createChannel();
```

바깥에서 부르라고 만든 API가 아니라 **하위 클래스에게 열어 준 확장점**이다. `public`으로 열면 클라이언트가 `send()`를 건너뛰고 `createChannel()`만 부를 수 있게 되고, 절차를 강제한 의미가 사라진다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out factory_method.code.Main
```

```
--- 1. 같은 절차, 다른 채널 ---
    [이메일] shop@example.com → user@example.com : 주문 ORD-1001 결제가 완료되었습니다.
    [기록] 이메일 발송 완료 (24자)
    [SMS] 1588-0000 → user@example.com : 주문 ORD-1001 결제가 완료되었습니다.
    [기록] SMS 발송 완료 (24자)
    [푸시] app=APP-KEY-77 → user@example.com : 주문 ORD-1001 결제가 완료되었습니다.
    [기록] 푸시 발송 완료 (24자)

--- 2. 제품의 사정에 절차가 맞춰진다 ---
    [이메일] … 문의는 고객센터로 연락 주세요.
    [기록] 이메일 발송 완료 (87자)
    [SMS] 1588-0000 → user@example.com : 주문 ORD-1001 결제가 완료되었습니다. 배송은 영업일 기준 2일 이내 시작…
    [기록] SMS 발송 완료 (45자)
    [푸시] … 문의는 고객센터로 연락 주세요.
    [기록] 푸시 발송 완료 (87자)

--- 3. 새 채널을 붙여도 기존 코드는 그대로 ---
    [슬랙] #cs → user@example.com : 주문 ORD-1001 결제가 완료되었습니다.
    [기록] 슬랙 발송 완료 (24자)
→ NotificationSender.send() 는 한 줄도 수정되지 않았다.
```

**1번의 호출부에 분기가 없다.**

```java
for (NotificationSender sender : senders) {
    sender.send("user@example.com", SHORT);
}
```

**2번이 이 패턴의 값어치다.** 같은 메시지인데 SMS만 45자에서 잘렸다. 자르는 코드는 `NotificationSender`에 한 번만 있고, **몇 자까지인지는 제품이 답한다.**

```java
String body = fit(message, channel.maxLength());
```

Creator는 제품이 무엇인지 모르면서도 제품의 사정에 맞춘다. 이게 "제품을 쓰는 로직"이 Creator에 있다는 말의 의미다.

---

## 4. 새 채널을 추가하려면

```java
public class KakaoChannel implements NotificationChannel {
    @Override public String name()   { return "카카오"; }
    @Override public int maxLength() { return 1_000; }
    @Override public void send(String to, String body) { ... }
}

public class KakaoNotificationSender extends NotificationSender {
    @Override
    protected NotificationChannel createChannel() {
        return new KakaoChannel(...);
    }
}
```

**클래스 두 개 추가가 전부다.** `NotificationSender`도, 기존 채널도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

`Main`의 3번은 이걸 익명 클래스로 즉석에서 보여 준다. 새 채널을 붙였는데 `send()`는 한 줄도 바뀌지 않았다.

---

## 5. 알아둘 점

### "팩토리"라 불리는 세 가지를 구분해야 한다

이 패턴이 유독 헷갈리는 이유는 **이름이 겹치는 것이 셋**이기 때문이다.

**① 단순 팩토리 (Simple Factory) — 패턴이 아니다**

```java
public static NotificationChannel create(String type) {
    switch (type) {
        case "EMAIL" -> return new EmailChannel(...);
        case "SMS"   -> return new SmsChannel(...);
    }
}
```

가장 흔히 "팩토리 패턴"이라고 부르는 것이지만 **GoF 목록에 없다.** 생성 코드를 한곳에 모아 주긴 하지만, **종류가 늘 때마다 이 `switch`를 다시 연다.** OCP를 만족하지 못한다.

**② 팩토리 메서드 (이 예제) — GoF 패턴**

```java
protected abstract NotificationChannel createChannel();
```

하위 클래스가 오버라이드로 답한다. **`switch`가 없다.** 새 종류는 새 클래스로 추가된다.

**③ 정적 팩토리 메서드 (Static Factory Method) — 관용구**

```java
List.of(1, 2, 3);
Integer.valueOf("42");
LocalDate.now();
Order.builder(...);
```

*Effective Java* 아이템 1이다. 생성자 대신 **이름 있는 메서드**로 객체를 만드는 기법이고, **GoF 패턴과는 무관하다.** 이름만 비슷하다.

> 구분하는 가장 빠른 방법: **`switch`나 `if`로 타입을 고르고 있으면 GoF 팩토리 메서드가 아니다.** 그리고 `static`이면 오버라이드할 수 없으므로 역시 아니다.

### 추상 팩토리와의 경계

| | 팩토리 메서드 | [추상 팩토리](abstract-factory.md) |
|---|---|---|
| 만드는 것 | 제품 **하나** | 서로 어울리는 제품 **한 벌** |
| 생성 메서드 개수 | 1개 | 제품 종류만큼 (N개) |
| 수단 | **상속** — 하위 클래스가 결정 | **합성** — 팩토리를 주입 |
| 막아 주는 것 | 구체 클래스 직접 생성 | **제품군이 섞이는 것** |

`NotificationSender`에 `createChannel()` 말고 `createFormatter()`가 하나 더 생기고, **그 둘이 짝이 맞아야 한다면** 그때부터 추상 팩토리다.

실무에서는 **추상 팩토리의 각 메서드가 팩토리 메서드**이기도 하다. 둘은 배타적이지 않다.

### 상속이 수단이라는 것의 대가

```java
new EmailNotificationSender("shop@example.com")   // 컴파일 타임에 확정
```

Creator를 고르는 순간 제품이 결정된다. **실행 중에 바꿀 수 없다.** 전략 패턴처럼 갈아끼우려면 Creator 자체를 새로 만들어야 한다.

그리고 제품 하나에 Creator 하나씩, **클래스가 짝으로 늘어난다.** 채널 3개면 클래스 6개다. 종류가 둘뿐이고 늘 일이 없다면 그냥 `NotificationChannel`을 생성자로 주입받는 편이 낫다.

```java
// 이걸로 충분한 경우가 많다
public NotificationSender(NotificationChannel channel) { this.channel = channel; }
```

**팩토리 메서드가 이것보다 나은 경우는 하나다** — 제품 생성에 Creator만 아는 정보가 필요하거나, 제품을 **매번 새로** 만들어야 할 때. 주입은 인스턴스를 고정하지만 팩토리 메서드는 호출할 때마다 만든다.

### 제품이 매번 새로 만들어진다

```java
public final void send(String to, String message) {
    NotificationChannel channel = createChannel();   // 호출할 때마다 new
```

이 예제의 채널은 가벼워서 문제없지만, 생성 비용이 크다면 ConcreteCreator에서 캐싱하면 된다.

```java
@Override
protected NotificationChannel createChannel() {
    return cachedChannel;    // 미리 만들어 둔 것을 돌려줘도 규격은 지켜진다
}
```

**팩토리 메서드는 "새로 만들라"고 요구하지 않는다.** "제품을 내놓으라"고 할 뿐이다. 정적 팩토리 메서드가 캐싱하는 것(`Integer.valueOf`)과 같은 자유다.

### 자바·스프링에서의 모습

```java
Collection.iterator()              // 컬렉션마다 다른 Iterator를 만든다 — 교과서적인 예
Calendar.getInstance()             // (실제로는 단순 팩토리에 가깝다)
```

`Collection.iterator()`가 정확한 예다. `ArrayList`는 `ArrayList.Itr`을, `LinkedList`는 `LinkedList.ListItr`을 돌려준다. **`for-each` 문은 어떤 컬렉션인지 모른 채 `iterator()`만 부른다.**

스프링에서는 `FactoryBean<T>`의 `getObject()`가 같은 자리다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 발송 실패 처리와 재시도 — `channel.send()`가 던지는 예외
- 채널별 인증·설정 로딩 (지금은 생성자로 문자열만 받는다)
- 글자 수 계산 — 실제 SMS는 인코딩에 따라 바이트 단위로 센다
- 발송 이력 저장 (지금은 콘솔 출력)
- 여러 채널로 동시에 보내기 — 그건 이 패턴이 아니라 [옵저버](observer.md)나 조합의 일이다

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 제품을 **쓰는 절차는 같은데** 어떤 제품을 쓸지가 갈릴 때
- 제품 생성에 **Creator만 아는 정보**가 필요할 때 (발신 번호, 앱 키)
- 프레임워크·라이브러리에서 **확장점을 열어 줄** 때 — 사용자가 하위 클래스로 답한다
- 제품을 호출할 때마다 **새로** 만들어야 할 때

**안 쓴다**
- 제품 종류가 둘뿐이고 늘 일이 없을 때 — 클래스만 짝으로 늘고 얻는 게 없다
- 실행 중에 제품을 바꿔야 할 때 — 상속은 런타임에 못 바꾼다. 전략 패턴을 쓴다
- Creator에 **제품을 쓰는 로직이 없을** 때 — 그건 그냥 팩토리 클래스다. 정적 팩토리 메서드로 충분하다
- 만들어야 할 제품이 **여럿이고 짝이 맞아야** 할 때 — 추상 팩토리다

판단은 한 줄이다. **"제품을 쓰는 로직이 있고, 그 안에서 제품 종류만 갈리는가?"** 그렇다면 팩토리 메서드다.
