# 상태 패턴 (State Pattern)

> 객체의 **상태를 클래스로 만들어서**, 상태가 바뀌면 행동도 함께 바뀌게 하는 패턴.

음료 자판기를 예제로 구현했다.

![상태 패턴 구조](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/state-pattern.png)

---

## 1. 왜 필요한가

자판기가 있다. 조작은 세 가지뿐이다 — **동전 투입 · 동전 반환 · 버튼 누르기**. 그런데 같은 조작이 상황에 따라 다르게 동작해야 한다. 동전이 없을 때 버튼을 누르면 아무 일도 없어야 하고, 이미 동전이 들어 있는데 또 넣으면 받지 말아야 하고, 매진이면 전부 거절해야 한다.

상태를 필드로 두고 분기하면 이렇게 된다.

```java
// 상태 패턴을 쓰지 않았을 때
public class VendingMachine {

    private static final int NO_COIN = 0;
    private static final int HAS_COIN = 1;
    private static final int SOLD_OUT = 2;

    private int state = NO_COIN;
    private int stock;

    public void insertCoin() {
        if (state == HAS_COIN) {
            System.out.println("이미 동전이 있다");
        } else if (state == SOLD_OUT) {
            System.out.println("매진이다");
        } else if (state == NO_COIN) {
            state = HAS_COIN;
        }
    }

    public void pressButton() {
        if (state == NO_COIN) {
            System.out.println("동전을 먼저 넣어라");
        } else if (state == SOLD_OUT) {
            System.out.println("매진이다");
        } else if (state == HAS_COIN) {
            stock--;
            state = (stock > 0) ? NO_COIN : SOLD_OUT;   // ← 전이 규칙이 여기 숨어 있다
        }
    }

    public void ejectCoin() { /* 또 같은 모양의 if 사슬 */ }
}
```

**조작이 3개, 상태가 3개니까 분기가 9개다.** 그리고 이게 세 메서드에 흩어져 있다. 여기서 "품절 임박" 상태를 하나 추가하면 **모든 메서드의 if 사슬을 빠짐없이 고쳐야 한다.** 하나라도 빠뜨리면 그 조작만 조용히 잘못 동작한다 — 컴파일러는 아무것도 알려주지 않는다.

더 나쁜 건 **전이 규칙이 조건문 사이에 파묻힌다**는 것이다. "언제 매진이 되는가"를 알려면 `pressButton` 안의 삼항 연산자를 찾아내야 한다.

상태 패턴은 **분기의 축을 뒤집는다.** 조작별로 상태를 나누는 대신, **상태별로 클래스를 만들고** 그 안에 조작을 전부 적는다.

```java
// 상태 패턴을 썼을 때 — 컨텍스트에 if가 없다
public void insertCoin() {
    state.insertCoin(this);
}

public void pressButton() {
    state.pressButton(this);
}
```

```java
// 규칙은 상태 클래스 안에 모인다
public class NoCoinState implements VendingMachineState {

    @Override
    public void pressButton(VendingMachine machine) {
        System.out.println("동전을 먼저 넣어야 한다");   // 이 상태에서 버튼은 무의미하다
    }
}
```

"동전이 없을 때 이 자판기가 어떻게 동작하는가"를 알고 싶으면 **`NoCoinState` 한 파일만 열면 된다.**

---

## 2. 구성 요소

| GoF 참여자 | 역할 | 이 예제의 클래스 |
|---|---|---|
| **Context** | 조작을 받아 현재 상태에게 넘긴다 | `VendingMachine` |
| **State** | 상태들이 공유하는 규격 | `VendingMachineState` |
| **ConcreteState** | 그 상태에서의 행동과 전이 | `NoCoinState` · `HasCoinState` · `SoldOutState` |
| **Client** | 자판기를 조작한다 | `Main` |

### State가 있어야 성립한다

```java
public interface VendingMachineState {
    void insertCoin(VendingMachine machine);
    void ejectCoin(VendingMachine machine);
    void pressButton(VendingMachine machine);
    String name();
}
```

**조작 하나가 메서드 하나다.** 그래서 새 상태를 추가하면 컴파일러가 "이 메서드들을 구현하라"고 강제한다 — 앞의 if 버전에서 분기를 빠뜨리던 문제가 **컴파일 에러로 바뀐다.** 이게 조건문을 다형성으로 바꿔서 얻는 가장 실질적인 이득이다.

### 컨텍스트는 위임만 한다

```java
public void insertCoin() {
    state.insertCoin(this);
}
```

`VendingMachine`에는 **`if`가 하나도 없다.** 아는 것은 "지금 어떤 상태냐"뿐이고, 판단은 전부 상태 객체가 한다.

### 전이는 누가 정하는가 — 상태 자신이 정한다

```java
// HasCoinState
@Override
public void pressButton(VendingMachine machine) {
    machine.releaseOne();
    machine.changeState(machine.stock() > 0
        ? machine.noCoinState()
        : machine.soldOutState());     // ← "언제 매진이 되는가"가 여기 있다
}
```

이 패턴의 핵심이 이 줄이다. 다음 상태를 **컨텍스트가 아니라 현재 상태가 계산한다.** `VendingMachine`은 `changeState()`로 시키는 대로 바꿀 뿐, 어떤 조건에서 매진이 되는지 모른다.

### 상태 객체는 공유된다

```java
private final VendingMachineState noCoin = new NoCoinState();
private final VendingMachineState hasCoin = new HasCoinState();
private final VendingMachineState soldOut = new SoldOutState();
```

상태 객체를 미리 하나씩 만들어 두고 계속 돌려 쓴다. 가능한 이유는 **상태 클래스가 필드를 갖지 않기 때문**이다 — 컨텍스트를 필드로 들지 않고 메서드 파라미터로 받는 설계가 그래서 중요하다. 상태 객체가 `machine`을 필드로 들었다면 자판기마다 상태 객체를 새로 만들어야 했다.

---

## 3. 실행

```bash
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out state.code.Main
```

```
--- 1. 정상 구매 — 상태가 순서대로 흐른다 ---
  자판기 준비 — 재고 2개, 상태: 동전 없음
    [동전 없음] 동전을 받았다
      (상태 전이: 동전 없음 → 동전 있음)
    [동전 있음] 판매 처리
      (음료 배출 — 남은 재고 1개)
      (상태 전이: 동전 있음 → 동전 없음)
  → 현재 상태: 동전 없음

--- 2. 잘못된 조작은 상태가 막는다 ---
    [동전 없음] 동전을 먼저 넣어야 한다
    [동전 없음] 반환할 동전이 없다
    [동전 없음] 동전을 받았다
      (상태 전이: 동전 없음 → 동전 있음)
    [동전 있음] 이미 동전이 들어 있다 — 받지 않는다
  → 컨텍스트에는 검증 코드가 한 줄도 없다

--- 3. 동전 반환 — 되돌아가는 전이 ---
    [동전 있음] 동전을 돌려준다
      (상태 전이: 동전 있음 → 동전 없음)
  → 현재 상태: 동전 없음

--- 4. 마지막 하나를 팔면 상태가 스스로 바뀐다 ---
    [동전 없음] 동전을 받았다
      (상태 전이: 동전 없음 → 동전 있음)
    [동전 있음] 판매 처리
      (음료 배출 — 남은 재고 0개)
      (상태 전이: 동전 있음 → 매진)
  → 현재 상태: 매진

--- 5. 매진 — 모든 조작이 거절된다 ---
    [매진] 팔 음료가 없다 — 동전을 받지 않는다
    [매진] 판매할 음료가 없다
    [매진] 반환할 동전이 없다
  → 현재 상태: 매진
→ 같은 세 가지 조작인데, 상태마다 반응이 전부 달랐다.
```

**증거는 같은 조작에 대한 응답이 매번 다르다는 것이다.**

- 1번 — 동전 투입 → 버튼의 정상 경로. 전이 로그가 상태 기계의 흐름을 그대로 보여준다
- 2번 — 잘못된 조작 셋이 모두 막혔는데, **`VendingMachine`에는 검증 코드가 한 줄도 없다.** 거절한 것은 상태 객체다
- 5번 — 같은 `insertCoin()`인데 2번에서는 "이미 동전이 있다", 5번에서는 "팔 음료가 없다"로 거절 사유가 다르다. **조건문 하나로 뭉뚱그렸다면 사라졌을 구분이다**
- 4번 — 재고가 0이 되는 순간 `HasCoinState`가 스스로 매진으로 전이했다. 클라이언트는 재고를 확인한 적도, 상태를 지정한 적도 없다

---

## 4. 새 상태를 추가하려면

품절 임박 상태(마지막 하나일 때 경고)를 넣는다고 하자.

```java
public class LastOneState implements VendingMachineState {

    @Override
    public void insertCoin(VendingMachine machine) {
        System.out.println("    [마지막 하나] 동전을 받았다 — 이게 마지막입니다");
        machine.changeState(machine.hasCoinState());
    }

    @Override
    public void ejectCoin(VendingMachine machine) {
        System.out.println("    [마지막 하나] 반환할 동전이 없다");
    }

    @Override
    public void pressButton(VendingMachine machine) {
        System.out.println("    [마지막 하나] 동전을 먼저 넣어야 한다");
    }

    @Override
    public String name() {
        return "마지막 하나";
    }
}
```

**클래스 하나 추가와, 이 상태로 보내는 쪽의 전이 한 줄이 전부다.** 기존 세 상태의 코드는 건드리지 않는다 — `HasCoinState`가 재고 판단 부분에서 이 상태를 가리키게만 하면 된다. if 버전이었다면 세 메서드의 분기를 전부 뒤져야 했다. → 개방-폐쇄 원칙(OCP)

**다만 공짜는 아니다.** 이 패턴이 잘 버티는 것은 **상태를 추가할 때**다. 반대로 **조작(메서드)을 추가할 때**는 모든 상태 클래스를 다 고쳐야 한다. 상태 3개에 조작을 하나 더하면 세 클래스가 전부 바뀐다. 축을 뒤집었으니 비용도 뒤집힌 것이다 — 상태는 자주 늘고 조작은 잘 안 늘어나는 문제에 쓰는 게 맞다.

---

## 5. 알아둘 점

### 전략 패턴과의 차이 — 또 구조는 같고, 이번엔 **누가 바꾸는가**가 다르다

[전략](strategy.md)·[커맨드](command.md)에 이어 세 번째로 같은 질문이 나온다. 클래스 다이어그램을 그리면 [전략 패턴](strategy.md)과 **구분이 안 된다.** 둘 다 인터페이스를 두고, 컨텍스트가 구현체에 위임한다.

차이는 **교체의 주체와 시점**에 있다.

| | 전략 패턴 | 상태 패턴 |
|---|---|---|
| 누가 바꾸나 | **클라이언트**가 골라서 주입 | **상태 자신**이 다음 상태를 정한다 |
| 언제 바뀌나 | 보통 한 번 정하면 끝 | 실행 중 **계속 바뀐다** |
| 구현체끼리 | 서로를 **모른다** | 서로를 **안다** (전이 대상) |
| 교체하는 이유 | 같은 일을 **다른 방법**으로 | 같은 조작에 **다른 반응**을 |
| 대체하는 것 | 알고리즘 선택 | **조건문(if/switch)** |

가장 확실한 판별법은 **구현체끼리 서로를 아느냐**다. `HasCoinState`는 `SoldOutState`의 존재를 안다 — 거기로 보내야 하니까. 전략 패턴에서 퀵정렬 전략이 병합정렬 전략을 아는 일은 없다.

> 판단 기준 한 줄: **"구현체가 다음 구현체를 지목하는가?"** 그러면 상태, 아니면 전략이다.

### 상태 패턴은 상태 기계를 코드로 옮긴 것이다

이 예제를 상태 전이도로 그리면 그대로 유한 상태 기계(FSM)가 된다.

```
동전 없음 ──insertCoin──> 동전 있음 ──pressButton(재고>0)──> 동전 없음
                              │
                              ├──ejectCoin──> 동전 없음
                              │
                              └──pressButton(재고=0)──> 매진 (막다른 상태)
```

**전이도의 화살표 하나가 상태 클래스의 메서드 한 줄에 대응한다.** 그래서 기획서의 상태 다이어그램을 코드로 옮기기 쉽고, 반대로 코드에서 전이도를 복원하기도 쉽다. 주문 상태(결제대기 → 결제완료 → 배송중 → 배송완료), 문서 결재 흐름처럼 **도메인에 상태 전이도가 이미 있는 문제**가 이 패턴의 자리다.

### 상태를 어디에 둘 것인가 — enum이라는 대안

자바에서는 enum이 메서드를 가질 수 있어서, 상태가 단순하면 클래스 대신 enum으로도 같은 걸 만든다.

```java
public enum VendingState {
    NO_COIN {
        @Override void insertCoin(VendingMachine m) { m.changeState(HAS_COIN); }
    },
    HAS_COIN {
        @Override void insertCoin(VendingMachine m) { /* 거절 */ }
    };

    abstract void insertCoin(VendingMachine m);
}
```

상태 개수가 고정이고 상태별 데이터가 없다면 enum이 더 간결하다 — 인스턴스가 하나로 보장되고, `switch`나 직렬화도 자연스럽다. 반대로 **상태별로 다른 필드가 필요하거나, 상태를 런타임에 늘려야 하거나, 상태마다 의존성을 주입받아야 한다면** 클래스 쪽이 맞다.

### 이 예제에서 의도적으로 뺀 것

학습 초점을 흐리지 않으려고 뺐다. 실무라면 필요하다.

- 금액 계산 — 동전의 "있고 없음"만 다룬다. 실제라면 잔액·거스름돈·복수 코인이 붙는다
- 스레드 안전 — `changeState`와 `stock` 감소가 원자적이지 않다. 동시 요청이 오면 재고가 음수가 될 수 있다
- 상태 영속화 — 프로세스가 죽으면 상태가 사라진다. 주문 상태처럼 DB에 남겨야 하는 경우 상태를 문자열/enum으로 저장하고 복원하는 매핑이 필요하다
- 전이 가드와 이력 — "이 전이가 허용되는가"를 별도로 검증하거나, 전이 로그를 남기는 것. 상태 기계 라이브러리(Spring Statemachine 등)가 해 주는 영역이다
- 잘못된 조작의 반환값 — 지금은 로그만 찍고 조용히 넘어간다. 실무라면 예외를 던지거나 결과 객체를 돌려줘야 한다

---

## 6. 언제 쓰고, 언제 안 쓰나

**쓴다**
- 객체의 행동이 **상태에 따라 달라지고**, 그 상태가 실행 중에 바뀔 때
- 같은 조건문(`if (state == ...)`)이 **여러 메서드에 반복**될 때 — 이게 가장 확실한 신호다
- 도메인에 **상태 전이도가 이미 있을 때** (주문, 결재, 예약, 티켓)
- 상태를 **앞으로 더 늘릴 것 같을** 때

**안 쓴다**
- 상태가 둘뿐이고 분기도 한두 군데라면 `boolean` 필드와 `if`가 낫다 — 클래스 3개를 만드는 게 오히려 손해다
- 상태가 행동을 바꾸지 않고 **데이터일 뿐**일 때 — 그냥 enum 필드다
- 구현체를 **클라이언트가 골라 주입**하는 구조라면 [전략 패턴](strategy.md)이다
- 조작(메서드)이 앞으로 계속 늘어날 예정이라면 — 상태를 늘리는 건 싸지만 조작을 늘리는 건 비싸다

판단은 한 줄이다. **"같은 상태 조건문이 여러 메서드에 반복되고, 상태가 늘어날 것 같은가?"** 그렇다면 상태 패턴이다.
