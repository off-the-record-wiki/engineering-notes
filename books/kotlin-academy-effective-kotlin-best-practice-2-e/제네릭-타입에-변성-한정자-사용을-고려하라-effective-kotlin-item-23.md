# (Effective Kotlin Item 23) Kotlin - 제네릭 타입에 변성 한정자 사용을 고려하라

#### 제네릭 타입에 변성 한정자 사용을 고려하라

* 코틀린 함수 타입에서는 변성 한정자(variance modifier)인 `in`과 `out`을 지정할 수 있다.
* 그들은 타입 매개변수의 사용 방법을 지정하는 데 사용된다.
* `in`은 타입 매개변수를 입력으로만 사용될 수 있음을 의미하고 `out`은 타입 매개변수가 출력으로만 사용될 수 있다는 것을 의미한다.
* 그 결과, 제네릭 타입을 사용할 때보다 많은 유연성을 제공한다. 예를 들어, `List` 타입 매개변수에는 `out` 한정자가 있어 `List<Int>`를 `List<Number>`의 하위 타입으로 사용할 수 있다.

```kotlin
public interface Iterable<out T> {
    public operator fun iterator(): Iterator<T>
}

public interface Collection<out E> : Iterable<E> {
    public val size: Int
    public operator fun contains(element: @UnsafeVariance E): Boolean
    public fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    override fun iterator(): Iterator<E>
}

public interface List<out E> : Collection<E> {
    public operator fun get(index: Int): E    // E를 반환(생산)만 함
    public fun indexOf(element: @UnsafeVariance E): Int
}

public interface Set<out E> : Collection<E>
```
