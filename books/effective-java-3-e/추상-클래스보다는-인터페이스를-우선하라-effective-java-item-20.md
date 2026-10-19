# (Effective Java Item 20) Java ‐ 추상 클래스보다는 인터페이스를 우선하라

#### 추상 클래스보다는 인터페이스를 우선하라

* 추상 클래스와 인터페이스는 Java에서 다중 구현을 제공하기 위한 수단이다. 
* 특히 Java 8부터는 디폴트 메서드를 통해서 인터페이스에도 구현을 제공할 수 있다. 또한 Java 9에서는 인터페이스에 private 메서드까지 정의할 수 있다.

```java
public interface Greeting {
    default String hello(String name) {
        return defaultGreeting(name);
    }

    private String defaultGreeting(String name) {
        return String.format("hello %s", name);
    }
}
```

* 인터페이스에 구현을 정의할 수 있는 시점에서 추상 클래스와 인터페이스의 큰 차이는 단일 상속과 다중 구현일 것이다.
* 즉, 다중 상속을 지원하지 않는 Java의 특성상 추상 클래스가 정의한 타입을 구현하는 클래스는 반드시 추상 클래스의 하위 클래스가 되어야한다는 점이다. 즉, 새로운 타입 정의에 제약이 생긴다는 의미이다.

#### 추상 클래스는 언제 사용하는게 좋을까?

* 추상 클래스와 인터페이스의 차이는 단일 상속/다중 구현뿐만은 아니다.
* 추상 클래스는 인터페이스와 달리 프로퍼티를 정의할 수 있다. 참고로 인터페이스도 프로퍼티를 정의할 수 있지만 기본적으로 public static 형태로 정의된다. 즉, 추상 클래스의 구현체가 추상 클래스가 가지고 있는 프로퍼티에 접근할 수 있다.
* 인터페이스에서는 protected 접근자를 사용할 수 없으나 추상 클래스에서는 protected 메서드를 정의할 수 있다.

```java
public interface Calculator {
    int calculate(int x, int y);
}

public abstract class AbstractCalculator implements Calculator {
    private final CalculateValidator calculateValidator;

    public AbstractCalculator(CalculateValidator calculateValidator) {
        this.calculateValidator = calculateValidator;
    }

    @Override
    public int calculate(int x, int y) {
        calculateValidator.validate(x, y);
        return operate(x, y);
    }

    public abstract boolean isSupport(CalculateType type);
    protected abstract int operate(int x, int y);
}
```
```java
public class PlusCalculator extends AbstractCalculator {
    public PlusCalculator(CalculateValidator calculateValidator) {
        super(calculateValidator);
    }

    @Override
    public boolean isSupport(CalculateType type) {
        return type == PLUS;
    }

    @Override
    protected int operate(int x, int y) {
        return x + y;
    }
}

public class MinusCalculator extends AbstractCalculator {
    public MinusCalculator(CalculateValidator calculateValidator) {
        super(calculateValidator);
    }

    @Override
    public boolean isSupport(CalculateType type) {
        return type == MINUS;
    }

    @Override
    protected int operate(int x, int y) {
        return x - y;
    }
}
```
