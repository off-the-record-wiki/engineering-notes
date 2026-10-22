# (Effective Java Item 35) Java - ordinal 메서드 대신 인스턴스 필드를 사용하라

* 대부분의 열거 타입 상수는 자연스럽게 하나의 정수값에 대응된다.
* 그리고 모든 열거 타입은 해당 상수가 그 열거 타입에서 몇 번째 위치인가를 반환하는 ordinal 메서드를 제공한다.

```java
// Bad
public enum Ensemble {
    SOLO, DUET, TRIO, QUARTET, QUINTET, SEXTET, SEPTET, OCTET, NONET, DECTET;

    public int numberOfMusicians() {
        return ordinal() + 1;
    }
}
```
* 상수 선언 순서를 바꾸는 순간 numberOfMusicians가 오동작하며, 이미 사용 중인 정수와 같이 값이 같은 상수는 추가할 방법이 없다.
* 또한 값을 중간에 비워둘 수 없다. 즉, 연속적인 값을 가져야만 한다.
* 해결책은 간단하다. 열거 타입 상수에 연결되는 값은 ordinal 메서드로 얻지 말고, 인스턴스 필드에 저장하도록 한다.
```java
// Good
@Getter
public enum Ensemble {
    SOLO(1), DUET(2), TRIO(3), QUARTET(4), QUINTET(5), SEXTET(6), SEPTET(7), OCTET(8);

    private final int numberOfMusicians;
    Ensemble(int size) { this.numberOfMusicians = size; }
}
```
