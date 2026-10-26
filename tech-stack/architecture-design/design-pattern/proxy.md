# 프록시 패턴 (Proxy Pattern)

> 실제 객체와 **같은 얼굴로 앞에 서서**, 그 객체로 가는 길을 통제하는 패턴.

리포트 조회를 예제로 구현했다.

![프록시 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/proxy-pattern.png)

---

## 1. 왜 필요한가

월별 매출 리포트를 조회하는 서비스가 있다. 두 가지가 무겁다 — **커넥션을 잡는 것**과 **집계 쿼리를 도는 것**.

여기에 요구가 하나씩 붙는다.

- 리포트 화면에 안 들어가는 사용자도 많다 → **쓸 때까지 커넥션을 잡지 말자**
- 매출은 아무나 보면 안 된다 → **매니저만 조회할 수 있게 하자**
- 같은 달을 여러 번 조회한다 → **두 번째부터는 캐시를 쓰자**

이걸 실제 객체 안에 넣으면 이렇게 된다.

```java
// 프록시를 쓰지 않았을 때
public class DatabaseReportService {

    private final Map<String, String> cache = new HashMap<>();   // ← 캐시 관심사
    private Connection connection;                                // ← 지연 생성 관심사

    public String monthlyReport(String month, User user) {        // ← 시그니처가 오염된다
        if (!user.canReadReport()) {                              // ← 권한 관심사
            throw new SecurityException("권한 없음");
        }
        String cached = cache.get(month);
        if (cached != null) return cached;

        if (connection == null) connection = connect();

        String result = ... 집계 ...;
        cache.put(month, result);
        return result;
    }
}
```

**리포트를 집계하는 코드는 세 줄인데 나머지가 전부 다른 관심사다.** 그리고 권한 때문에 `User`가 파라미터로 들어오면서, 이 클래스를 배치 작업에서 쓰려면 가짜 사용자를 만들어 넘겨야 한다.

프록시는 이것들을 **바깥으로 꺼낸다.**

```java
// 프록시를 썼을 때 — 실제 객체는 집계만 한다
public class DatabaseReportService implements ReportService {
    @Override
    public String monthlyReport(String month) {
        return ... 집계 ...;
    }
}
```

권한·캐시·지연 생성은 앞에 선 프록시가 맡는다. **실제 객체는 프록시의 존재를 모른다.**

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Subject** | 실제 객체와 프록시가 공유하는 규격 | `ReportService` |
| **RealSubject** | 진짜 일을 하는 쪽 | `DatabaseReportService` |
| **Proxy** | 같은 얼굴로 앞에 서서 통제 | `LazyReportProxy` · `AuthReportProxy` · `CachingReportProxy` |
| **Client** | 둘을 구분하지 못한다 | `Main` |

### Subject가 있어야 성립한다

```java
public interface ReportService {
    String monthlyReport(String month);
}
```

프록시가 진짜인 척할 수 있는 **유일한 근거**다. 이 인터페이스가 없으면 클라이언트가 `DatabaseReportService` 타입을 직접 알아야 하고, 그 순간 프록시를 끼워 넣을 자리가 사라진다.

### 프록시의 종류 — GoF가 드는 것들

이 패턴은 구조가 단순해서, 배울 것은 **무엇을 통제하느냐**다.

| 종류 | 통제하는 것 | 이 예제 |
|---|---|---|
| **가상 프록시** (virtual) | **언제 만들지** | `LazyReportProxy` |
| **보호 프록시** (protection) | **누가 부를지** | `AuthReportProxy` |
| **스마트 참조** (캐싱) | **부를지 말지** | `CachingReportProxy` |
| **원격 프록시** (remote) | **어디에 있는지** | — (RMI, gRPC 스텁) |

**가상 프록시 — 생성을 미룬다**

```java
public class LazyReportProxy implements ReportService {

    private final String connectionUrl;
    private DatabaseReportService real;            // 아직 없다

    @Override
    public String monthlyReport(String month) {
        if (real == null) {
            real = new DatabaseReportService(connectionUrl);   // 첫 요청 때 만든다
        }
        return real.monthlyReport(month);
    }
}
```

주목할 점은 **감쌀 대상을 생성자로 받지 않는다**는 것이다. 스스로 만든다. 그래서 "언제 만들지"를 통제할 수 있다.

**보호 프록시 — 위임 자체를 막는다**

```java
@Override
public String monthlyReport(String month) {
    if (!user.canReadReport()) {
        throw new SecurityException("리포트 조회 권한이 없습니다");   // 실제 객체를 안 부른다
    }
    return target.monthlyReport(month);
}
```

**캐싱 프록시 — 두 번째부터 안 부른다**

```java
@Override
public String monthlyReport(String month) {
    String cached = cache.get(month);
    if (cached != null) {
        return cached;                        // 실제 객체를 안 부른다
    }
    String result = target.monthlyReport(month);
    cache.put(month, result);
    return result;
}
```

셋의 공통점이 이 패턴의 정의다. **실제 객체를 부르지 않는 경로가 있다.**

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out proxy.code.Main
```

```
--- 1. 가상 프록시 — 쓸 때까지 만들지 않는다 ---
    [가상 프록시] 준비 완료 — 실제 객체는 아직 없다
  프록시만 만들었다. 커넥션 로그가 없다.
  첫 요청:
    [가상 프록시] 첫 요청 — 이제야 실제 객체를 만든다
      (커넥션 준비 — jdbc:postgresql://report-db · 무겁다)
      (집계 쿼리 #1 실행 — 무겁다)
  → 2026-07 매출 47,000,000원

--- 2. 보호 프록시 — 권한이 없으면 부르지 않는다 ---
  [매니저]
    [보호 프록시] 김하나(MANAGER) 통과
      (집계 쿼리 #1 실행 — 무겁다)
  [일반 직원]
    [보호 프록시] 이서준(STAFF) 권한 없음 — 실제 객체를 부르지 않는다
  → 거절: 리포트 조회 권한이 없습니다
  집계 쿼리 로그가 한 번만 찍혔다 — 실제 객체는 두 번째를 모른다.

--- 3. 캐싱 프록시 — 두 번째부터 부르지 않는다 ---
    [캐싱 프록시] '2026-07' 캐시 없음 — 위임한다
      (집계 쿼리 #1 실행 — 무겁다)
    [캐싱 프록시] '2026-07' 캐시 적중 — 실제 객체를 부르지 않는다
    [캐싱 프록시] '2026-08' 캐시 없음 — 위임한다
      (집계 쿼리 #2 실행 — 무겁다)
```

**증거는 로그의 부재다.**

- 1번 — 프록시를 만들었는데 커넥션 로그가 없다
- 2번 — 두 번 요청했는데 집계 쿼리는 **한 번**만 나갔다
- 3번 — 세 번 요청했는데 집계 쿼리는 **두 번**만 나갔다. 돌아온 값은 셋 다 같다

`DatabaseReportService`는 자기가 건너뛰어졌다는 사실조차 모른다.

### 프록시는 겹칠 수 있다

```java
ReportService stacked =
    new AuthReportProxy(
        new CachingReportProxy(
            new LazyReportProxy(DB_URL)), manager);
```

```
[보호 프록시] 김하나(MANAGER) 통과
[캐싱 프록시] '2026-09' 캐시 없음 — 위임한다
[가상 프록시] 첫 요청 — 이제야 실제 객체를 만든다
      (집계 쿼리 #1 실행)

[보호 프록시] 김하나(MANAGER) 통과
[캐싱 프록시] '2026-09' 캐시 적중 — 실제 객체를 부르지 않는다
```

권한 → 캐시 → 지연 생성 순으로 겹쳤다. 두 번째 호출에서는 캐시가 막아서 안쪽 두 겹이 실행되지 않는다. **순서가 곧 정책이다** — 캐시를 권한 앞에 두면 권한 없는 사람도 캐시된 값을 받게 된다.

---

## 4. 새 통제를 추가하려면

```java
public class RateLimitReportProxy implements ReportService {

    private final ReportService target;
    private final Map<String, Integer> counts = new HashMap<>();

    @Override
    public String monthlyReport(String month) {
        if (counts.merge(month, 1, Integer::sum) > 10) {
            throw new IllegalStateException("조회 한도 초과");
        }
        return target.monthlyReport(month);
    }
}
```

**클래스 하나 추가가 전부다.** `DatabaseReportService`도, 다른 프록시도, 클라이언트도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

---

## 5. 알아둘 점

### 데코레이터와의 차이 — 구조는 같고 위임이 다르다

가장 헷갈리는 지점이다. 클래스 다이어그램을 그리면 [데코레이터](decorator.md)와 **거의 똑같다.** 둘 다 같은 인터페이스를 구현하고 같은 타입을 들고 있다.

차이는 **위임을 어떻게 하느냐**에 있다.

```java
// 데코레이터 — 반드시 부른다
return beverage.cost() + 500;

// 프록시 — 안 부를 수 있다
if (!user.canReadReport()) throw new SecurityException(...);
if (cached != null) return cached;
```

| | 데코레이터 | 프록시 |
|---|---|---|
| 감쌀 대상 | **생성자로 받는다** | 받거나 **스스로 만든다** |
| 위임 | 항상 한다 | **안 할 수도 있다** |
| 목적 | 기능을 **더한다** | 접근을 **통제한다** |
| 클라이언트 | 겹친 걸 안다 (직접 조립) | **모르는 게 정상** |

마지막 항목도 실질적인 차이다. 데코레이터는 클라이언트가 `new Whipped(new ExtraShot(...))`처럼 **의도적으로 조립**한다. 프록시는 클라이언트가 프록시를 받았는지도 모르는 게 보통이다 — 스프링이 대신 끼워 넣기 때문이다.

> 판단 기준 한 줄: **"실제 객체를 안 부르는 경로가 있는가?"** 있으면 프록시, 없으면 데코레이터다.

### 어댑터·파사드와의 차이

| 패턴 | 인터페이스 | 감싸는 개수 | 목적 |
|---|---|---|---|
| [어댑터](adapter.md) | **바꾼다** | 1 | 맞물리게 |
| [데코레이터](decorator.md) | 유지한다 | 1 | 기능을 더하려고 |
| **프록시** | **유지한다** | 1 | **접근을 통제하려고** |
| [파사드](facade.md) | 발명한다 | 여러 개 | 단순하게 |

프록시와 데코레이터만 인터페이스를 유지한다. 그 둘 사이는 위의 표로 가른다.

### 스프링 AOP가 이 패턴이다

```java
@Service
public class ReportService {

    @Transactional
    @Cacheable("reports")
    @PreAuthorize("hasRole('MANAGER')")
    public String monthlyReport(String month) { ... }
}
```

이 세 애너테이션이 각각 **스마트 참조 · 캐싱 프록시 · 보호 프록시**다. 스프링은 런타임에 프록시 객체를 만들어 빈으로 등록하고, 주입받는 쪽은 진짜 대신 프록시를 받는다. **우리가 손으로 짠 것과 구조가 같다.**

그래서 스프링에서 유명한 함정이 이 패턴에서 그대로 나온다.

```java
public String report(String month) {
    return this.cachedReport(month);   // ← self-invocation. 프록시를 안 거친다
}

@Cacheable("reports")
public String cachedReport(String month) { ... }
```

같은 객체 안에서 자기 메서드를 부르면 **프록시를 통과하지 않으므로 캐시가 동작하지 않는다.** 프록시는 바깥에서 들어오는 호출만 가로챌 수 있기 때문이다. 이 예제로 치면 `DatabaseReportService`가 자기 메서드를 부를 때 `CachingReportProxy`를 거치지 않는 것과 같다.

### 프록시는 실제 객체를 대신할 수 있어야 한다

```java
ReportService service = new AuthReportProxy(real, staff);
service.monthlyReport("2026-07");     // SecurityException
```

권한이 없으면 예외가 난다. 이건 의도된 통제지만, **원래 없던 예외가 생겼다**는 뜻이기도 하다. 클라이언트가 `ReportService`를 받았을 때 이런 실패를 예상하지 못한다면 리스코프 치환 원칙을 흔든다.

> 보호 프록시를 쓸 때는 **실패가 규격의 일부**라는 걸 인터페이스에 드러내는 편이 낫다. 예외를 문서화하거나, 애초에 반환 타입에 실패를 담는다.

### 캐싱 프록시는 무효화가 숙제다

이 예제의 캐시는 한 번 담기면 영원히 남는다. 매출이 정정되어도 옛 값이 나간다.

실무라면 TTL, 최대 크기(LRU), 명시적 무효화가 필요하고, 그 순간 프록시가 꽤 복잡해진다. **직접 만들기보다 `@Cacheable`이나 Caffeine 같은 것을 쓰는 편이 낫다** — 프록시 패턴을 아는 것은 그것들이 무엇을 하는지 이해하기 위해서다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 스레드 안전 — `LazyReportProxy`의 `real == null` 검사가 [싱글턴](singleton.md)의 그 경쟁 조건과 같은 모양이다
- 캐시 무효화 (TTL, LRU, 명시적 제거)
- 동적 프록시 (`java.lang.reflect.Proxy`, CGLIB) — 인터페이스마다 클래스를 만들지 않아도 된다
- 권한 정보를 파라미터가 아니라 컨텍스트에서 읽기 (`SecurityContextHolder`)
- 프록시 자신의 예외 처리 — 지금은 실제 객체의 예외가 그대로 통과한다

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 실제 객체의 **생성이나 호출이 비쌀** 때 (지연 생성, 캐싱)
- **접근을 제한**해야 하는데 그 규칙이 객체의 본업과 무관할 때
- 객체가 **다른 프로세스**에 있는데 로컬처럼 쓰고 싶을 때
- 실제 객체를 **수정할 수 없을** 때 (라이브러리, 생성된 코드)

**안 쓴다**
- 통제할 게 없을 때 — 그냥 위임만 하면 프록시가 아니라 낭비다
- 기능을 **더하고 싶을** 때 — [데코레이터](decorator.md)다
- 인터페이스를 **바꿔야** 할 때 — [어댑터](adapter.md)다
- 프레임워크가 이미 해 주고 있을 때 — 스프링이라면 `@Cacheable`, `@PreAuthorize`, `@Transactional`로 끝난다

판단은 한 줄이다. **"실제 객체를 부르지 않을 이유가 있는가?"** 있으면 프록시다.
