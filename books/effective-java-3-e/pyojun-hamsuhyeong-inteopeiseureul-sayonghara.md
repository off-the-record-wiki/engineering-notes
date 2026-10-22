# (Effective Java Item 44) Java - 표준 함수형 인터페이스를 사용하라

* 함수형 인터페이스를 새로 정의하기보다 기본 함수형 인터페이스를 사용하는 것을 권장한다.

```java
public class Cache<K, V> extends LinkedHashMap<K, V> {

    private final int limit;

    public Cache(int limit) {
        this.limit = limit;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > this.limit;
    }
}
```
```java
public class Cache<K, V> extends LinkedHashMap<K, V> {

    private final BiPredicate<Map<K, V>, Map.Entry<K, V>> biPredicate;

    public Cache(BiPredicate<Map<K, V>, Map.Entry<K, V>> biPredicate) {
        this.biPredicate = biPredicate;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return biPredicate.test(this, eldest);
    }
}
```

#### `UnaryOperator<T>` : 인수 1개, 인수의 타입 = 반환 타입

* 함수 시그니처 : `T apply(T t)`

```java
UnaryOperator<Integer> square = x -> x * x;
Integer result = square.apply(5);
```
```java
UnaryOperator<String> toUpperCase = String::toUpperCase;
String result = toUpperCase.apply("java");
```

#### `BinaryOperator<T>` : 인수 2개, 인수의 타입 = 반환 타입

* 함수 시그니처 : `T apply(T t1, T t2)`

```java
BinaryOperator<Integer> add = (a, b) -> a + b;
System.out.println("Sum: " + add.apply(5, 3));
```
```java
BinaryOperator<Integer> max = BinaryOperator.maxBy(Integer::compareTo);
System.out.println("Max: " + max.apply(10, 20));
```

#### `Predicate<T>` : 인수 1개, 반환 타입은 boolean

* 함수 시그니처 : `boolean test(T t)`

```java
Predicate<String> isLongerThan5 = s -> s.length() > 5;
Predicate<String> containsA = s -> s.contains("a");
```

#### `Function<T, R>` : 인수 타입 != 반환 타입

* 함수 시그니처 : `R apply(T t)`

```java
Function<Integer, String> intToString = num -> "Number: " + num;
```

#### `Supplier<T>` : 인수 X, 반환 타입 O

* 함수 시그니처 : `T get()`

```java
Supplier<String> supplier = () -> "Hello Supplier!";
```

#### `Consumer<T>` : 인수 1개, 반환 타입 X

* 함수 시그니처 : `void accept(T t)`

```java
Consumer<String> printer = s -> System.out.println("입력: " + s);
```

#### 기본 함수형 인터페이스에 박싱된 기본 타입을 넣어 사용하지 말라

```java
Function<Integer, Integer> f = x -> x + 1;
```
* 표준 함수형 인터페이스 대부분은 Primitive Type만 지원한다.
* 그렇다고 기본 함수형 인터페이스에 박싱된 기본 타입을 넣어 사용하지는 말아야 한다.
* 내부적으로 int -> Integer로 박싱이 되고 연산 시 Integer -> int 언박싱, 결과를 리턴할 때, int -> Integer 박싱이 이루어져 반복 시 성능 저하가 발생한다.

#### 표준 함수형 인터페이스 대신 직접 구현해야 할 때도 있다.

```java
@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);
}
```
* 자주 쓰이며, 이름 자체가 용도를 명확히 설명해준다.
* 반드시 따라야 하는 규약이 있다.
* 유용한 디폴트 메서드를 제공할 수 있다.

#### 함수형 인터페이스를 직접 구현할 때의 주의사항

* `@FunctionalInterface` 어노테이션을 붙여야 한다.
