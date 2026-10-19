# Java ‐ String

#### String 클래스 - 비교

```java
// String 클래스 내부의 equals() 메서드 구조 - 메서드 오버라이딩이 되어 있어 Object의 equals()와는 다르다.
public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        return (anObject instanceof String aString)
                && (!COMPACT_STRINGS || this.coder == aString.coder)
                && StringLatin1.equals(value, aString.value);
}
```
```java
public class StringEqualsMain1 {

	public static void main(String[] args) {

		String s1 = new String("Hello");
		String s2 = new String("Hello");

		System.out.println("s1 == s2 -> " + (s1 == s2));
		System.out.println("s1.equals(s2) -> " + (s1.equals(s2)));

		String s3 = "Hello";
		String s4 = "Hello";

		System.out.println("s3 == s4 -> " + (s3 == s4));
		System.out.println("s3.equals(s4) -> " + (s3.equals(s4)));
	}
}
```

> 실행 결과

```
s1 == s2 -> false
s1.equals(s2) -> true
s3 == s4 -> true
s3.equals(s4) -> true
```

* `new String()`을 사용해서 각각 인스턴스를 생성한 경우 동일성 측면에서는 false, 허나 String 클래스 내부적으로 메서드 오버라이딩된 `equals()`에 의해 동등성 측면에서는 true가 된다. 
* 리터럴을 사용한 경우 동일성과 동등성 측면에서 true가 된다.

> ❓문자열 풀(String Pool)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/f5461674-e6ed-4dab-9ae8-402f5341e590.png)

> * 풀(Pool)은 자원이 모여있는 곳을 말한다.<br>
> * 프로그래밍에서 풀(Pool)은 공용 자원을 모아둔 곳을 말한다. 여러 곳에서 함께 사용할 수 있는 객체를 필요할 때마다 생성하고, 제거하는 것은 비효율적이다.<br>
> * 대신에 이렇게 문자열 풀에 필요한 String 인스턴스를 미리 만들어두고 여러 곳에서 재사용할 수 있다면 성능과 메모리를 최적화 할 수 있다.<br>
> * 참고로 문자열 풀은 힙 영역을 사용한다. 그리고 문자열 풀에서 문자를 조회할 때는 해시 알고리즘을 사용하기 때문에 매우 빠른 속도로 원하는 String 인스턴스를 찾을 수가 있다.<br>

#### String 클래스 주요 메서드 정리

* [Java - String 클래스 지원 메서드](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)

#### StringBuilder - 가변 String

* String 클래스를 기본적으로 불변이다.
* 불변인 String 클래스의 내부 값은 변경할 수 없다. 따라서 변경된 값을 기반으로 String 객체를 생성한다.
* 불변인 String 클래스의 단점은 문자를 더하거나 변경할 때마다 계속해서 새로운 객체를 생성해야 한다는 점이다.
* 문자를 자주 더하거나 변경해야 하는 상황이라면 더 많은 String 객체를 만들고, GC해야 한다. 결과적으로 컴퓨터의 CPU, 메모리, 자원을 더 많이 소모하게 된다.
* 그리고 문자열 크기가 클수록, 문자열을 더 자주 변경할수록 시스템의 자원을 더 많이 소모한다.

```java
public class StringBuilderMain1 {

	public static void main(String[] args) {

		StringBuilder sb = new StringBuilder();
		sb.append("A");
		sb.append("B");
		sb.append("C");
		sb.append("D");
		sb.append("E");

		// StringBuilder(가변) -> String(불변)
		System.out.println(sb.toString());
	}
}
```

* String은 불변이다. 즉, 한 번 생성되면 그 내용을 변경할 수 없다. 따라서 문자열에 변화를 주려고 할 때마다 새로운 객체가 생성되고 기존 객체는 버려진다. 이 과정에서 메모리와 처리 시간을 더 많이 소모한다.
* StringBuilder는 가변이다. 하나의 StringBuilder 안에서 문자열을 추가, 삭제, 수정할 수 있으며 이 때마다 새로운 객체를 생성하지 않는다. 이로 인해 메모리 사용을 줄이고 성능을 향상시킬 수 있다. 단 사이드 이펙트를 주의해야 한다.

#### 메서드 체이닝(Method Chaining)

* 메서드 체이닝이 가능한 이유는 반환 값이 바로 자기 자신의 참조값이기 때문이다.

```java
public class MethodChainingMain1 {

	public static void main(String[] args) {

                // 반환 타입이 자기 자신의 참조값
		StringBuilder result = new StringBuilder("A").append("B").append("C").append("D").append("E").append("F");
		System.out.println(result);
	}
}
```
