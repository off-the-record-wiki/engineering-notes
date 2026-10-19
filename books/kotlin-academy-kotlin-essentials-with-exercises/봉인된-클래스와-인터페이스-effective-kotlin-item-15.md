# (Effective Kotlin Item 15) Kotlin - Sealed Classes and Interfaces

#### 봉인된 클래스와 인터페이스

* 서브 클래스 전부를 미리 알 수 없는 계층구조를 '제한 없는 계층구조'라고 한다.
* 제한된 계층구조를 정의하고 싶다면 클래스나 인터페이스 앞에 `sealed` 제어자를 붙인다.

```kotlin
sealed interface Result
class Success(val data: String) : Result
class Failure(val exception: Throwable) : Result
```
```kotlin
sealed class Result
class Success(val data: String) : Result()
class Failure(val exception: Throwable) : Result()
```
* 봉인된 클래스나 봉인된 인터페이스의 서브 타입들은 다음 사항들을 만족해야 한다.

1. 봉인된 클래스나 봉인된 인터페이스와 같은 패키지와 모듈에서 정의되어야 한다.
2. 지역적이 될 수 없고 객체 표현식을 사용해 정의할 수 없다.

#### 봉인된 클래스와 when 표현식

* 타입의 개수가 정해져 있기 때문에 모두를 브랜치로 지정하면 철저한 `when` 표현식을 작성할 수 있다.
* 예를 들어, 봉인된 클래스나 봉인된 인터페이스를 사용하고 모든 서브타입을 `is`로 확인할 수 있다.
```kotlin
sealed class Response<out V>
class Success<V>(val value: V) : Response<V>()
class Failure(val error: Throwable) : Response<Nothing>()

fun handle(response: Response<String>) {
    val text = when (response) {
        is Success -> "Success with ${response.value}"
        is Failure -> "Error"
    }
    print(text)
}
```

#### 봉인된 타입 대 열거형

* 열거형 클래스는 '값의 집합'을 표현할 때 사용한다.
* 봉인된 클래스와 봉인된 인터페이스 클래스나 객체 선언으로 만들 수 있는 '서브타입의 집합'을 표현한다.
* 인스턴스를 여러 개 만들 수 있고 데이터를 담을 수 있다.
* 서브 클래스 수가 고정되어 있음을 표현하고 싶다면 봉인된 클래스를 사용한다.

```kotlin
sealed class MathOperation
class Plus(val left: Int, val right: Int) : MathOperation()
class Minus(val left: Int, val right: Int) : MathOperation()
class Times(val left: Int, val right: Int) : MathOperation()
class Divide(val left: Int, val right: Int) : MathOperation()

sealed interface Tree
class Leaf(val value: Any?) : Tree
class Node(val left: Tree, val right: Tree) : Tree
```
