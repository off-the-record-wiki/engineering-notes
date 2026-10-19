# Java - Optional

#### Optional이 왜 필요한지?

* NPE(NullPointerException) 문제
  * 자바에서 `null`은 값이 없음을 표현하는 가장 기본적인 방법이다.
  * 하지만 `null`을 잘못 사용하거나 `null` 참조에 대한 메서드를 호출하면 NPE가 발생하여 프로그램이 예기치 않게 종료될 수 있다.
  * 특히 여러 메서드가 연쇄적으로 호출되어 `null` 체크가 누락되면 추적이 어렵고 디버깅 비용이 증가한다.
* 가독성 저하
  * `null` 체크 로직이 누적되면 코드가 복잡해지고 가독성이 떨어진다.
* 의도가 드러나지 않음
  * 메서드 시그니처만 보고 이 메서드가 `null`을 반환할 수도 있다는 사실을 명확히 파악하기 어렵다.
  * 호출하는 입장에서는 반드시 값이 존재할 것이라고 가정했다가 런타임에 null이 나와서 문제가 생길 수 있다.
* Optional의 등장
  * 이러한 문제를 해결하고자 자바 8부터 Optional 클래스가 도입됐다.
  * Optional은 값이 있을수도 있고 없을수도 있음을 명시적으로 표현해주어 메서드의 계약이나 호출 의도를 좀 더 분명하게 드러낸다.
  * Optional을 사용하면 "빈 값"을 표현할 때 더 이상 `null` 자체를 넘겨주지 않고 `Optional.empty()`처럼 의도를 드러내는 객체를 사용할 수 있다.
  * `null` 체크 로직을 간결하게 만들고 특정 경우에 NPE가 발생할 수 있는 부분을 빌드 타임이나 IDE, 코드 리뷰에서 더 쉽게 파악할 수 있게 해준다.

#### Optional 값 생성

* `Optional.of(T value)`
* `Optional.ofNullable(T value)`
* `Optional.empty()`

#### Optioanl 값 획득

* `isPresent()`, `isEmpty()`
* `get()`
* `orElse(T other)`
* `orElseGet(Supplier<? extends T> supplier)`
* `orElseThrow()`
* `or(Supplier<? extends Optional<? extends T>> supplier)`

<br>

* `get()`메서드는 Optional 사용 시 가능하면 피해야 한다. 왜냐하면 값이 없는 상태에서 `get()`을 호출하면 예외가 터지기 때문에 안전하게 사용하려면 `isPresent()`와 같은 사전 체크가 반드시 필요하다.
* `get()`보다는 `orElse()`, `orElseGet()`, `orElseThrow()` 등의 메서드를 활용하면 좀 더 세련되고 안전하게 값을 처리할 수 있다.<br>

#### Optional 값 처리 방법

* `ifPresent()`
  * 값이 존재하면 action 실행
  * 값이 없으면 아무것도 안 함
* `ifPresentOrElse()`
  * 값이 존재하면 action 실행
  * 값이 없으면 emptyAction 실행
* `map()`
  * 값이 있으면 mapper를 적용한 결과 반환
  * 값이 없으면 Optional.empty() 반환
* `flatMap()`
  * map과 유사하나 Optional을 반환할 때 중첩되지 않고 평탄화해서 반환
* `filter()`
  * 값이 있고 조건을 만족하면 그대로 반환
  * 조건 불만족이거나 값이 없다면 Optional.empty() 반환
* `stream()`
  * 값이 있으면 단일 요소를 담은 Stream\<T> 반환
  * 값이 없으면 빈 스트림 반환

#### 즉시 평가와 지연 평가

* 즉시 평가(eager evaluation) : 값(혹은 객체)을 바로 생성하거나 계산해 버리는 것
* 지연 평가(lazy evaluation) : 값이 실제로 필요할 때까지 계산을 미루는 것

#### `orElse()` vs `orElseGet()`

```java
import java.util.Optional;
import java.util.Random;

public class OrElseGetMain {
	public static void main(String[] args) {

		Optional<Integer> optValue = Optional.of(100);
		Optional<Integer> optEmpty = Optional.empty();
		System.out.println("단순 계산");

		Integer i1 = optValue.orElse(10 + 20); // 10 + 20 계산 후 버림
		Integer i2 = optEmpty.orElse(10 + 20); // 10 + 20 계산 후 사용
		System.out.println("i1 = " + i1);
		System.out.println("i2 = " + i2);

		// 값이 있으면 그 값, 없으면 지정된 기본값 사용
		System.out.println("=== orElse ===");
		System.out.println("값이 있는 경우");
		Integer value1 = optValue.orElse(createData());
		System.out.println("value1 = " + value1);
		System.out.println("값이 없는 경우");
		Integer empty1 = optEmpty.orElse(createData());
		System.out.println("empty1 = " + empty1);

		// 값이 있으면 그 값, 없으면 지정된 람다 사용
		System.out.println("=== orElseGet ===");
		System.out.println("값이 있는 경우");
		Integer value2 = optValue.orElseGet(() -> createData());
		System.out.println("value2 = " + value2);
		System.out.println("값이 없는 경우");
		Integer empty2 = optEmpty.orElseGet(() -> createData());
		System.out.println("empty2 = " + empty2);
	}

	public static int createData() {
		System.out.println("데이터를 생성합니다...");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		int createValue = new Random().nextInt(100);
		System.out.println("데이터 생성이 완료되었습니다. 생성 값: " + createValue);
		return createValue;
	}
}
```

* `orElse(T other)` : 빈 값이면 `other`를 반환하는데 이 때, `other`를 항상 미리 계산한다.
  * 따라서 `other`를 생성하는 비용이 큰 경우 실제로 값이 있을 때도 쓸데없이 생성 로직이 실행될 수 있다.
  * `orElse()`에 넘기는 표현식은 즉시 평가하므로 즉시 평가가 적용된다.
* `orElseGet(Supplier supplier)` : 빈 값이면 `supplier`를 통해 값을 생성하기 때문에 값이 있을 경우에는 `supplier`가 호출되지 않는다.
  * 생성 비용이 높은 객체를 다룰 때는 `orElseGet()`이 더 효율적이다.
  * `orElseGet()`에 넘기는 표현식은 필요할 때만 평가하므로 지연 평가가 적용된다.

>❗생성 비용이 높다라는 개념은?
>
> * 복잡한 초기화 작업이 필요한 경우
>   * 객체 생성 시 내부적으로 많은 계산이나 리소스 준비가 필요할 경우
>   * Ex. DB 커넥션 생성, 파일 읽기, 네트워크 연결
> * 메모리 사용량이 많은 경우
>   * 큰 배열이나 컬렉션을 포함하고 있어 메모리를 많이 사용하는 객체
> * GC 부담이 커지는 경우
>   * 객체를 자주 생성하고 버리면 GC가 자주 일어나 성능이 저하
> * 생성 과정에서 외부 시스템과 연동되는 경우
>   * 객체 생성 시 외부 API를 호출하는 경우, 디스크 접근, 로깅

#### Optional Best Practice 1 - 반환 타입으로만 사용하고 필드에는 가급적 쓰지 않는다.

```java
// Bad
public class Product {
    private Optional<String> name;

}

// Good
public class Product {
   private String name; // 필드는 원시 타입 그대로 둔다.

}

// name 값을 가져올 때 필드가 null일 수도 있음을 고려해야 한다면 메서드에서 Optional을 반환할 수 있다.
public Optional<String> getNameAsOptional() {
   return Optional.ofNullable(name);
}   
```

* Optional 자체도 참조 타입이기 때문에 혹시라도 개발자가 부주의로 Optional 필드에 `null`을 할당하면 그 자체가 NPE를 발생시킬 여지를 남긴다.
* 값이 없음을 나타내기 위해 사용하는 것이 Optional인데 정작 필드 자체가 `null`이면 혼란이 가중된다.

#### Optional Best Practice 2 - 메서드 매개변수로 Optional을 사용하지 않는다.

* 자바 공식 문서에 보면 Optional은 메서드의 반환값으로 사용하기를 권장하며, 매개변수로 사용하지 말라고 명시되어 있다.
* 호출하는 측에서는 단순히 `null` 값을 전달하는 대신 `Optional.empty()`를 전달해야 하는 부담이 생기며 결국 `null`을 사용하든 `Optional.emtpy()`를 사용하든 큰 차이가 없어 가독성이 저하된다.

```java
// Bad
public void processOrder(Optional<Long> orderId) {
   // ...
}

// Good
public void processOrder(long orderId) {
   if (orderId == null) {
      System.out.println("없다.");
   }
}
```

#### Optional Best Practice 3 - 컬렉션(Collection)이나 배열 타입은 Optional로 감싸지 않는다.

* 컬렉션 자체는 비어있는 상태를 표시할 수 있다.
* 따라서 `Optional<List<T>>`처럼 다시 감싸주면 빈 리스트가 이중으로 표현이 되기 때문에 혼란을 야기한다.

```java
// Bad
public Optional<List<String>> getUserRoles(String userId) {
   List<String> userRoleList = ...
   
   if (foundUser) {
      return Optional.of(userRoleList);
   } else {
      return Optional.empty();
   }
}

// Good
public List<String> getUserRoles(String userId) {
   if (!foundUser) {
      return Collections.emptyList();
   }
   return userRolesList;
}
```

#### Optional Best Practice 4 - `isPresent()`와 `get()` 조합을 직접 사용하지 않는다.

* Optional의 `get()` 메서드는 가급적 사용하지 않아야 한다.
* `isPresent() { opt.get() }`는 사실상 null 체크와 다를 바가 없으며 깜빡하면 `NoSuchElementException` 예외가 발생할 위험이 있다.
* 대신 `orElse`, `orElseGet`, `orElseThrow`, `ifPresentOrElse`, `map`, `filter` 등의 메서드를 활용하면 간결하고 안전하게 처리가 가능하다.

```java
// Bad
public static void main(String[] args) {
   Optional<String> optStr = Optional.ofNullable("Hello");

   if (optStr.isPresent()) {
      System.out.println(optStr.get());
   } else {
      System.out.println("Nothing");
}

// Good
public static void main(String[] args) {
   Optional<String> optStr = Optional.ofNullable("Hello");

   // 1) orElse
   System.out.println(optStr.orElse("Nothing");

   // 2) ifPresentOrElse
   optStr.ifPresentOrElse(
      System.out::println,
      () -> System.out.println("Nothing");
   
   // 3) map
   int length = optStr.map(String::length).orElse(0);
   System.out.println("Length : " + length);
```

#### Optional Best Practice 5 - `orElseGet()` vs `orElse()` 차이를 분명히 이해하고 사용해라

* `orElse(T other)`는 항상 `other`를 즉시 생성하거나 계산한다.
* `orElseGet(Supplier<? extends T>)`는 필요할 때만 `Supplier`를 호출한다.
* 값이 이미 존재하는 경우에는 `Supplier`가 호출되지 않으므로 비용이 큰 연산을 뒤로 미룰 수 있다.
* 비용이 크지 않다면 간단하게 `orElse()`를 사용하고 복잡하고 비용이 큰 객체, 그리고 Optional 값이 이미 존재할 가능성이 높다면 `orElseGet()`을 사용한다.

#### Optional Best Practice 6 - 무조건 Optional이 좋은 것만은 아니다

* Optional이 편의성과 안전성을 높여주는 것은 맞지만 모든 곳에서 무조건 사용하는 곳은 오히려 코드 복잡성을 증가시킬 수 있다.
  * 항상 값이 있는 상황 - 비즈니스 로직상 null이 될 수 없는 경우라면 방어적 코드로 예외를 던지거나 일반 타입을 사용하는 것이 낫다.
  * 값이 없으면 예외를 던지는 것이 자연스러운 상황 - ID 기반으로 엔티티를 찾아야 하는 경우, Optional 대신 예외를 던지는게 나을 수 있다.
  * 흔히 비는 경우가 아니라 흔히 채워져 있는 경우 - Optional을 매번 쓰면 `.get()`, `orElse()`, `orElseThrow()` 등 처리가 강제되기 때문에 오히려 코드가 장황해진다.
  * 성능이 극도로 중요한 로우레벨 코드 - Optional은 래퍼 객체를 생성하기에 수많은 객체가 생겨나는 부분에서는 성능에 영향을 줄 수 있다.

#### Optional Best Practice 7 - 클라이언트 메서드 vs 서버 메서드

* Optional을 고려할 떄 가장 중요한 핵심은 Optional을 반환하는 코드를 호출하는 클라이언트 메서드에 있다.
  * 이 로직이 `null`을 반환할 수 있는가?
  * `null`이 가능하다면 호출하는 사람 입장에서 값이 없을 수도 있다는 사실을 명시적으로 인지할 필요가 있는지?
  * `null`이 적절하지 않고 예외를 던지는게 더 맞지 않을지?
