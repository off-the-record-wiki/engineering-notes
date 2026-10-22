# (Effective Java Item 55) Java ‐ 옵셔널 반환은 신중히 하라

* 자바 8 전에는 메서드가 특정 조건에서 값을 반환할 수 없을 때 취할 수 있는 선택지가 2가지가 있었다.
* 예외를 던지거나, `null`을 반환하는 것이 있다.
* 그러나 예외는 진짜 예외적인 상황에서만 사용해야 하며 예외를 생성할 때, 스택 추적 전체를 캡처하기 때문에 비용도 만만치 않다.
* `null`을 반환하면 문제가 생기지 않지만, 그 나름대로의 문제가 있다.
* `null`을 반환할 수 있는 메서드를 호출하면 별도의 `null` 처리 코드가 필요하다.
* `null` 처리를 무시하고 반환된 `null`값을 어딘가에 저장해두게 된다면 언젠가 NPE가 발생할 수 있다. 그것도 근본적인 원인 즉, `null`을 반환하게 한 실제 원인과 전혀 무관한 지점에서 말이다.
* 자바 버전이 8로 올라가면서 다른 선택지가 생겼는데 그것은 바로 `Optional`이다.

#### Optional

* `Optional<T>`는 `null`이 아닌 `T` 타입의 참조를 하나 담거나, 혹은 아무것도 담지 않을 수 있다.
* 아무것도 담지 않은 옵셔널은 "비었다"고 말한다.
* 반대로 어떤 값을 담은 옵셔널은 "비지 않았다"고 말한다.
* 옵셔널은 원소를 최대 1개 가질 수 있는 불변 컬렉션이다. `Optional<T>`가 `Collection<T>`를 구현하진 않았지만, 원칙적으로 그렇다.

```java
// 컬렉션에서 최댓값을 구해 Optional<E>로 반환한다.
public static <E extends Comparable<T>> E max(Collection<E> c) {
    if (c.isEmpty()) {
        throw new IllegalArgumentException("빈 컬렉션");
    }
  
    E result = null;
    for (E e : c) {
        if (result == null || e.compareTo(result) > 0) {
            result = Objects.requireNonNull(e);
        }
    }
    return result;
}
```
* 빈 옵셔널은 `Optional.empty()`로 만들고, 값이 든 옵셔널은 `Optional.of(value)`로 생성했다.
* 만약 `Optional.of(value)`에 null을 넣게 되면 NPE를 던지게 되니 이 부분도 주의해야 한다.
* `null`값도 허용하는 옵셔널을 만들고자 한다면 `Optional.ofNullable(value)`를 사용하면 된다.
* 옵셔널을 반환하는 메서드에서는 절대 null을 반환하지 말도록 하자. 이는 Optional 설계를 완전히 무시하는 행위다.
* 그렇다면 `null`을 반환하거나 예외를 던지는 대신 `Optional` 반환을 택해야 하는 기준은 무엇인가? 옵셔널은 검사 예외와 취지가 비슷하다.
* 메서드가 옵셔널을 반환한다면 클라이언트는 값을 받지 못했을 때 취해야 할 행동을 선택해야 한다.

#### 옵셔널 활용 1 : 기본값을 설정한다.

```java
String lastWordInLexicon = max(words).orElse("단어 없어");
```

#### 옵셔널 활용 2 : 상황에 맞는 예외를 던진다.

```java
Toy myToy = max(toys).orElseThrow(TemperTantrumException::new);
```

#### 옵셔널 활용 3 : 항상 값이 채워져 있다고 확신할 수 있다면 곧바로 값을 꺼낸다.

```java
// 단, 값이 없는 상태에서 get을 호출해 꺼내게 된다면 NoSuchElementException을 던지게 된다.
Element lastNobleGas = max(Elements.NOBLE_GASES).get();
```

#### 옵셔널 활용 4 : 기본 값 설정 비용이 큰 경우 `Supplier<T>`를 인수로 받는 `orElseGet`을 사용하면 초기 설정 비용을 낮출 수 있다.

```java
public T orElseGet(Supplier<? extends T> other)

public static String orElseGetBenchmark() {
    return Optional.of("baeldung").orElseGet(() -> getRandomName());
}
```

#### 옵셔널 활용 5 : isPresent 메서드를 사용한다.

```java
public class ParentPid {
    public static void main(String[] args) {
        ProcessHandle ph = ProcessHandle.current();

        Optional<ProcessHandle> parentProcess = ph.parent();
        System.out.println("Parent PID: " + (parentProcess.isPresent() ?
                String.valueOf(parentProcess.get().pid()) : "N/A"));

        System.out.println("Parent PID: " +
            ph.parent().map(h -> String.valueOf(h.pid())).orElse("N/A"));
    }
}
```

* `isPresent`메서드는 옵셔널이 채워져 있으면 true, 비어있으면 false를 반환하도록 설계되어 있다.
* `isPresent`를 사용하기 전에 앞의 옵셔널 활용 1~4로 표현할 수 있는지 면밀히 검토하자

#### 스트림을 사용할 경우

* 자바 8의 스트림을 이용하면서 같이 사용할 수 있다.
* `(Optional::isPresent)`로 그 값을 꺼내 `(Optional.get)`스트림에 매핑한다.
```java
streamOfOptionals
    .filter(Optional::isPresent)
    .map(Optional::get)
```
* 자바 9부터는 `Optional`을 `Stream`으로 변환해주는 `Optional.stream()` 메서드가 추가되었다.
* `Optional`에 값이 있으면 그 값을 원소로 담은 스트림으로, 값이 없다면 빈 스트림으로 변환된다.
* 이를 `Stream`에서 제공하는 `flatMap` 메서드를 사용하면 아래와 같이 바꿀 수 있다.
```java
streamOfOptionals.flatMap(Optional::stream)
```

#### 옵셔널을 사용하면 안되는 경우

* 컬렉션, 스트림, 배열, 옵셔널 같은 컨테이너 타입의 경우 옵셔널로 감싸면 안 된다.
* 빈 `Optional<List<T>>`를 반환하기보다는 빈 `List<T>`를 반환하는 게 좋다.

#### 옵셔널을 사용하면 좋은 경우

* 결과를 알 수 없으며, 클라이언트가 상황에 대해 특수한 처리를 해야한다면 `Optional<T>`를 반환한다.
* 단, `Optional`을 사용하면 새로 할당되고 객체를 초기화하며 값을 꺼내기 위해 메서드를 호출하는 단계를 거치기 때문에 성능이 저하될 수 있다는 점은 염두에 두어야 한다.

#### int, long, double 전용 옵셔널 클래스가 있음을 기억하고 박싱된 기본 타입을 담은 옵셔널을 반환하는 일은 없도록 해야 한다.

* `OptionalInt`, `OptionalLong`, `OptionalDouble`와 같은 것들이 기본 타입 전용 옵셔널 클래스이다. 이 클래스들은 박싱(기본 -> 참조)을 하지 않기 때문에 메모리 효율이 좋다.
* 반면 `Optional<Integer>`, `Optional<Long>`, `Optional<Double>`와 같은 박싱된 기본 타입, 즉, 객체를 담는 일반 `Optional` 클래스로 불필요한 객체 생성을 유발하여 성능 및 메모리 문제를 발생시킬 수 있다.

#### Optional은 Map의 값으로 절대 사용해선 안 된다.

* Map 안에 Key가 없다는 사실을 나타내는 방법은 2가지이다.
  * 키 자체가 없는 경우
  * 키는 있지만 그 키가 속이 빈 옵셔널인 경우
* 복잡성을 높여 오류 가능성을 오히려 높이기 때문에 사용하지 않는 것을 권장한다. 옵셔널을 컬렉션의 키, 값, 원소나 배열 원소로 사용하는 것이 적절한 상황은 거의 없다고 보아야 한다.
