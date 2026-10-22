# (Effective Kotlin Item 38) Kotlin - 연산과 행동을 전달하려면 함수 타입이나 함수형 인터페이스를 사용하라

#### 연산과 행동을 전달하려면 함수 타입이나 함수형 인터페이스를 사용하라

* 단일 메서드를 가지는 인터페이스를 단일 추상 메서드(Single-Abstract Method, SAM)라고 한다.
* 코틀린은 동작을 나타내는 함수로 함수 타입 혹은 함수형 인터페이스를 제공한다.

```kotlin
// 함수 타입 정의
val add: (Int, Int) -> Int = { a, b -> a + b }
val isEven: (Int) -> Boolean = { it % 2 == 0 }

// 함수형 인터페이스 정의
fun interface Validator {
    fun validate(value: String): Boolean
}
```

#### 타입 별칭과 함께 함수 타입 사용하기

```kotlin
// 구체적인 함수 타입의 이름을 지정하고 싶다면 타입 별칭(type alias)을 사용
typealias Handler = (String, Int) -> Boolean
```

#### 함수형 인터페이스를 사용하는 이유

* 함수형 인터페이스는 함수 타입보다 복잡한 방법이다.
* 함수형 인터페이스는 따로 정의해야 한다는 단점이 있지만 다음과 같은 장점들이 있다.
  * 명명된 타입을 새롭게 정의한다.
  * 핸들러 함수의 이름은 다르게 지정될 수 있다.
  * 다른 언어와의 상호 운용성이 좋다.

```kotlin
// fun interface로 타입을 분리하면 의미가 다른 함수는 못 섞임
fun interface AgeChecker { fun check(age: Int): Boolean }
```

#### 여러 추상 메서드가 포함된 인터페이스를 사용하여 행동을 표현하지 말라

```kotlin
// ⭕ Good - 각 '행동'을 추상 메서드 하나짜리 fun interface로 분리
fun interface Validator {
    fun validate(data: String): Boolean
}

fun interface Transformer {
    fun transform(data: String): String
}

fun interface Saver {
    fun save(data: String)
}
```
