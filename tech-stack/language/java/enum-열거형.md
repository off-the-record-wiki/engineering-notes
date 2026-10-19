# Java ‐ ENUM

#### 타입 안전 열거형 패턴

* 타입 안전 열거형 패턴을 사용하면 나열된 항목만 사용할 수 있다는 것이 핵심이다.
* 타입 안전 열거형 패턴(Type-Safe Enum Pattern)의 장점
  * 타입 안정성 향상 : 정해진 객체만을 사용할 수 있기 때문에 잘못된 값을 입력하는 문제를 근본적으로 방지할 수 있다.
  * 데이터 일관성 : 정해진 객체만 사용하므로 데이터의 일관성이 보장된다.

#### 열거형 - Enum 타입

* 자바는 타입 안전 열거형 패턴을 매우 편리하게 사용할 수 있는 열거형(Enum Type)을 제공한다.
* Enumeration은 일련의 명명된 상수들의 집합을 정의하는 것을 말하며, 이러한 상수들을 사용하여 코드 내에서 미리 정의된 값들의 집합을 나타낸다.

```java
public enum Grade {
    BASIC,
    GOLD,
    DIAMOND
}
```

#### 열거형 - 주요 메서드

* `values()` : 모든 ENUM 상수를 포함하는 배열을 반환한다.
* `valueOf(String name)` : 주어진 이름과 일치하는 ENUM 상수를 반환한다.
* `name()` : ENUM 상수의 이름을 문자열로 반환한다.
* `ordinal()` : ENUM 상수의 선언 순서(0부터 시작)를 반환한다.(사용 지양)
* `toString()` : ENUM 상수의 이름을 문자열로 반환한다.

```java
public class GradeMain {

	public static void main(String[] args) {

		// 모든 ENUM 반환
		Grade[] grades = Grade.values();
		for (Grade grade : grades) {
			System.out.println("grade.name = " + grade.name() + "ordinal = " + grade.ordinal());
		}

		// 입력받은 문자열을 ENUM으로 변환
		String input = "GOLD";
		Grade grade = Grade.valueOf(input);
		System.out.println("gold = " + grade);
	}
}
```

* 열거형은 `java.lang.Enum`을 강제로 상속 받는다.
* 열거형은 이미 `java.lang.Enum`을 상속 받았기 때문에 추가로 다른 클래스를 상속을 받을 수 없다.
* 열거형은 인터페이스를 구현할 수 있다.
* 열거형에 추상 메서드를 선언하고, 구현할 수 있다.
