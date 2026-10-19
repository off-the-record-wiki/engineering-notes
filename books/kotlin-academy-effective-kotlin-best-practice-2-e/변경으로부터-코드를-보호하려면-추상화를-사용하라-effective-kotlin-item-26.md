# (Effective Kotlin Item 26) Kotlin - 변경으로부터 코드를 보호하려면 추상화를 사용하라

#### 변경으로부터 코드를 보호하려면 추상화를 사용하라

* 함수나 클래스를 추상화하여 실제 코드를 숨기면 세부 정보로부터 사용자를 보호할 수 있을 뿐만 아니라 나중에 사용자가 알아채지도 못하게 이 코드를 변경할 수 있는 자유가 생긴다.

#### 추상화 대상 1 - 상수

* 리터럴 상수 값은 의미를 알 수 없는 경우가 많으며, 코드에서 반복될 때 특히 문제가 된다.
* 이러한 값을 상수 프로퍼티로 이동시키면 의미있는 이름이 할당될 뿐만 아니라 이 값들을 변경해야 할 떼 상수 값을 더 잘 관리할 수 있다.

```kotlin
// ❌ Bad - 숫자 7이 문맥에 따라 이해가 가능하지만 상수로 추출하면 더 이해하기가 쉽다.
fun isPasswordValid(text: String): Boolean {
    if (text.length < 7) {
        return false
    }
    return true
}
```
```kotlin
// ⭕ Good - 최소 비밀번호 길이를 더 쉽게 수정할 수 있다. 유효성 검사 로직을 이해할 필요 없이 상수에 할당된 값만 변경하면 된다.
private const val MIN_PASSWORD_LENGTH = 7

fun isPasswordValid(text: String): Boolean {
    return text.length >= MIN_PASSWORD_LENGTH
}
```

* 이와 같이 값이 추출되면 필요할 때마다 쉽게 변경할 수 있다.
* 상수로 추출하면 다음과 같은 이점이 있다는 것을 기억하면 좋을 것 같다.
  * 이름을 갖게 된다.
  * 이후에 값을 변경할 때 도움이 된다.

#### 추상화 대상 2 - 함수

```kotlin
// ❌ Bad - 모든 호출 지점을 찾아 고쳐야 한다.
// 여기저기서 이 긴 호출이 그대로 반복된다
Toast.makeText(this, message, Toast.LENGTH_LONG).show()

// ... 다른 화면에서도
Toast.makeText(this, message, Toast.LENGTH_LONG).show()

// ... 또 다른 곳에서도
Toast.makeText(this, message, Toast.LENGTH_LONG).show()
```
```kotlin
// ⭕ Good - 반복되는 호출을 확장 함수 하나로 묶는다.
fun Context.toast(
    message: String,
    duration: Int = Toast.LENGTH_LONG
) {
    Toast.makeText(this, message, duration).show()
}
```

* 그러나 만약 사용자에게 토스트를 보여주는게 아니라 스낵바로 바꾼다면 이름 자체가 구현을 드러내기 때문에 이름이 더는 맞지 않게 된다.
* 이 부분이 핵심이다. 한 단계 더 나아가 **메시지를 보여준다**는 의도로 추상화를 한다.

```kotlin
// ⭕ Good - 구현(toast/snackbar)이 아니라 '의도(메시지를 보여준다)'로 이름 짓는다.
fun Context.showMessage(
    message: String,
    duration: MessageLength = MessageLength.LONG
) {
    val toastDuration = when (duration) {
        MessageLength.SHORT -> Toast.LENGTH_SHORT
        MessageLength.LONG -> Toast.LENGTH_LONG
    }
    Toast.makeText(this, message, toastDuration).show()
}
```
* 그러나 함수는 매우 간단한 추상화지만 매우 제한적이기도 하다.
* 함수는 상태를 유지하지 않는다. 함수 시그니처를 변경하면 모든 호출부에 영향을 미치기 때문에 구현을 추상화하는 더 강력한 방법은 클래스를 사용하는 것이다.

#### 추상화 대상 3 - 클래스

```kotlin
// ⭕ Good - 메시지 표시를 '클래스'로 묶는다 — 의존성(Context)도 상태도 품을 수 있다
class MessageDisplay(private val context: Context) {

    fun show(
        message: String,
        duration: MessageLength = MessageLength.LONG
    ) {
        val toastDuration = when (duration) {
            MessageLength.SHORT -> Toast.LENGTH_SHORT
            MessageLength.LONG -> Toast.LENGTH_LONG
        }
        Toast.makeText(context, message, toastDuration).show()
    }
}
```
* 클래스 추상화의 진가는 인터페이스로 한 겹 감쌀 때 나온다. 사용하는 쪽이 구체 클래스가 아니라 인터페이스에만 의존하게 되면 구현을 통째로 갈아끼워도 사용하는 코드는 전혀 모른다.
```kotlin
// ⭕ Good - '무엇을 하는가'만 정의한 인터페이스 — 어떻게 보여주는지는 숨겨짐
interface MessageDisplay {
    fun show(message: String, duration: MessageLength = MessageLength.LONG)
}
```
```kotlin
class ToastDisplay(private val context: Context) : MessageDisplay {
    override fun show(message: String, duration: MessageLength) {
        val toastDuration = when (duration) {
            MessageLength.SHORT -> Toast.LENGTH_SHORT
            MessageLength.LONG -> Toast.LENGTH_LONG
        }
        Toast.makeText(context, message, toastDuration).show()
    }
}
```

```kotlin
// 구체 구현이 아니라 인터페이스에 의존한다
class SomeViewModel(private val messageDisplay: MessageDisplay) {
    fun onSaved() {
        messageDisplay.show("저장되었습니다")
    }
}
```

#### 추상화 대상 4 - 인터페이스

```kotlin
// ❌ Bad - 사용하는 쪽이 구체 클래스 ToastDisplay에 직접 묶여 있다
class ToastDisplay(private val context: Context) {
    fun show(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
```
```kotlin
class SomeViewModel(
    private val display: ToastDisplay   // ❌ 구체 타입에 의존
) {
    fun onSaved() {
        display.show("저장되었습니다")
    }
}
```
* `SomeViewModel`이 `ToastDisplay`라는 구체 구현을 알고 있다.
* 이걸 만약 스낵바로 바꾸려면 `SomeViewModel`의 타입 선언부터 고쳐야 하고, 테스트할 때 진짜 `ToastDisplay`를 끌고 와야 한다.
* 구현이 사용처로 새어 나온 것이다.

```kotlin
// ⭕ Good - '계약'만 정의 — 어떻게 보여주는지는 전혀 노출하지 않는다
interface MessageDisplay {
    fun show(message: String, duration: MessageLength = MessageLength.LONG)
}
```
```kotlin
class ToastDisplay(private val context: Context) : MessageDisplay {
    override fun show(message: String, duration: MessageLength) {
        val length = when (duration) {
            MessageLength.SHORT -> Toast.LENGTH_SHORT
            MessageLength.LONG -> Toast.LENGTH_LONG
        }
        Toast.makeText(context, message, length).show()
    }
}
```
```kotlin
class SnackbarDisplay(private val view: View) : MessageDisplay {
    override fun show(message: String, duration: MessageLength) {
        val length = when (duration) {
            MessageLength.SHORT -> Snackbar.LENGTH_SHORT
            MessageLength.LONG -> Snackbar.LENGTH_LONG
        }
        Snackbar.make(view, message, length).show()
    }
}
```

#### Next ID

```kotlin
var nextId : Int = 0

val newId = nextId++
```
* 프로젝트에 고유 ID가 필요해서 위와 같이 작성한 경우 코드 전체에 퍼져 있다면 주의해야 한다.
* 해당 방법은 다음과 같은 이유로 권장하지 않는다.
  * 프로그램을 처음 시작할 때마다 0부터 시작한다.
  * 쓰레드 안전하지 않다.

#### 추상화의 문제(트레이드 오프)⚠️

* 새로운 추상화를 추가하려면 코드를 읽는 개발자가 특정 개념에 대해 익숙하거나 배워야 한다.
* 또 다른 추상화를 정의하면 프로젝트에서 이해해야하는 항목이 추가되는 것이다.
* 추상화의 가시성을 제한하거나 구체적인 작업에만 사용되는 추상화를 정의할 땐 문제가 적다.
* 추상화를 정의하면 이런 비용이 발생한다는 점을 이해해야 한다. **따라서 기본적으로 모든 것을 추상화해선 안 된다.**
* 추상화는 많은 것을 숨길 수 있다. 그러나 한편으로는 생각할 것이 적을수록 개발하기가 더 쉽지만 한편으로는 너무 많은 추상화를 사용하면 행동의 결과를 이해하기가 더 어려워진다.
