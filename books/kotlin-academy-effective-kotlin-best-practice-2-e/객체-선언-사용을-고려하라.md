# (Effective Kotlin Item 48) Kotlin - 객체 선언 사용을 고려하라

#### 객체 선언 사용을 고려하라

* 인스턴스에 따라 상태가 달라지지 않는 클래스인 경우 이를 객체 선언(object declaration)으로 변환해 싱글톤으로 정의할 수 있다.
* 생성자를 따로 정의하지 않고 `class` 대신 `object` 키워드를 사용하면 된다. 싱글톤 객체를 참조할 때는 객체 선언의 이름을 사용한다.

```kotlin
object Singleton {
    fun doSomething() {
        // ...
    }
}
```
* 객체 선언은 생성되는 객체의 수를 제한하는 데 유용하다.
* 예를 들면, 이벤트나 마커처럼 자주 생성되는 클래스를 객체 선언으로 대체하는 것이 좋다.
* 객체 선언을 하면 인스턴스가 단 하나만 존재하게 됨을 보장할 수 있다.

```kotlin
// ❌ Bad
sealed interface StoreMessage<T>
data class Save<T>(val data: T): StoreMessage<T>
data class DeleteAll<T>: StoreMessage<T>
```
* 이럴 때는 공변 Nothing 객체 패턴을 사용하면 된다.
* 상위 타입 클래스의 제네릭 타입을 공변으로 설정해야 한다. 그런 다음, 객체 선언의 타입 인수로 `Nothing`을 사용해야 한다.

```kotlin
// ⭕ Good
sealed interface StoreMessage<out T>
data class Save<T>(val data: T): StoreMessage<T>
data object DeleteAll<T>: StoreMessage<Nothing>
```
* `Nothing`은 코틀린에서 모든 타입의 하위타입이고, `StoreMessage`의 `T`는 공변이므로 `StoreMessage<Nothing>`은 모든 `StoreMessage<T>` 타입의 하위타입이다.
* 따라서 `DeleteAll`은 `T`가 무엇이든지 상관없이 `StoreMessage<T>`의 하위타입이 된다.
