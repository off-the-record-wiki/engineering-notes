# (Effective Kotlin Item 52) Kotlin - 인라인 값 클래스 사용을 고려하라

#### 인라인 값 클래스 사용을 고려하라

* 함수만 인라인화 될 수 있는 것이 아니다. 단일 값을 보유하는 객체도 인라인화될 수 있다.
* 클래스를 인라인화하려면 읽기 전용 기본 생성자 프로퍼티, `value` 한정자 및 `JvmInline` 어노테이션이 있어야 한다.
```kotlin
@JvmInline
value class Name(private val value: String) {
    // ...
}
```
* 인라인 값 클래스는 가능하다면 보유한 값으로 대체된다.
* 인라인 값 클래스의 메서드는 정적 메서드가 된다.
```kotlin
@JvmInline
value class Name(private val value: String) {
    // ...

    fun greet() {
        print("Hello, I am $value)
    }
}
```
* 인라인 값 클래스를 사용하면 성능 오버헤드 없이 타입 래퍼를 만들 수 있다.
* 인라인 값 클래스의 일반적인 3가지 용도는 다음과 같다.
  * 측정 단위를 나타낸다.
  * 값을 다른 목적으로 사용하는 것을 방지한다.
  * 메모리 사용을 최적화한다.

#### 측정 단위를 표시하라

* 타이머를 설정하기 위해 메서드를 사용한다고 가정하자.
```kotlin
// ❌ Bad
interface Timer {
    fun callAfter(time: Int, callback: () -> Unit)
}
```
* 여기서 `time`은 밀리초, 초 또는 분 단위의 시간일 수 있다. 단위가 불분명하기 때문에 실수하기 쉬우며 심각한 결과를 초래할 수 있다.
* 그래서 매개변수 이름에 단위를 명시하는 방법이 사용된다.
```kotlin
// ⚠️ Caution
interface Timer {
    fun callAfter(timeMillis: Int, callback: () -> Unit)
}
```
* 이 방법도 좋지만 실수할 여지가 아직까지 남아 있다. 예를 들어, 함수를 호출할 때 프로퍼티 이름이 표시되지 않는 경우가 많다.
* 반환 타입에서 시간 단위를 표시하기 쉽지 않다는 것도 문제가 된다. 단위가 무엇인지 모르기 때문에 시간을 올바르게 설정하지 못할 가능성이 크다.
```kotlin
// ⭕ Good
interface User {
    fun decideAboutTime(): Int
    fun wakeUp()
}

interface Timer {
    fun callAfter(timeMillis: Int, callback: () -> Unit)
}

fun setUpUserWakeUpUser(user: User, timer: Timer) {
    val time: Int = user.decideAboutTime()
    timer.callAfter(time) {
        user.wakeUp()
    }
}
```
* 함수 이름을 `decideAboutTimeMillis()`로 지정하면 반환할 시간의 단위를 표시할 수 있다.
* 하지만 필요하지 않을 때도 세부 정보를 알려주게 되므로 그다지 좋은 방법은 아니다.
* 이 때, 더 좋은 방법은 인라인 값 클래스를 사용하여 타입을 잘못 사용하지 않도록 엄격한 타입을 도입하는 것이다.

```kotlin
// ⭕ Good
@JvmInline
value class Minutes(val minutes: Int) {
    fun toMillis(): Millis = Millis(minutes * 60 * 1000)
    // ...
}

@JvmInline
value class Millis(val milliseconds: Int) {
    // ...
}

interface User {
    fun decideAboutTime(): Minutes
    fun wakeUp()
}

interface Timer {
    fun callAfter(timeMillis: Millis, callback: () -> Unit)
}
```
* 인라인 값 클래스가 있으면 자연스럽게 올바른 타입을 사용하게 된다.
* 인라인 값 클래스는 측정 기준 단위를 표현할 때 특히 유용하다. 예를 들어, 프론트엔드에서는 픽셀 밀리미터, dp 등과 같은 다양한 단위를 사용하는 경우가 많은데 DSL과 비슷한 방법으로 확장 프로퍼티를 정의하여 객체를 만들 수 있다.

#### 값 오용으로부터 보호하라

* 대규모 프로젝트에서 원시형을 잘못 사용하는 경우를 방지하기 위해 래퍼 클래스를 주로 사용한다.
* 따라서 다양한 종류의 ID를 각각 래퍼로 정의해야 하며, 성능 오버헤드를 피하기 위해 인라인 값 클래스로 만들어야 한다.
```kotlin
@JvmInline
value class StudentId(val value: String)

@JvmInline
value class ClassId(val value: String)

data class Student(val id: StudentId, val classId: ClassId)
```

#### 메모리 사용을 최적화하라

* 래핑된 타입 대신 원시형을 사용하는 것은 최적화의 한 방법이다. 하지만 최적화를 위해 원시형으로 바꾸게 되면 작업하기가 어려워진다.
* 인라인 값 클래스를 사용해 원시형을 래핑하면 해결할 수 있다.
```kotlin
@JvmInline
value class OptionalDouble(val value: Double) {
    fun isUndefined() = value.isNan()

    companion object {
        const val UNDEFINED_VALUE = Double.NaN
        val Undefined = OptionalDouble(UNDEFINED_VALUE)
    }
}
```

#### 인라인 값 클래스와 인터페이스

* 인라인 값 클래스도 인터페이스를 구현할 수 있다. 인라인 값 클래스가 인터페이스를 구현하면 타입 변환을 방지할 수 있다.
```kotlin
interface TimeUnit {
    val millis: Long
}

@JvmInline
value class Minutes(val minutes: Long) : TimeUnit {
    override val millis: Long get() = minutes * 60 * 1000
    // ...

@JvmInline
values class Millis(val milliseconds: Long) : TimeUnit {
    override val millis: Long get() = milliseconds
}

fun setUpTimer(time: TimeUnit) {
    val millis = time.millis
    // ...
}

setUpTimer(Minutes(123))
setUpTimer(Millis(456789))
```
* 하지만 객체가 인터페이스로 사용되면 인라인되지 않는다. 인터페이스 타입을 알리기 위해 인라인 값 클래스가 객체로 만들어져야 하므로 인라인으로 지정한 효과가 없어지게 된다.
* 즉, 인터페이스로 제공된 인라인 값 클래스는 인라인화되지 않는다.
* 널 가능 타입인 매개변수에 인라인 값 클래스가 인수로 들어오는 경우 또한 인라인화되지 않는다.

#### 타입 별칭

* 코틀린의 타입 별칭을 사용하면 타입에 대한 다른 이름을 만들 수 있다.

```kotlin
typealias NewName = Int
val n : NewName = 10
```
* 타입에 별칭을 붙이면 긴 이름을 가지며 반복적으로 사용되는 타입을 간단하게 처리할 수 있다. 자주 사용되는 함수 타입의 별칭을 지정하는 것이 좋다.
