# Java - Functional Interface

## 📚 함수형 인터페이스와 제네릭

```java
public class GenericFunctionalInterfaceMain {

	public static void main(String[] args) {

		GenericFunction<String, String> upperCaseFunction = String::toUpperCase;
		String result = upperCaseFunction.apply("hello");
		System.out.println(result);

		GenericFunction<Integer, Integer> squareFunction = x -> x * x;
		Integer squareResult = squareFunction.apply(5);
		System.out.println(squareResult);
	}

        // 함수형 인터페이스 정의
        // T : 파라미터 타입
        // R : 반환 타입
	@FunctionalInterface
	interface GenericFunction<T, R> {
		R apply(T t);
	}
}
```

* 제네릭을 사용하면 동일한 구조의 함수형 인터페이스를 다양한 타입에 맞춰 활용할 수 있다.
* 결론적으로 코드의 중복을 줄이고 유지보수성을 높이는데 큰 도움이 된다.

## 📚 기본 함수형 인터페이스 정리

```java
package java.util.function;

@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}
```

* 하나의 매개변수를 받고 결과를 반환하는 함수형 인터페이스이다.
* 입력값\<T>를 받아서 다른 반환 타입의 출력값\<R>을 반환하는 연산을 표현할 때 사용한다.

```java
package java.util.function;

@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}
```

* 입력값\<T>만 받고 결과를 반환하지 않는 연산을 수행하는 함수형 인터페이스이다.
* Ex. 컬렉션에 값 추가, 콘솔에 결과 출력, 로그를 작성, DB 저장 등

```java
package java.util.function;

@FunctionalInterface
public interface Supplier<T> {
    T get();
}
```

* 입력을 받지 않고 어떤 데이터를 공급해주는 함수형 인터페이스이다.
* 객체나 값 생성, 지연 초기화 등에 주로 사용된다.

```java
package java.lang;

@FunctionalInterface
public interface Runnable {
    void run();
}
```

* 멀티 쓰레드에서 쓰레드 작업을 정의할 때 사용된다.

## 📚 특화 함수형 인터페이스 정리

```java
package java.util.function;

@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);
}
```

* 입력 값을 받아서 true, false로 구분하는 함수형 인터페이스이다.
* 조건 검사, 필터링 등의 용도로 많이 사용된다.
* predicate 자체는 수학/논리학에서 "술어"를 의미하며, 참/거짓을 판별하는 명제를 표현한다.

```java
package java.util.function;

@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T> {
    T apply(T t);
}
```

* Function\<T, T>를 상속받기 때문에 입력과 반환 타입이 반드시 같아야 한다.

```java
@FunctionalInterface
public interface BinaryOperator<T> extends BiFunction<T,T,T> {
    T apply(T t1, T t2);
}
```

* 같은 타입의 2개 파라미터를 받아 같은 타입의 결과를 반환할 때 사용된다.

## 📚 기타 함수형 인터페이스 정리

* BiXXX 시리즈 : 입력 값이 2개 이상
* TriXXX 시리즈 : 입력 값이 3개 이상
