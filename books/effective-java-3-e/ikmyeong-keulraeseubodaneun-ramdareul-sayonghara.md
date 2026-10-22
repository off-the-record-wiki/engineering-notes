# (Effective Java Item 42) Java ‐ 익명 클래스보다는 람다를 사용하라

```java
// 익명 클래스
Collections.sort(words, new Comparator<String>() {
	public int compare(String s1, String s2) {
		return Integer.compare(s1.length(), s2.length());
	}
});
```
* 람다 이전에 함수 객체를 사용하던 방법
* 익명 클래스 방식은 코드가 길어지기 때문에 함수형 프로그래밍에 적합하지 않다.

```java
// 람다(Lambda)
Collections.sort(words, (s1, s2) -> Integer.compare(s1.length(), s2.length()));
```
* 자바 8부터 추상 메서드 하나만 가지는 인터페이스를 함수형 인터페이스라고 부르며 특별하게 사용된다.
* 함수형 인터페이스의 인스턴스를 람다식으로 만들 수 있다.
* 람다는 익명 클래스와 비슷한 개념이지만 코드가 훨씬 간결하다.

```java
public enum Operation {
	PLUS("+", (x, y) -> x + y),
	MINUS("-", (x, y) -> x - y),
	TIMES("*", (x, y) -> x * y),
	DIVIDE("/", (x, y) -> x / y);

	private final String symbol;
	private final DoubleBinaryOperator op;

	Operation(String symbol, DoubleBinaryOperator op) {
		this.symbol = symbol;
		this.op = op;
	}

	@Override public String toString() {
		return symbol;
	}

	public double apply(double x, double y) {
		return op.applyAsDouble(x, y);
	}
}
```

#### 람다와 익명 클래스 비교

* 람다
  * 함수 객체를 구현하는 데 간결한 람다를 적극 활용하라.
  * 람다의 this는 바깥 인스턴스를 가리킨다. 자신의 참조가 불가능하다.
  * 람다는 이름이 없고 문서화가 불가능하다. 코드 자체로 설명되지않거나 3줄 이상 나올 경우 람다를 쓰지 말것
* 익명 클래스
  * 람다는 자신을 참조할 수 없으므로 함수 객체가 자신을 참조해야한다면 익명 클래스를 사용하라.
