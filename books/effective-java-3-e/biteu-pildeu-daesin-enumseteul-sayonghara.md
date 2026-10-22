# (Effective Java Item 36) Java ‐ 비트 필드 대신 EnumSet을 사용하라

* `EnumSet` 클래스는 열거 타입 상수의 값으로 구성된 집합을 효과적으로 표현해준다.
* `Set` 인터페이스를 완벽히 구현하며, 타입 안전하고, 다른 어떤 `Set` 구현체와도 함께 사용할 수 있다.
* 하지만 `EnumSet`의 내부는 비트 벡터로 구현되었다. 원소가 총 64개 이하라면, 즉 대부분의 경우에 EnumSet 전체를 long 변수 하나로 표현하여 비트 필드에 비견되는 성능을 보여준다.
* `removeAll`, `retainAll`와 같은 대량 작업은 비트를 효율적으로 처리할 수 있는 산술 연산을 써서 구현했다.

```java
// Good
public class Text {
    public enum Style {
        BOLD, ITALIC, UNDERLINE, STRIKEROUGH
    }

    public void applyStyles(Set<Style> styles) {
        // ...
    }
}
```
