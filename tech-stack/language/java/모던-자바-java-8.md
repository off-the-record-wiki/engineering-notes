# Java ‐ Java 8+

#### 함수형 인터페이스와 람다식

* 함수형 인터페이스(Functional Interface)
  * 단, 하나의 추상 메서드만 가지는 인터페이스
  * 자바에서 람다식을 사용하기 위한 기반이 되는 인터페이스
  * `@FunctionalInterface` 어노테이션을 사용하여 명확히 정의 가능
  * 함수형 인터페이스를 사용하면 람다식을 활용하여 코드 간결화 가능

```java
@FunctionalInterface
interface MyFunction {
    int apply(int x, int y); // 단 하나의 추상 메서드
}
```

* 람다식(Lambda Expression)
  * 익명 함수를 표현하는 방법
  * 코드를 간결하게 작성할 수 있으며, 함수형 인터페이스와 함께 사용

```java
MyFunction add = (a, b) -> a + b; // 람다식을 사용하여 함수 구현
System.out.println(add.apply(3, 5)); // 출력: 8

Predicate<Integer> isEven = n -> n % 2 == 0;
System.out.println(isEven.test(10)); // 출력: true
```

#### 컬렉션과 Stream API

* Collections (컬렉션)
  * 데이터를 저장하고 관리하는 자료구조 (List, Set, Map 등)
  * 외부 반복을 사용하여 데이터를 처리
* Stream (스트림)
  * 컬렉션에서 데이터를 효율적으로 처리할 수 있도록 도와주는 API
  * 내부 반복을 사용하여 map(), filter(), reduce() 같은 함수형 연산 수행 가능
  * 병렬 처리 가능(parallelStream() 사용)
* 스트림의 병렬 처리
  * 여러 개의 쓰레드를 사용해 작업을 나눠 동시에 처리하는 방식으로 데이터가 많고 CPU Bouund 작업의 경우 `parallelStream()`을 사용하면 성능을 향상시킬 수 있으나 데이터가 적은 경우에는 오히려 성능이 저하될 수 있다.
