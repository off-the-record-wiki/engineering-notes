# (Effective Java Item 38) Java ‐ 확장할 수 있는 열거 타입이 필요하면 인터페이스를 사용하라

#### 인터페이스를 이용해 확장 가능 열거 타입을 모방

```java
public interface Operation {
    double apply(double x, double y);
}
```
```java
// Good
public enum BasicOperation implements Operation {
    PLUS("+") {
        public double apply(double x, double y) { return x + y; }
    },
    MINUS("-") {
        public double apply(double x, double y) { return x - y; }
    },
    TIMES("*") {
        public double apply(double x, double y) { return x * y; }
    },
    DIVIDE("/") {
        public double apply(double x, double y) { return x / y; }
    };

    private final String symbol;

    BasicOperation(String symbol) {
        this.symbol = symbol;
    }

    @Override public String toString() {
        return symbol;
    }
}
```
* 열거 타입인 BasicOperation은 확장할 수 없지만 인터페이스인 Operation은 확장할 수 있어 이 인터페이스를 연산 타입으로 사용해 확장할 수 있는 예시를 보여주는 코드이다.
* 열거 타입 자체는 확장이 불가능하나 인터페이스와 그 인터페이스를 구현하는 기본 열거 타입을 함께 사용해 같은 효과를 낼 수 있다.
* **"언제 사용해야 하나?"**
  * 타입이 미래에 확장될 수 있다.
  * enum을 수정하기 위해 기존 프로젝트를 다시 컴파일해야 한다.
  * 도메인의 값 집합이 고정돼 있지 않을 때
