# (Effective Java Item 64) Java ‐ 객체는 인터페이스를 사용해 참조하라

* 적합한 인터페이스가 있다면 매개변수뿐 아니라 반환값, 변수, 필드를 전부 인터페이스 타입으로 선언하자.
* 객체의 실제 클래스를 사용해야 할 상황은 "오직" 생성자로 초기화할 때 뿐이다.
```java
// Bad
LinkedHashSet<Son> sonSet = new LinkedHashSet<>();
```
```java
// Good
Set<Son> sonSet = new LinkedHashSet<>();
```

#### String, BigInteger 같은 값 클래스
```java
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {

}
```

#### 클래스 기반으로 작성된 프레임 워크가 제공하는 객체
```java
public abstract class OutputStream implements Closeable, Flushable {

}

OutputStream outputStream = new FileOutputStream("");
```
* `OutputStream`을 구현하는 클래스는 `FileOutputStream`, `ByteArrayOutputStream`가 있다.
* 확장 기능을 사용하기 위해선 클래스를 참조해야 한다.
* 특정 구현 클래스 보다는 기반 클래스인 `OutputStream`을 사용하는 것이 더 좋다.

#### 인터페이스에는 없는 특별한 메소드를 제공하는 클래스
* `PriorityQueue`의 경우 `Queue` 인터페이스에 없는 `comparator()` 메서드를 제공한다.
* 클래스 타입을 직접 사용하는 경우는 이런 특정 메소드를 사용해야하는 상황으로 최소화시켜야하며 남발해선 안된다.
