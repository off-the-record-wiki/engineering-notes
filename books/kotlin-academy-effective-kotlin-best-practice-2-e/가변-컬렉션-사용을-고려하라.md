# (Effective Kotlin Item 59) Kotlin - 가변 컬렉션 사용을 고려하라

#### 가변 컬렉션 사용을 고려하라

* 불변 컬렉션 대신 가변 컬렉션을 사용할 때의 가장 큰 장점은 성능이 더 빠르다는 것이다.
* 불변 컬렉션에 요소를 추가하게 되면 새로운 컬렉션을 생성하고, 기존 요소들을 전부 추가한 뒤, 마지막에 인수로 들어온 요소를 추가한다.
* 대규모 컬렉션에서 요소를 추가할 경우, 모든 요소를 새로운 컬렉션에 추가하게 되면 성능이 저하된다.
* 따라서 성능을 높이려면 가변 컬렉션을 사용하는 것이 좋으며, 특히 요소를 추가할 때 큰 효과가 있다.
* 다만 불변 컬렉션을 사용하면 안정성 측면에서 가변 컬렉션보다 더 좋다.
* 하지만 지역변수는 동기화나 캡슐화로 인한 문제가 발생할 가능성이 적기 때문에 굳이 가변성을 제한하지 않아도 된다.
* 따라서 지역 처리(local processing)에서는 가변 컬렉션을 사용하는 것이 더 합리적이다. 표준 라이브러리는 성능을 높이기 위해 모든 컬렉션 처리 함수의 내부에서 가변 컬렉션을 사용해 구현했다.

```kotlin
package kotlin.collections

public interface Iterable<out T> {
    public operator fun iterator(): Iterator<T>
}

public interface MutableIterable<out T> : Iterable<T> {
    override fun iterator(): MutableIterator<T>
}

public interface Collection<out E> : Iterable<E> { ... }
```
