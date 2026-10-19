# Java - Lambda

#### 람다가 필요한 이유

* 중복을 제거하고 좋은 코드를 유지하는 핵심은 변하는 부분과 변하지 않는 부분을 분리하는 것
* 값 매개변수화(=동적 파라미터 전달) 

#### 람다 정의

* 자바 8부터 도입된 람다는 자바에서 함수형 프로그래밍을 지원하기 위한 기능이다.
* 람다는 익명 함수로 분류되기 때문에 이름 없이 함수를 표현한다.

#### 함수형 인터페이스(Functional Interface)

* 정확히 하나의 추상 메서드를 가지는 인터페이스를 말한다.
* 람다는 추상 메서드가 하나인 함수형 인터페이스에만 할당할 수 있다.
* 단일 추상 메서드를 줄여서 SAM(Single Abstract Method)라고 한다.
* 람다를 사용할 인터페이스라면 클래스 상단에 `@FunctionalInterface` 어노테이션을 달아두는 것을 권장한다.

#### 람다와 생략

```java
@FunctionalInterface
public interface MyFunction {
	int apply(int a, int b);
}
```
```java
public class Main {
	public static void main(String[] args) {

		// Ex1
		MyFunction myFunction1 = (int a, int b) -> {
			return a + b;
		};
		System.out.println(myFunction1.apply(1, 2));

		// Ex2
		MyFunction myFunction2 = (int a, int b) -> a + b;
		System.out.println(myFunction2.apply(3, 4));

		// Ex3
		MyFunction myFunction3 = (int a, int b) -> {
			System.out.println("Lambda Expression");
			return a + b;
		};
		System.out.println(myFunction3.apply(5, 6));

                // Ex4
                MyFunction myFunction = (a, b) -> {
			System.out.println("hello Lambda with parameters: " + a + ", " + b);
			return a + b;
		};
		System.out.println(myFunction.apply(10, 20));
	}
}
```
```java
@FunctionalInterface
public interface Procedure {
	void run();
}
```
```java
public class ProcedureMain {
	public static void main(String[] args) {

		// Ex1
		Procedure procedure1 = () -> {
			System.out.println("hello Lambda!");
		};
		procedure1.run();

		// Ex2
		Procedure procedure2 = () -> System.out.println("hello Lambda!");
		procedure2.run();
	}
}
```

* 매개변수 타입 : 생략 가능, 그러나 필요하다면 명시적으로 작성
* 반환 타입 : 문법적으로 명시할 수 없고 식 결과를 보고 컴파일러가 항상 추론한다.
* 람다는 보통 간결하게 사용하는 것을 권장
  * 단일 표현식이라면 중괄호와 리턴은 생략해라.
  * 타입 추론을 통해 매개변수 타입을 생략해라.

#### 람다의 전달

```java
public class LambdaPass {
	public static void main(String[] args) {
		MyFunction addFunction = getOperation("+");
		System.out.println("Result: " + addFunction.apply(5, 3));

		MyFunction subtractFunction = getOperation("-");
		System.out.println("Result: " + subtractFunction.apply(5, 3));

		MyFunction multiplyFunction = getOperation("*");
		System.out.println("Result: " + multiplyFunction.apply(5, 3));

		MyFunction divideFunction = getOperation("/");
		System.out.println("Result: " + divideFunction.apply(5, 3));
	}

	static MyFunction getOperation(String operator) {
		switch (operator) {
			case "+":
				return (a, b) -> a + b;
			case "-":
				return (a, b) -> a - b;
			case "*":
				return (a, b) -> a * b;
			case "/":
				return (a, b) -> {
					if (b == 0) {
						throw new IllegalArgumentException("Division by zero is not allowed.");
					}
					return a / b;
				};
			default:
				throw new IllegalArgumentException("Invalid operator: " + operator);
		}
	}
}
```

#### 고차 함수

* 람다는 함수형 인터페이스를 구현한 익명 클래스와 같은 개념으로 이해하면 된다.
* 람다를 변수에 대입한다는 것은 람다 인스턴스의 참조값을 대입하는 것이고 람다를 메서드의 매개변수나 반환값으로 넘긴다는 것 역시 람다 인스턴스 참조값을 넘긴다는 것과 같다.
* 람다를 변수에 대입, 람다는 메서드 매개변수에 전달, 람다를 메서드에서 반환하는 것 모두 가능하다.

#### 고차 함수(Higher-Order Function)

* 함수(람다)를 인자로 받는 함수
* 함수(람다)를 반환하는 함수

```java
static void calculate(MyFunction function) {
   // ...
}
```
```java
static MyFunction getOperation(String operator) {
   // ...
   return (a, b) -> a + b;
}
```
