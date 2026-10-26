# 템플릿 메서드 패턴 (Template Method Pattern)

> 알고리즘의 **뼈대는 부모가 고정**하고, 달라지는 **단계만 자식이 채우게** 만드는 패턴.

리포트 생성을 예제로 구현했다.

![템플릿 메서드 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/template-method-pattern.png)

---

## 1. 왜 필요한가

리포트 종류가 늘어날 때 이런 코드가 생긴다.

```java
// 템플릿 메서드를 쓰지 않았을 때
public class SalesReport {
    public String generate() {
        List<Row> rows = 매출_조회();
        StringBuilder sb = new StringBuilder();
        sb.append("===== 일일 매출 리포트 =====\n");   // ①
        sb.append(매출_본문(rows));
        sb.append(합계(rows)).append('\n');
        sb.append("========================\n");   // ②
        return sb.toString();
    }
}

public class InventoryReport {
    public String generate() {
        List<Row> rows = 재고_조회();
        StringBuilder sb = new StringBuilder();
        sb.append("===== 재고 현황 리포트 =====\n");   // ①과 똑같은 규칙
        sb.append(재고_본문(rows));
        sb.append("========================\n");   // ②와 똑같은 문자열
        return sb.toString();
    }
}
```

문제는 **절차가 복사된다**는 것이다. 머리말 서식을 바꾸라는 요청이 오면 리포트 클래스를 전부 열어야 하고, 하나를 빠뜨리면 그 리포트만 조용히 다른 모양이 된다. 새 리포트를 만드는 사람이 순서를 다르게 짜도 막을 방법이 없다.

템플릿 메서드는 **절차를 부모 클래스 한 곳에만 둔다.** 자식은 빈칸만 채운다.

```java
// 템플릿 메서드를 썼을 때 — 절차는 여기 한 번만 존재한다
public final String generate() {
    List<Row> rows = fetchRows();
    StringBuilder report = new StringBuilder();
    report.append(header());
    report.append(body(rows));
    if (includeSummary()) {
        report.append(summary(rows)).append('\n');
    }
    report.append(footer());
    return report.toString();
}
```

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **AbstractClass** | 템플릿 메서드로 절차를 정의하고, 단계를 선언 | `ReportGenerator` |
| **ConcreteClass** | 달라지는 단계만 구현 | `SalesReportGenerator` · `InventoryReportGenerator` |
| **Client** | 템플릿 메서드만 호출 | `Main` |

전략 패턴과 달리 참여자가 **둘뿐이다.** 알고리즘을 담을 별도의 인터페이스도, 그걸 들고 있을 Context도 없다. 상속 계층 하나로 끝난다.

### 단계는 세 종류다

이 패턴을 이해하는 핵심은 "부모가 선언한 메서드가 다 같은 성격이 아니다"라는 점이다.

| 종류 | 접근 제어 | 의미 |
|---|---|---|
| **고정 단계** | `private` | 모두가 똑같이 쓴다. 재정의할 수 없다. |
| **추상 단계** (primitive operation) | `protected abstract` | 하위 클래스가 **반드시** 구현한다. |
| **훅** (hook) | `protected` + 기본 구현 | 기본 동작이 있다. 필요한 쪽만 재정의한다. |

```java
public abstract class ReportGenerator {

    public final String generate() { ... }              // 템플릿 메서드

    protected abstract String title();                  // 추상 단계
    protected abstract List<Row> fetchRows();
    protected abstract String body(List<Row> rows);

    protected boolean includeSummary() { return true; } // 훅
    protected String summary(List<Row> rows) {
        return "총 " + rows.size() + "건";
    }

    private String header() { ... }                     // 고정 단계
    private String footer() { ... }
}
```

### ConcreteClass — 필요한 칸만 채운다

```java
public class SalesReportGenerator extends ReportGenerator {
    // 추상 단계 3개는 구현이 강제된다
    @Override protected String title()      { return "일일 매출 리포트"; }
    @Override protected List<Row> fetchRows() { ... }
    @Override protected String body(List<Row> rows) { ... }

    // 훅은 필요해서 재정의한다 — 건수 대신 합계 금액
    @Override
    protected String summary(List<Row> rows) {
        int total = rows.stream().mapToInt(Row::value).sum();
        return String.format("합계 %,d원", total);
    }
}

public class InventoryReportGenerator extends ReportGenerator {
    // ... 추상 단계 3개 구현 ...

    // 훅으로 요약 단계를 통째로 끈다
    @Override
    protected boolean includeSummary() { return false; }
}
```

`InventoryReportGenerator`는 요약 코드를 한 줄도 쓰지 않고 요약을 없앴다. **단계를 켜고 끄는 스위치가 훅이다.**

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out template_method.code.Main
```

```
===== 일일 매출 리포트 =====
아메리카노         4,500원
카페라떼          5,000원
샌드위치          7,000원
합계 16,500원
========================

===== 재고 현황 리포트 =====
원두             12개
우유              3개  ← 재주문 필요
종이컵           250개
========================
```

머리말과 꼬리말은 두 리포트가 **정확히 같다.** 그 부분은 자식 코드에 존재하지 않기 때문이다.

---

## 4. 새 리포트를 추가하려면

```java
public class PayrollReportGenerator extends ReportGenerator {
    @Override protected String title()        { return "급여 지급 리포트"; }
    @Override protected List<Row> fetchRows() { return payrollRepository.findAll(); }
    @Override protected String body(List<Row> rows) { ... }
}
```

**추상 단계 세 개만 채우면 끝이다.** `generate()`도, 기존 리포트도 건드리지 않는다. 그러면서 머리말·꼬리말·요약은 자동으로 따라온다. → 개방-폐쇄 원칙(OCP)

---

## 5. 알아둘 점

### 템플릿 메서드는 반드시 `final`이어야 한다

```java
public final String generate() { ... }
```

`final`을 빼면 하위 클래스가 `generate()` 자체를 통째로 재정의할 수 있다. 그 순간 "절차를 부모가 소유한다"는 전제가 무너지고, 남는 건 그냥 상속받은 클래스 몇 개다. **이 패턴에서 `final`은 장식이 아니라 패턴의 정의 그 자체다.**

### 단계는 `public`이 아니라 `protected`다

```java
protected abstract String body(List<Row> rows);
```

단계는 **하위 클래스에게만** 열려 있는 확장점이지, 바깥에서 호출하라고 만든 API가 아니다. `public`으로 열면 클라이언트가 `generate()`를 건너뛰고 `body()`만 부르는 코드를 쓸 수 있게 되고, 절차를 강제한 의미가 사라진다.

### 호출 방향이 거꾸로다 — 헐리우드 원칙

보통의 재사용은 자식이 부모의 기능을 **불러다 쓴다.** 템플릿 메서드는 반대다. 부모의 `generate()`가 자식의 `body()`를 **불러 준다.**

> "Don't call us, we'll call you." — 헐리우드 원칙 (제어의 역전)

프레임워크가 이 구조를 쓰는 이유이기도 하다. 스프링의 `AbstractController`, JUnit의 `@BeforeEach`/`@Test` 실행 순서, 자바의 `AbstractList`가 모두 같은 모양이다. **흐름은 프레임워크가 갖고, 사용자는 빈칸만 채운다.**

### 전략 패턴과의 차이

같은 문제를 풀지만 도구가 다르다.

| | 전략 패턴 | 템플릿 메서드 |
|---|---|---|
| 수단 | **합성**(위임) | **상속** |
| 갈아끼우는 단위 | 알고리즘 **전체** | 알고리즘의 **일부 단계** |
| 런타임 교체 | 가능 (`changeDiscountPolicy`) | 불가능 (컴파일 타임에 확정) |
| 결합도 | 낮다 — 인터페이스만 안다 | 높다 — 부모의 내부 구조에 묶인다 |
| 잘 하는 일 | 방법이 여러 개일 때 | **중복된 절차를 없앨 때** |

판단 기준은 한 줄이다. **"절차는 같은데 단계만 다른가?"** → 템플릿 메서드. **"하는 일 자체가 다른가?"** → 전략.

### 단계의 시그니처가 갈리면 이 패턴은 못 쓴다

템플릿 메서드는 모든 하위 클래스가 **같은 시그니처**를 공유한다는 전제 위에 서 있다. 이게 깨지면 억지가 된다.

```java
// 로그인을 템플릿 메서드로 묶으려 하면
public abstract class LoginService {
    public final Token login(LoginRequest request) {    // 파라미터를 하나로 통일해야 한다
        UserIdentity identity = authenticate(request);
        ...
    }
    protected abstract UserIdentity authenticate(LoginRequest request);
}

public class EmailLoginService extends LoginService {
    @Override
    protected UserIdentity authenticate(LoginRequest request) {
        EmailRequest req = (EmailRequest) request;      // ← 다운캐스팅. 여기서 이미 졌다
        ...
    }
}
```

이메일 로그인은 `(email, password)`, 소셜 로그인은 `(provider, authCode)`다. 입력이 근본적으로 다른데 부모가 시그니처를 못 박으니 하위 클래스마다 캐스팅이 생긴다. 이럴 땐 **전략 + 조합**이 맞다.

반대로 소셜 로그인 **안쪽**의 Google/Kakao/Naver 처리는 OAuth2 절차가 동일하므로 템플릿 메서드가 잘 맞는다. **같은 도메인이라도 축을 나눠서 봐야 한다.**

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- `fetchRows()`의 예외·빈 결과 처리
- 리포트 출력 대상 추상화 (문자열 대신 `Writer`/스트림)
- 단계별 로깅·소요 시간 측정 (템플릿 메서드에 넣기 좋은 자리다)
- 금액·수량 값 객체 (`int` 대신 `Money`, `Quantity`)

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 여러 클래스가 **같은 순서로 같은 일**을 하는데, 중간 몇 단계만 다를 때
- 그 순서를 하위 클래스가 어기면 안 될 때 (= 순서 자체가 규칙일 때)
- 프레임워크·라이브러리에서 확장점을 열어 줄 때

**안 쓴다**
- 단계마다 입력·반환 타입이 달라야 할 때 — 캐스팅이 생기면 잘못 고른 것이다
- 실행 중에 동작을 바꿔야 할 때 — 상속은 런타임에 못 바꾼다. 전략 패턴을 쓴다
- 변형 축이 두 개 이상일 때 (예: 리포트 종류 × 출력 형식) — 상속 계층이 곱셈으로 늘어난다. 브리지 패턴이나 전략과의 조합을 본다
- 훅이 열 개쯤 되고 하위 클래스가 대부분을 재정의할 때 — 공유되는 게 없다는 뜻이므로 상속을 걷어낸다
