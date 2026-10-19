# (Effective Kotlin Item 16) Kotlin - Unit?을 반환이나 연산에 사용하지 말라

#### Unit?을 반환이나 연산에 사용하지 말라

* `Unit?`은 `Boolean`이 `true` 또는 `false`를 갖는 것처럼 `Unit` 또는 `null`이라는 값만 가질 수 있다.
```kotlin
// ❌ Bad
fun verifyKey(key: String): Unit? =
    if (key.isCorrect()) Unit else null
```
```kotlin
// ⭕ Good
fun isKeyCorrect(key: String): Boolean =
    key.isCorrect()
```
* `Unit?`을 사용해 논리값을 표현하는 것은 오해의 소지가 있으며 감지하기 어려운 코드로 이어질 수 있다.
* `Unit?`이 사용된 코드는 오해의 소지가 있고 혼동될 수 있으므로 `Boolean`을 사용하는 형태로 변경하는 것이 좋다.
