# 데코레이터 패턴 (Decorator Pattern)

> 원본을 고치지 않고 **같은 타입으로 겹겹이 감싸서** 기능을 얹는 패턴.

음료 토핑을 예제로 구현했다.

![데코레이터 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/decorator-pattern.png)

---

## 1. 왜 필요한가

음료에 토핑을 붙여 팔아야 한다. 상속으로 풀면 이렇게 된다.

```java
// 데코레이터를 쓰지 않았을 때
class Americano { }
class AmericanoWithShot extends Americano { }
class AmericanoWithShotAndWhip extends Americano { }
class AmericanoWithShotAndWhipAndSyrup extends Americano { }
class AmericanoWithWhipAndSyrup extends Americano { }
class LatteWithShot extends Latte { }
// ...
```

문제는 **조합마다 클래스가 필요하다**는 것이다. 토핑이 `n`개면 조합은 `2ⁿ`개고, 음료 종류만큼 다시 곱해진다. 토핑 하나만 추가해도 클래스 수가 두 배가 된다.

필드로 푸는 방법도 있다.

```java
public class Beverage {
    private boolean shot;
    private boolean whip;
    private boolean syrup;      // 토핑이 늘 때마다 필드가 늘고

    public int cost() {
        int cost = basePrice;
        if (shot)  cost += 500;
        if (whip)  cost += 700;
        if (syrup) cost += 300;   // 이 메서드를 매번 다시 연다
        return cost;
    }
}
```

이번엔 클래스는 안 늘지만 **잘 돌아가던 `cost()`를 계속 수정해야 한다.** 샷을 두 번 넣는 것도 `boolean`으로는 표현되지 않는다.

데코레이터는 **토핑 하나를 클래스 하나로** 만들고, 조합은 실행 시점에 감싸서 만든다.

```java
// 데코레이터를 썼을 때 — 조합이 코드가 아니라 객체다
Beverage drink = new Whipped(new ExtraShot(new Americano()));
```

토핑이 `n`개면 클래스도 `n`개다. 새 토핑은 클래스 하나 추가로 끝나고, `Americano`는 한 줄도 바뀌지 않는다.

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Component** | 원본과 데코레이터가 공유하는 규격 | `Beverage` |
| **ConcreteComponent** | 감싸이는 원본 | `Americano` · `Latte` |
| **Decorator** | 같은 규격을 구현하면서 원본을 보유 | `BeverageDecorator` |
| **ConcreteDecorator** | 실제로 기능을 얹는다 | `ExtraShot` · `Whipped` · `Syrup` · `LargeSize` |
| **Client** | `Component` 타입만 안다 | `Main` |

### 핵심은 Decorator의 이중성이다

```java
public abstract class BeverageDecorator implements Beverage {   // ① Beverage'이다'
    protected final Beverage beverage;                          // ② Beverage'를 가진다'
}
```

**is-a이면서 has-a.** 이 두 줄이 데코레이터의 정의다.

- ①이 있어서 **원본이 오던 자리에 그대로 들어간다.** 클라이언트는 차이를 모른다
- ②가 있어서 **감쌀 수 있다.** 그리고 감싼 결과가 다시 `Beverage`이므로 **또 감쌀 수 있다**

둘 중 하나만 있으면 다른 것이 된다. 구현만 하면 그냥 다른 구현체이고, 보유만 하면 단순 위임이다.

`BeverageDecorator`가 추상 클래스인 이유도 여기 있다. **아무것도 더하지 않는 데코레이터는 존재할 이유가 없으므로** 직접 만들지 못하게 막는다.

### ConcreteDecorator — 감싼 것에게 먼저 묻고, 자기 몫을 얹는다

```java
public class ExtraShot extends BeverageDecorator {

    @Override
    public String description() {
        return beverage.description() + ", 샷 추가";    // 위임 → 가공
    }

    @Override
    public int cost() {
        return beverage.cost() + 500;                  // 위임 → 가공
    }
}
```

모든 데코레이터가 이 모양이다. **자기가 감싼 것을 먼저 호출하고, 그 결과에 자기를 더한다.**

### Component는 작아야 한다

```java
public interface Beverage {
    String description();
    int cost();
}
```

메서드가 둘뿐이다. 여기에 하나가 늘 때마다 **모든 데코레이터가** 그것을 어떻게 위임할지 답해야 한다. 데코레이터가 다섯 개인데 메서드를 하나 추가하면 다섯 곳을 고친다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out decorator.code.Main
```

```
--- 1. 원본과 감싼 것을 구분하지 않는다 ---
아메리카노 → 4,500원
아메리카노, 샷 추가 → 5,000원

--- 2. 감싼 만큼 쌓인다 ---
아메리카노, 샷 추가, 휘핑 → 5,700원
카페라떼, 샷 추가, 바닐라 시럽 → 5,800원

--- 3. 순서가 결과를 바꾼다 ---
아메리카노, 샷 추가, 라지 → 7,500원
아메리카노, 라지, 샷 추가 → 7,250원

--- 4. 실행 중에 조합한다 ---
카페라떼, 샷 추가, 휘핑, 라지 → 9,300원
```

`print()`는 `Beverage` 하나만 받는다. 원본인지 세 겹인지 **구분하는 코드가 없다.**

```java
private static void print(Beverage beverage) {
    System.out.printf("%s → %,d원%n", beverage.description(), beverage.cost());
}
```

### 호출이 흐르는 길

`new Whipped(new ExtraShot(new Americano())).cost()`를 부르면 이렇게 된다.

```
Whipped.cost()
  → ExtraShot.cost()
      → Americano.cost() = 4,500
      ← 4,500
  ← 4,500 + 500 = 5,000
← 5,000 + 700 = 5,700
```

**안쪽 끝까지 갔다가, 나오면서 쌓인다.** 데코레이터를 읽을 때는 이 왕복을 머릿속에 그려야 한다.

---

## 4. 새 토핑을 추가하려면

```java
public class Cinnamon extends BeverageDecorator {

    public Cinnamon(Beverage beverage) {
        super(beverage);
    }

    @Override public String description() { return beverage.description() + ", 시나몬"; }
    @Override public int cost()           { return beverage.cost() + 200; }
}
```

**클래스 파일 하나 추가가 전부다.** `Americano`도, `Beverage`도, 기존 토핑도 건드리지 않는다. → 개방-폐쇄 원칙(OCP)

그리고 추가하는 순간 **기존 모든 조합과 자동으로 결합된다.** `new Cinnamon(new Whipped(new Latte()))`가 바로 된다. 상속이었다면 조합마다 클래스를 또 만들어야 했다.

---

## 5. 알아둘 점

### 순서가 결과를 바꾼다

더하기만 하는 데코레이터끼리는 순서가 상관없다. 하지만 하나라도 다른 연산이 섞이면 달라진다.

```java
new LargeSize(new ExtraShot(americano))    // (4,500 + 500) × 1.5 = 7,500원
new ExtraShot(new LargeSize(americano))    //  4,500 × 1.5 + 500  = 7,250원
```

같은 재료인데 **250원이 다르다.** `LargeSize`가 더하지 않고 곱하기 때문이다.

이건 버그가 아니라 이 패턴의 성질이다. 다만 **순서가 곧 사양이 된다**는 뜻이므로, 이런 데코레이터가 섞이면 감싸는 순서를 문서로 못 박거나 조립을 한 곳(팩토리, 빌더)으로 모아야 한다. 여기저기서 자유롭게 감싸게 두면 가격이 조용히 갈린다.

### 실행 중에 조합할 수 있다 — 상속과 갈리는 지점

```java
Beverage drink = new Latte();
for (String option : List.of("샷", "휘핑", "라지")) {
    drink = wrap(drink, option);     // 조합이 컴파일 시점에 정해져 있지 않다
}
```

주문서에서 읽어 온 옵션으로 조합을 만든다. 상속으로는 불가능하다. **상속은 컴파일 타임에 확정되고, 데코레이터는 런타임에 조립된다.**

### 감싸면 원래 타입을 잃는다

```java
Beverage drink = new ExtraShot(new Americano());
drink instanceof Americano       // false — 감싼 순간 아니다
```

`ExtraShot`은 `Americano`를 **가지고** 있을 뿐 `Americano`가 아니다. 그래서 클라이언트가 `instanceof`나 캐스팅으로 구체 타입을 확인하는 코드가 있으면 이 패턴은 깨진다.

> 클라이언트는 `Component` 타입만 알아야 한다. 이 조건을 못 지키는 상황이라면 데코레이터가 맞는 도구인지 다시 봐야 한다.

### 프록시·컴포지트와 헷갈릴 때

셋 다 "같은 인터페이스로 감싼다"는 구조가 같아서 그림만 보면 구분이 안 된다. **목적이 다르다.**

| | 데코레이터 | 프록시 | 컴포지트 |
|---|---|---|---|
| 목적 | 기능을 **더한다** | 접근을 **통제한다** (지연 생성, 권한, 캐싱) | 여러 개를 **하나처럼** 다룬다 |
| 감싸는 개수 | 1개 | 1개 | **여러 개** |
| 누가 정하나 | 클라이언트가 조립 | 대개 프록시가 스스로 관리 | 트리 구조로 구성 |

판단은 질문 하나다. **"감싸서 무엇을 하려는가?"** 없던 동작을 더하면 데코레이터, 원래 동작을 그대로 두고 언제/누가를 통제하면 프록시.

### 자바 표준 라이브러리의 데코레이터

`java.io`가 통째로 데코레이터다.

```java
InputStream in = new BufferedInputStream(new FileInputStream("a.txt"));
```

`BufferedInputStream`은 `InputStream`이면서 `InputStream`을 가진다. 이 예제의 `ExtraShot`과 정확히 같은 구조다. 처음 자바를 배울 때 "왜 스트림을 이렇게 겹쳐 쓰지?" 싶었던 게 이 패턴이다.

`Collections.unmodifiableList()`, `synchronizedList()`도 같은 모양이다. 원본 리스트를 감싸서 "수정 금지"라는 동작을 얹는다.

### 실무에서 더 자주 쓰이는 모양

토핑보다는 **횡단 관심사**를 얹을 때 쓴다.

```java
PaymentGateway gateway =
    new RetryingGateway(                    // 실패하면 재시도
        new LoggingGateway(                 // 요청·응답 기록
            new HttpPaymentGateway()));     // 실제 결제
```

로깅·재시도·캐싱·타임아웃은 **결제 로직과 아무 상관이 없는데 결제 코드에 섞이기 쉬운 것들**이다. 데코레이터로 빼면 `HttpPaymentGateway`는 결제만 하고, 필요한 쪽에서 원하는 만큼 감싼다. 스프링 AOP가 프록시로 하는 일도 성격이 같다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 금액 값 객체 (`int` 대신 `Money`) — `LargeSize`의 `× 3 / 2`는 원 단위 절사가 그냥 버려진다
- 조합 규칙 검증 — "샷은 최대 3번", "휘핑은 라떼만" 같은 제약
- 조립을 한 곳으로 모으는 팩토리·빌더 — 순서 사고를 막는다
- `equals`/`hashCode` — 감싼 것과 원본을 어떻게 비교할지 정해야 한다

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 기능의 **조합**이 필요하고, 조합 수가 많을 때 (상속이면 클래스가 폭발한다)
- 실행 중에 기능을 붙였다 뗐다 해야 할 때
- 원본 클래스를 **수정할 수 없을 때** (라이브러리, 레거시)
- 로깅·재시도·캐싱처럼 **본래 로직과 무관한 관심사**를 얹을 때

**안 쓴다**
- 조합이 두세 개뿐이고 늘 일이 없을 때 — 필드나 파라미터가 낫다. 클래스만 늘고 얻는 게 없다
- 클라이언트가 구체 타입을 알아야 할 때 — 감싸면 `instanceof`가 깨진다
- Component 인터페이스가 클 때 — 메서드가 열 개면 데코레이터마다 열 개를 위임해야 한다
- 기능을 **더하는 게 아니라 바꿔야** 할 때 — 그건 전략 패턴이다

마지막 판단 기준은 이렇다. **"기존 동작 위에 얹는가, 기존 동작을 대체하는가?"** 얹으면 데코레이터, 대체하면 전략이다.
