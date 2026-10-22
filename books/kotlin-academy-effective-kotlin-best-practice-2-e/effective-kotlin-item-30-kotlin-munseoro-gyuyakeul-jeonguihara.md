# (Effective Kotlin Item 30) Kotlin - 문서로 규약을 정의하라

#### 문서로 규약을 정의하라

```kotlin
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
* 메시지 표시 방법을 자유롭게 변경할 수 있도록 `showMessage()` 함수를 추출했으나 이 기능을 문서화하지는 않았다.
* 범용적으로 사용하기 위해 함수 이름을 `showMessage()`로 지정하고 메시지 타입 또한 구체적인 타입 대신 문자열로 받고 있지만 사용자들은 다른 의미로 받아들일 수 있다.
* 코드뿐 아니라 KDoc 주석을 추가하면 함수의 기능과 사용법을 상세하게 설명할 수 있다.

```kotlin
/**
 * 사용자에게 짧은 메시지를 화면에 표시한다.
 *
 * 현재는 [Toast]로 구현되어 있으나, 호출하는 쪽은 표시 방식에 의존하지 않는다.
 * 따라서 추후 구현을 스낵바 등으로 교체해도 호출부는 영향을 받지 않는다.
 *
 * @param message 표시할 메시지 내용
 * @param duration 메시지 표시 시간. 기본값은 [MessageLength.LONG]
 */
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

#### 규약

* 추상화된 객체에 설명을 추가하면 사용자는 이를 규약으로 생각하고 어떤 방식으로 동작할 것인지 추측한다.
* 그러한 예상되는 모든 동작을 요소 규약이라고 한다.
* 규약이 잘 정의되면 작성자는 클래스 사용 방식에 대해 걱정할 필요가 없으며, 사용자는 내부적으로 구현된 방식에 대해 신경 쓸 필요가 없다.
* 사용자는 실제 구현을 알지 못해도 규약에 의존할 수 있다.

#### 규약의 정의

* 이름 : 이름에 일반적인 개념이 포함되어 있다면 이름만 보고 어떻게 동작할 것인지 쉽게 알 수 있다. 예를 들어, `sum()` 메서드를 볼 때 어떻게 동작하는지 알기 위해 주석을 읽을 필요가 없다.
* 주석과 문서 : 필요한 모든 것을 설명할 수 있는 가장 강력한 방법이다.
* 타입 : 타입은 객체에 대해 많은 것을 알려준다. 각 타입은 잘 정의된 메서드들을 정의하며, 일부 타입은 문서에 그 책임이 정해져 있다. 함수를 볼 때 반환 타입과 인수 타입에 대한 정보는 매우 중요하다.

#### 주석이 필요한지?

* 가독성 있는 코드를 작성하는 데 집중해야 한다는 것에는 전적으로 동의한다. 하지만 요소에 주석을 추가하면 해당 요소를 더 잘 설명할 수 있으며, 규약도 정의할 수 있다는 점을 인지해야 한다.
* 주석은 유용하고 중요한 경우도 많다.

#### KDoc 포맷

* 주석을 사용하여 함수를 문서화할 때 주석을 표기하는 공식적인 형식을 KDoc이라고 한다.
* 모든 KDoc 주석은 `/**`로 시작하고 `*/`로 끝나며, 주석 내 모든 줄은 일반적으로 `*`로 시작한다.
* KDoc 주석의 구조는 다음과 같다.
  * 문서 텍스트의 첫 번째 단락은 요소에 대한 요약이다.
  * 두 번째 단락은 요약을 자세하게 설명한다.
  * 이후에 각각의 줄은 태그로 시작된다. 태그는 구성 요소를 지정하여 어떤 역할을 하는지 설명한다.

| 태그 | 용도 | 비고 |
|---|---|---|
| `@param name` | 파라미터 설명 | 타입 파라미터도 `@param T` 형태로 가능 |
| `@return` | 반환값 설명 | `Unit` 반환이면 생략 |
| `@throws Class` | 던지는 예외 설명 | `@exception`과 동일 |
| `@exception Class` | `@throws`의 별칭 | |
| `@receiver` | 확장 함수의 리시버 설명 | |
| `@constructor` | 클래스의 주 생성자 설명 | 클래스 KDoc에 사용 |
| `@property name` | 주 생성자에 선언된 프로퍼티 설명 | 클래스 KDoc에 사용 |
| `@sample fqName` | 예시 코드를 함수 참조로 삽입 | 정규화된 함수명 지정 |
| `@see ref` | 관련 요소 참조(링크) | |
| `@author` | 작성자 | |
| `@since` | 도입된 버전 | |
| `@suppress` | 생성 문서에서 제외 | 공개 API지만 문서엔 숨길 때 |

#### 타입 시스템과 기대치

* 타입 계층은 객체를 나타내는 중요한 정보이다.
* 공개 함수의 규약은 반드시 적절한 설명이 필요하다.

```kotlin
// ❌ Bad - 반환 타입이 String이지만 실제로는 null 같은 의미의 빈 값을 섞어 쓴다
// 호출부에서는 "있다"고 기대하지만 실제론 빈 문자열이 올 수도 있다.
fun findUser(id: Long): String? {
    return repository.get(id)?.name
}
```
```kotlin
// ⭕ Good - "없을 수 있다"를 타입(String?)으로 정직하게 드러낸다
fun findUser(id: Long): String? {
    return repository.get(id)?.name
}
```

#### 구현 누출

* 구현 세부사항은 항상 누출된다.
* 프로그래밍 안에서 구현 세부사항이 누출되는데 예를 들어, 리플렉션을 사용하여 함수를 호출하면 일반 함수를 사용하는 것보다 성능이 훨씬 떨어진다.
* 추상화를 할 때도 구현 사항이 누출될 수 있으나 최대한 숨기는 것이 좋다. 캡슐화를 통해 이를 보호하는데, 이 때 캡슐화는 '내가 허용하는 것만 할 수 있고 그 이상은 할 수 없다'라고 설명할 수 있다.
