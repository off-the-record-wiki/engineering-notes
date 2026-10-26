# 싱글턴 패턴 (Singleton Pattern)

> 인스턴스가 **하나뿐임을 클래스 자신이 보장**하고, 그 하나에 접근하는 통로를 제공하는 패턴.

애플리케이션 설정을 예제로 구현했다.

![싱글턴 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/singleton-pattern.png)

---

#### 1. 왜 필요한가

설정 파일을 읽는 객체가 있다고 하자. 그냥 쓰면 이렇게 된다.

```java
// 싱글턴을 쓰지 않았을 때
public class OrderService {
    private final AppConfig config = new AppConfig();   // 파일을 읽는다
}
public class PaymentService {
    private final AppConfig config = new AppConfig();   // 또 읽는다
}
public class ReportService {
    private final AppConfig config = new AppConfig();   // 또 읽는다
}
```

문제는 두 가지다.

**낭비** — 같은 파일을 세 번 읽고, 같은 내용을 세 벌 들고 있는다.

**불일치** — 더 중요한 쪽이다. 설정을 실행 중에 바꿀 수 있게 만들면, `OrderService`가 바꾼 값을 `PaymentService`는 모른다. **"같은 설정"이라는 말이 거짓이 된다.** 이게 커넥션 풀이나 캐시라면 더 심각해진다. 풀이 세 개면 커넥션 수 제한이 세 배가 된다.

싱글턴은 **하나만 존재한다는 사실을 클래스가 직접 책임진다.**

```java
public class AppConfig {
    private AppConfig() { ... }          // ① 바깥에서 new 할 수 없다
    public static AppConfig getInstance() { ... }   // ② 통로는 여기 하나뿐
}
```

`private` 생성자가 출발점이다. **인스턴스를 얻는 경로를 하나로 좁히지 않으면 나머지는 다 의미가 없다.**

---

#### 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Singleton** | 유일한 인스턴스를 스스로 관리하고 제공 | `AppConfig` |
| **Client** | `getInstance()`로 받아 쓴다 | `Main` |

참여자가 **하나뿐이다.** 이 패턴에는 인터페이스도, 위임할 상대도 없다. 그래서 배울 것은 "구조"가 아니라 **"어떻게 구현하느냐"** 다. 이 예제도 구현 방식 네 가지를 나란히 두는 데 초점을 맞췄다.

| 클래스 | 방식 | 비고 |
|---|---|---|
| `AppConfig` | LazyHolder (홀더) | 직접 구현할 때의 기본형 |
| `EagerAppConfig` | 이른 초기화 | 가장 짧다 |
| `EnumAppConfig` | enum | 가장 튼튼하다 |
| `UnsafeAppConfig` | 동기화 없는 게으른 초기화 | **깨지는 예** |

#### LazyHolder — 이 예제의 주력

```java
public class AppConfig {

    private final Map<String, String> values;

    private AppConfig() {
        this.values = load();               // 파일 읽기
    }

    private static class Holder {
        private static final AppConfig INSTANCE = new AppConfig();
    }

    public static AppConfig getInstance() {
        return Holder.INSTANCE;
    }
}
```

세 가지를 동시에 만족한다.

- **지연 초기화** — `AppConfig`가 로딩될 때 `Holder`는 **같이 로딩되지 않는다.** `Holder.INSTANCE`를 처음 읽는 순간에야 초기화된다.
- **스레드 안전** — 클래스 초기화는 JVM이 락을 걸어 **단 한 번만** 수행한다. 여러 스레드가 동시에 들어와도 하나는 초기화하고 나머지는 기다린다.
- **동기화 비용 0** — 우리가 `synchronized`를 한 글자도 쓰지 않았다.

핵심은 **안전장치를 우리가 만들지 않는다**는 점이다. JVM의 클래스 초기화 규칙에 얹었을 뿐이다.

#### 이른 초기화 — 가장 짧다

```java
public class EagerAppConfig {
    private static final EagerAppConfig INSTANCE = new EagerAppConfig();
    private EagerAppConfig() { }
    public static EagerAppConfig getInstance() { return INSTANCE; }
}
```

이것도 클래스 초기화에 얹은 것이라 스레드 안전하다. 대신 **쓰든 안 쓰든 만들어진다.** 생성 비용이 작다면 이걸로 충분하고, 파일·네트워크가 얽히거나 실행 중에 안 쓰일 수도 있으면 홀더 방식이 낫다.

#### enum — 가장 튼튼하다

```java
public enum EnumAppConfig {
    INSTANCE;

    private final Map<String, String> values;

    EnumAppConfig() { this.values = load(); }

    public String get(String key) { return values.get(key); }
}
```

홀더 방식으로도 막지 못하는 두 구멍을 **언어 차원에서** 막는다.

- **리플렉션** — `Constructor.setAccessible(true)`로 `private` 생성자를 뚫는 공격이 enum에는 통하지 않는다. JVM이 거부한다.
- **직렬화** — 일반 클래스는 역직렬화할 때마다 새 객체가 생긴다. enum은 이름으로 찾아오므로 그런 일이 없다.

제약은 하나다. enum은 클래스를 상속할 수 없고, 초기화 시점을 미룰 수 없다. 그게 걸리지 않는다면 이게 가장 간단하고 안전한 답이다.

> *Effective Java* 아이템 3이 권하는 방식이다. "원소가 하나인 열거 타입이 싱글턴을 만드는 가장 좋은 방법이다."

---

#### 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out singleton.code.Main
```

```
--- 1. LazyHolder 방식 ---
getInstance() 호출 전 — 아직 아무것도 안 만들어졌다
  (설정 파일을 읽는다 — 이 줄은 딱 한 번만 나온다)
같은 인스턴스인가? true
app.name = design-pattern-shop

--- 2. 이른 초기화(eager) 방식 ---
  (EagerAppConfig 생성 — 클래스가 로딩되는 시점이다)
같은 인스턴스인가? true

--- 3. enum 방식 ---
  (EnumAppConfig 생성 — 이것도 딱 한 번이다)
app.name = design-pattern-shop

--- 4. 동기화 없는 게으른 초기화 ---
8개 스레드가 동시에 getInstance() 호출
→ 만들어진 인스턴스: 8개  (싱글턴이라면 1개여야 한다)
```

두 곳을 보면 된다.

**"설정 파일을 읽는다"가 한 번만 찍힌다.** `getInstance()`를 두 번 불렀는데도 생성자는 한 번만 돌았다. 그리고 그 줄이 `getInstance()` **호출 전**이 아니라 **후**에 나온다는 게 지연 초기화의 증거다.

**마지막 줄이 8개다.** 이게 다음 절의 주제다.

---

#### 4. 동기화가 없으면 실제로 깨진다

```java
// UnsafeAppConfig — 흔히 처음 배우는 모양
private static UnsafeAppConfig instance;

public static UnsafeAppConfig getInstance() {
    if (instance == null) {                 // ① 검사
        instance = new UnsafeAppConfig();   // ② 생성
    }
    return instance;
}
```

싱글 스레드에서는 멀쩡히 동작한다. **그래서 테스트를 통과하고 운영에서 터진다.**

문제는 ①과 ② 사이가 열려 있다는 것이다. 생성자가 오래 걸릴수록(=설정 파일을 읽을수록) 그 틈은 넓어진다.

| | Thread A | Thread B |
|---|---|---|
| **①** | `instance == null` → 참 | `instance == null` → 참 |
| **②** | `new UnsafeAppConfig()` | `new UnsafeAppConfig()` |

둘 다 "없다"를 보고 각자 만든다. `Main`은 `CountDownLatch`로 8개 스레드를 같은 순간에 출발시켜 이걸 재현한다.

```java
CountDownLatch gate = new CountDownLatch(1);
for (int i = 0; i < 8; i++) {
    pool.submit(() -> {
        gate.await();                                   // 다 같이 대기하다가
        serials.add(UnsafeAppConfig.getInstance().serial());
        return null;
    });
}
gate.countDown();                                       // 한 번에 출발
```

결과는 **8개**다. 운이 나빠서가 아니라, 이 코드가 원래 그렇다.

`synchronized`를 `getInstance()`에 붙이면 고쳐지지만, **호출할 때마다** 락을 잡는다. 초기화는 딱 한 번인데 그 뒤 수백만 번의 호출이 계속 비용을 낸다. 그래서 나온 게 DCL(double-checked locking)이고, DCL은 `volatile`을 빠뜨리면 또 미묘하게 깨진다.

**홀더 방식은 이 문제를 아예 만들지 않는다.** 우리가 락을 다루지 않기 때문이다. 직접 구현할 거라면 홀더나 enum을 쓰고, DCL은 남의 코드에서 읽을 때만 알아보면 된다.

---

#### 5. 알아둘 점

#### 싱글턴은 다른 패턴과 방향이 반대다

지금까지 본 패턴들은 전부 **유연해지려고** 썼다. 전략은 갈아끼우려고, 템플릿 메서드는 확장하려고, 추상 팩토리는 제품군을 통째로 바꾸려고.

싱글턴은 그 반대다. **"하나만 있어야 한다"는 제약을 코드에 못 박는다.** 그래서 대가가 따른다.

| 얻는 것 | 잃는 것 |
|---|---|
| 유일성 보장 | 교체 가능성 |
| 어디서든 접근 | 테스트 독립성 |
| 생성 비용 절약 | 의존성의 명시성 |

#### 전역 상태의 문제

```java
public class OrderService {
    public void order() {
        String region = AppConfig.getInstance().get("region");   // 생성자에 안 보인다
    }
}
```

`OrderService`의 생성자만 봐서는 이 클래스가 `AppConfig`에 의존한다는 걸 알 수 없다. **의존성이 코드 안쪽에 숨는다.** 테스트에서 설정을 바꿔치기하려 해도 끼어들 자리가 없다.

주입하면 전부 드러난다.

```java
public class OrderService {
    private final AppConfig config;
    public OrderService(AppConfig config) { this.config = config; }   // 다 보인다
}
```

**객체가 하나인 것과, 그 하나를 전역에서 꺼내 쓰는 것은 다른 문제다.** 싱글턴은 이 둘을 묶어 버린다.

#### 상태를 가지면 위험해진다

이 예제의 `AppConfig`는 `final` 필드에 불변 `Map`을 담는다. 의도적이다.

```java
private final Map<String, String> values = Map.of(...);   // 읽기 전용
```

싱글턴이 **변경 가능한 상태**를 가지면 그건 전역 변수다. 모든 스레드가 동시에 건드리므로 필드마다 동기화를 따로 신경 써야 하고, 테스트는 실행 순서에 따라 결과가 달라진다.

> 싱글턴으로 만들 만한 것은 대체로 **불변 설정**이거나 **자기 내부를 스스로 동기화하는 자원 관리자**(커넥션 풀, 스레드 풀)다. 그 사이의 어중간한 것은 대개 싱글턴이면 안 된다.

#### 스프링을 쓴다면 직접 만들 일이 거의 없다

```java
@Component
public class AppConfig { ... }     // 컨테이너가 하나만 만들어 관리한다
```

스프링 빈은 기본이 싱글턴 스코프다. **유일성은 얻으면서 위의 단점은 대부분 피한다.** 생성자 주입이라 의존성이 드러나고, 테스트에서는 다른 구현을 주입할 수 있고, `private` 생성자도 `getInstance()`도 필요 없다.

그래서 실무에서 이 패턴을 손으로 구현하는 경우는 프레임워크 밖(라이브러리, 유틸리티 초기화)에 한정된다. 그래도 알아야 하는 이유는, **스프링이 하는 일이 바로 이것**이기 때문이다.

#### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 진짜 파일 I/O (`Properties`, 클래스패스 리소스 로딩)
- 리플렉션 방어 — 생성자에서 `if (instance != null) throw ...` (enum이면 불필요)
- 직렬화 대응 — `readResolve()` (enum이면 불필요)
- 설정 리로드 — 실행 중 갱신이 필요하면 싱글턴보다 별도 컴포넌트가 낫다

---

#### 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 물리적으로 **하나여야만** 하는 자원일 때 (커넥션 풀, 스레드 풀, 파일 락)
- 불변 설정처럼 여러 벌 존재할 이유가 없고, 상태도 없을 때
- 프레임워크 밖이라 컨테이너의 도움을 받을 수 없을 때

**안 쓴다**
- **"하나면 충분하다"** 일 뿐일 때 — 그냥 하나 만들어 주입하면 된다. 굳이 못 박을 이유가 없다
- 변경 가능한 상태를 가질 때 — 전역 변수가 된다
- 테스트에서 바꿔치기해야 할 때 — `getInstance()`는 끼어들 자리를 주지 않는다
- 스프링 같은 DI 컨테이너를 쓰고 있을 때 — 이미 해 주고 있다

판단은 한 줄이다. **"하나여야만 하는가, 아니면 하나면 충분한가?"** 후자라면 싱글턴이 아니다.
