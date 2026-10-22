# (Effective Java Item 46) Java - 스트림에서는 부작용 없는 함수를 사용하라

* 스트림 패러다임의 핵심은 계산을 일련의 변환으로 재구성하는 부분에 있다.
* 이 때, 각 변환 단계는 가능한 한 이전 단계의 결과를 받아 처리하는 순수 함수여야 한다.
  * 순수 함수란, 오직 입력만이 결과에 영향을 주는 함수
  * 다른 가변 상태를 참조하지 않고, 함수 스스로도 다른 상태를 변경하지 않는다.
  * 스트림 연산에 건네는 함수 객체는 모두 부작용이 없어야 한다.
  * forEach 연산은 스트림 계산 결과를 보고할 때만 사용하고, 계산할 때는 사용하지 말아야 한다.

```java
// Bad
Map<String, Long> freq = new HashMap<>();

try (Stream<String> words = new Scanner(file).tokens()) {
    words.forEach(word -> {
        freq.merge(word.toLowerCase(), 1L, Long::sum);
    });
}

// Good
Map<String, Long> freq = new HashMap<>();

try (Stream<String> words = new Scanner(file).tokens()) {
    freq = words.collect(groupingBy(String::toLowerCase, counting()));
}
```

#### Collectors 클래스

* 수집기가 생성하는 객체는 일반적으로 컬렉션
* 수집기를 사용하면 스트림의 원소를 손쉽게 컬렉션으로 모을 수가 있다.
* `toList()`, `toSet()`, `toCollection()`, `toMap()` 등이 있다.

#### `toList()`

* 모든 Stream의 요소를 List 인스턴스로 수집한다.
* 특정 List를 구현하는 것이 아니다.

#### `toSet()`

* 모든 Stream의 요소를 Set 인스턴스로 수집한다.
* 중복을 거른다.

#### `toCollection()`

* toList(), toSet() 등은 특정 List, Set의 구현체를 지정할 수 없는데, toCollection을 사용하면 Collection(List, Set 등)의 구현이 가능하다.

#### `toMap()`

* 모든 Stream의 요소를 Map 인스턴스로 수집한다.
* keyMapper: Stream 요소에서 Map의 Key를 추출하는 데 사용한다.
* valueMapper: Key와 관련된 Value의 값을 추출한다.
* toMap()에서 중복 Key가 존재할 경우, IllegalStateException이 발생한다.
* BinaryOperator로 충돌 처리 방법을 지정할 수 있다.

```java
import java.util.*;
import java.util.stream.*;

public class StreamExamples {

    public static void main(String[] args) {

        List<String> names = List.of("Alice", "Bob", "Charlie", "Alice");

        // 1. toList()
        List<String> listResult = names.stream()
                .map(String::toUpperCase)
                .toList();   // Java 16+
        System.out.println("toList = " + listResult);


        // 2. toSet()
        Set<String> setResult = names.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        System.out.println("toSet = " + setResult);


        // 3. toMap()
        Map<String, Integer> mapResult = names.stream()
                .distinct()
                .collect(Collectors.toMap(
                        name -> name,       // key
                        String::length      // value
                ));
        System.out.println("toMap = " + mapResult);


        // 4. toCollection()
        LinkedList<String> linkedListResult = names.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedList::new));
        System.out.println("toCollection = " + linkedListResult);


        // (보너스) 중복 key 처리 toMap()
        Map<String, Long> countMap = names.stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> 1L,
                        Long::sum                 // 중복 key 머지
                ));
        System.out.println("중복 허용 toMap = " + countMap);
    }
}
```

#### `groupingBy()`

* 이 메서드는 입력으로 분류 함수를 받고 출력으로 원소들을 카테고리별로 모아 놓은 맵을 담은 수집기를 반환한다.
* 분류 함수는 입력받은 원소가 속하는 카테고리를 반환하게 된다.

#### `joining()`

* 이 메서드는 `CharSequence 인스턴스의 스트림에만 적용할 수 있다.
* 이 중 매개변수가 없는 joining은 단순히 원소들을 연결(Concatenate)하는 수집기를 반환하게 된다.

> Stream API에 대한 생각
>
> * 데이터의 양을 줄이는 행위는 무조건 DB에서 처리하고 제한된 데이터의 최종 계산 및 화면 맞춤형 가공은 애플리케이션에서 처리한다.
