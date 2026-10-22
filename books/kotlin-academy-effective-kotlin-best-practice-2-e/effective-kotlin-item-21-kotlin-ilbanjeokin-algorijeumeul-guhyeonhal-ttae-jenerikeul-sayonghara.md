# (Effective Kotlin Item 21) Kotlin - 일반적인 알고리즘을 구현할 때 제네릭을 사용하라

#### 일반적인 알고리즘을 구현할 때 제네릭을 사용하라

* 함수에 값을 인수로 전달할 수 있는 것처럼 타입 인수를 사용하면 타입을 전달할 수 있다.
* 타입 인수를 받는 함수를 제네릭 함수라고 부른다.

```kotlin
/**
 * Returns a list containing only elements matching the given [predicate].
 * 
 * @sample samples.collections.Collections.Filtering.filter
 */
public inline fun <T> Array<out T>.filter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList<T>(), predicate)
}
```
```kotlin
/**
 * Returns a list containing only elements matching the given [predicate].
 * 
 * @sample samples.collections.Collections.Filtering.filter
 */
public inline fun <T> Array<out T>.filter(predicate: (T) -> Boolean): List<T> {
    return filterTo(ArrayList<T>(), predicate)
}
```
* 타입 매개변수는 컴파일러에 유용하다. 컴파일러가 타입을 확인하고 타입을 좀 더 정확하게 추론할 수 있는 프로그램은 좀 더 안전해지고 개발자는 즐겁게 프로그래밍을 할 수 있다.
* 람다 표현식을 사용하여 `filter`를 호출하는 경우가 대표적인 사례이다.
* 컴파일러는 해당 인수가 컬렉션 요소의 타입과 동일한 타입이라는 것을 알기 때문에 우리가 타입을 잘못 사용하는 것을 막아준다.

#### 제네릭 제약

* 타입 매개변수의 가장 중요한 기능 중 하나는 구체 타입의 하위 타입으로 타입을 제한하는 것이다.
* 콜론(:) 뒤에 상위 타입을 두어 제한을 둘 수 있다. 이 타입은 이전에 정의한 타입 매개변수를 제약 조건으로 사용할 수 있다.
* 타입 매개변수에 상위 타입을 지정하면 해당 타입을 사용하는 인스턴스는 상위 타입이 제공하는 모든 메서드를 사용할 수 있다. `T`를 `Iterable<Int>`의 하위 타입으로 제한하면 `T` 타입의 인스턴스는 순회할 수 있으며 이 반복자에 의해 반환되는 요소가 `Int` 타입이라는 것을 알 수 있다.
* 또한 `Comparable<T>`로 제한하면 이 타입은 비교 가능한 타입임을 알 수 있다.
* 제약 조건으로 `Any`도 많이 사용하며, 이는 널 가능 타입이 아닌 모든 타입일 수 있음을 의미한다.

```kotlin
/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element in the original array.
 * 
 * @sample samples.collections.Collections.Transformations.mapNotNull
 */
public inline fun <T, R : Any> Array<out T>.mapNotNull(transform: (T) -> R?): List<R> {
    return mapNotNullTo(ArrayList<R>(), transform)
}
```
