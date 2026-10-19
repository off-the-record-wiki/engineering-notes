# (Effective Kotlin Item 39) Kotlin - 제한된 계층구조를 표현하기 위해 sealed 클래스와 sealed 인터페이스를 사용하라

#### 제한된 계층구조를 표현하기 위해 sealed 클래스와 sealed 인터페이스를 사용하라

* `sealed` 클래스와 `sealed` 인터페이스는 제한된 게층구조를 나타낸다.
* 제한된 계층구조는 미리 정해진 일련의 구체적인 클래스로 구성된 계층구조를 의미한다.

```kotlin
sealed interface Payment {
    data class Card(val number: String) : Payment
    data class Cash(val amount: Int) : Payment
    object Free : Payment
}
```
```kotlin
// 미래에 변경될 수 있지만, 현재에는 final로 간주되는 계층구조를 표현할 때 사용
sealed class Result {
    data class Success(val data: String) : Result()
    data class Error(val message: String) : Result()
    object Loading : Result()
}
```
* 즉, `sealed` 한정자를 사용하면 서브클래스의 종류를 제한할 수 있다.
* `sealed` 클래스가 라이브러리나 모듈 안에 정의되어 있다면 그 라이브러리나 모듈을 사용하는 클라이언트는 `sealed` 클래스의 서브 클래스를 추가할 수 없다.
* 어느 누구도 로컬 클래스 또는 객체 표현식으로 `sealed` 클래스를 상속할 수 없다. 서브 클래스가 제한된 계층구조를 가지게 되는 것이다.

#### sealed 클래스와 when 표현식

* `when` 표현식을 사용하면 모든 분기에 대해 값을 반환해야 한다. 대부분의 경우 모든 경우를 처리하기 위해 `else`절을 지정할 수밖에 없다.
* 하지만 `when` 구문에서 타입이 제한된 값을 인수로 받으면 `else`없이 가능한 모든 값을 분기로 처리할 수 있다.
* `sealed` 클래스의 가능한 하위 타입을 `is`로 검사하는 것이 대표적인 예시이다.
* `sealed` 클래스를 사용하는 `when` 표현식에 `else`가 없을 때, `sealed` 클래스의 서브 클래스가 추가되었다면 `when` 표현식을 사용하는 모든 곳에 새로운 타입을 추가해야 한다.
* `when` 표현식에서 새로운 클래스를 추가하도록 강제하여 모든 값에 대한 분기를 갖추게 되므로, 프로젝트 내에서는 아주 편리하다. 하지만 라이브러리나 공유 모듈의 공개 API에 `sealed` 클래스가 포함되어 있을 때, 하위 타입을 추가하면 하위 버전 호환이 불가능해진다.
