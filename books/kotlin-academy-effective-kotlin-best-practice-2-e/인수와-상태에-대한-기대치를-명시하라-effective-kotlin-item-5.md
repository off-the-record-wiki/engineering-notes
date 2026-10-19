# (Effective Kotlin Item 5) Kotlin - 인수와 상태에 대한 기대치를 명시하라

#### 인수와 상태에 대한 기대치를 명시하라

* `require` 블록 : 인수에 대한 기대치를 명시하는 범용적인 방법
* `check` 블록 : 상태에 대한 기대치를 명시하는 범용적인 방법
* `error` 함수 : 애플리케이션이 예기치 못한 상태에 도달했음을 알리는 범용적인 방법
* `return` 또는 `throw`와 함께 사용하는 엘비스 연산자

> 기대치(Expectation) : 입력값에 대한 요구사항

```kotlin
// ⭕ Good
fun pop(num: Int = 1): List<T> {
    require(num <= size) {
        "Cannot remove more elements than current size"
    }

    // ...
}
```

* 이런 방식으로 기대치를 명시한다고 해서 문서에 작성하지 않아도 되는 것은 아니지만 이는 매우 유용하다.
* 이러한 선언적 검사는 여러 이점을 가지고 있다.
  * 문서를 읽지 않은 개발자도 기대치를 알 수 있다.
  * 기대치가 충족되지 않으면 함수는 예상치 못한 동작으로 이어지는 대신 예외를 던진다.
  * 코드가 어느 정도 자체적으로 검사된다. 해당 조건이 코드에서 확인되기 때문에 단위 테스트의 필요성이 줄어든다.

#### 인수(Argument)

* 코틀린에서 기대치를 명시하는 가장 일반적이고 직접적인 방법은 `require` 함수를 사용하는 것이다.
* `require` 함수는 해당 요구사항을 확인하고 충족되지 않는 경우 `IllgalArgumentException`을 던진다.

```kotlin
// ⭕ Good
data class User(
    val name: String,
    val email: String
) {
    init {
        require(name.isEmpty())
        require(isValidEmail(email))
    }
}
```

* `require` 함수는 인수에 대한 기대치를 명시할 때 사용할 수 있다.
* 이와 비슷하게, 현재 상태에 대한 기대치를 명시해야 할 때가 있다.
* 이 경우 `check` 함수를 사용할 수 있다.
* `check` 함수는 기대치가 충족되지 않을 때, `IllegalStateException`을 던진다.

> **인수에 대한 기대치 : 프로그래밍에서 이 함수가 제대로 작동하기 위해 호출자가 반드시 지켜야 하는 입력값의 규칙을 말한다.**
> * 범위(Range)에 대한 기대치 : 숫자형 인수가 특정 범위 내에 있어야 할 때 사용한다.
> * 상태(State)에 대한 기대치 : 함수가 실행되기 위해 객체나 시스템이 특정 상태여야 함을 요구한다.
> * 포맷(Format) 또는 논리(Logic)에 대한 기대치 : 입력값이 특정 형태를 갖추거나 논리적인 앞뒤 순서가 맞아야 할 때 사용한다.

#### 상태(State)

* `check` 함수는 `require` 함수와 비슷하게 동작하지만 명시된 기대치가 충족되지 않았을 때 `IllegalStateException`을 던진다.
* `check` 함수는 상태가 올바른가를 확인한다.
* 예외 메시지는 `require` 함수와 마찬가지로 지연 메시지를 사용하여 사용자화할 수 있다.
* 기대치가 함수 전체에 대한 것인 경우에는 함수의 시작 부분에, 일반적으로는 `require` 블록 다음에 배치한다.
* 그러나 상태에 대한 요구사항이 지역적으로 한정된다면 `check` 함수를 도입부가 아닌 곳에서 사용할 수 있다.
* 사용자가 규약을 어기고 호출하면 안 되는 상황에서 함수를 호출할 거라 의심될 경우 `check` 함수를 사용할 수 있다.
* 사용자가 그렇게 하지 않을 것이라고 믿는 것보다는 해당 사항을 확인하고 적절한 예외를 던지는 것이 좋다.

> **상태에 대한 기대치 : 내부 객체의 상태**

#### 널 가능성과 스마트 캐스팅

* 코틀린에서는 `require`와 `check` 함수가 반환되면 해당 함수에서 확인된 것은 이후에도 `true`라고 가정한다.
```kotlin
// ⭕ Good
public inline fun require(value: Boolean): Unit {
    contract {
        returns() implies value
    }
    require(value) { "Failed requirement." }
}
```
* 이 블록에서 검사된 모든 것은 나중에 동일한 함수 내에서 `true`로 간주한다.
* 무언가가 `true`로 확인되면 컴파일러에서 그것을 확실한 것으로 취급하므로 스마트 캐스팅이 잘 작동한다.

#### 널 아님 단언 !!의 문제들

* `requireNotNull`과 `checkNotNull` 대신 널 아님 단언인 `!!` 연산자를 사용할 수 있다.
* `!!` 연산자를 사용하면 자바에서 널로 인한 문제가 그대로 발생한다.
* `null`이 아니라고 생각하고 사용했지만 실제로 `null`인 경우 NPE가 발생한다.
* 널 아님 단언 `!!`은 널 가능이지만 `null`이 오지 않을 것 같은 곳에서 자주 사용된다.
* 널 아님 단언 `!!`을 사용하거나 명시적으로 오류를 던지는 방식으로 사용한다면 언젠가는 오류가 발생할 수 있다는 것을 염두에 두어야 한다.
* **그러나 명시적인 오류는 일반적인 NPE보다 훨씬 더 많은 정보를 제공하므로 대부분의 경우 NPE보다 구체적인 오류를 던지는 것이 더 좋다.**
* 널 아님 단언 `!!`를 사용하기 적합한 경우에는 극히 드물며, 널 가능성을 올바르게 표기하지 않은 라이브러리를 사용하는 경우 외에는 거의 없다.
* 일반적으로 널 아님 단언 `!!` 사용은 피해야 한다.
* `!!`를 사용하지 않기 위해서는 무의미한 널 가능성을 피해야 한다.

```kotlin
// ❌ Bad
class UserProfile(val name: String?)

fun printUserName(user: UserProfile) {
    // 다른 개발자가 쓴 코드: 왜 널이 아닐 거라 확신하는지 알 수 없음
    println(user.name!!.uppercase()) 
}
```

#### 엘비스 연산자 사용하기

* 널 가능성을 위해 오른쪽에 `throw` 또는 `return`을 두고 엘비스 연산자를 사용하는 방식도 자주 쓰인다.
* 엘비스 연산자를 사용하면 가독성이 높아지며, 우리가 원하는 동작을 더 유연하게 구현할 수 있게 된다.
* 우선 오류를 던지는 대신에 엘비스 연산자와 `return`을 사용해서 함수 실행을 중지하는 경우를 들 수 있다.

```kotlin
// ⭕ Good
fun sendEmail(person: Person, text: String) {
    val email: String = person.email ?: run {
        log("Email not sent, no email address")
        return
    }
    // ...
}
```
* 변수의 널 가능성에 대한 처리를 지정할 때, `return` 또는 `throw`와 함께 엘비스 연산자를 쓰는 방법은 널리 쓰이는 관용적인 방법이므로 주저하지 말고 사용해야 한다.

#### error 함수

* 코틀린 표준 라이브러리에는 `IllegalStateException`을 던질 때 사용하는 `error` 함수가 있다.
* 이 함수는 기대하지 않았던 데이터 타입이 인수로 들어오는 것처럼 절대로 발생하지 않을거라고 예상했던 상황을 처리하는 데 자주 사용된다.

```kotlin
public inline fun error(message: Any): Nothing = throw IllegalStateException(message.toString())
```

* `error` 함수는 `Nothing`을 반환한다. 이 함수는 절대 정상적으로 종료되지 않고, 무조건 예외를 던져서 흐름을 끊는다는 것을 컴파일러에게 알려준다.
