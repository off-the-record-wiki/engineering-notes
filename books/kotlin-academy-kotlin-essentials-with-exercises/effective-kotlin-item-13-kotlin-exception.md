# (Effective Kotlin Item 13) Kotlin ‐ Exception

#### 예외 던지기

* `throw` 키워드를 사용하면 직접 예외를 던질 수 있다.

```kotlin
private fun functionThrowing() {
    throw ArithmeticException("Some Message")
}

fun main() {
    println("Before")
    functionThrowing()
    println("After")
}
```

* 예외를 던진다는 것은 현재의 함수는 지금의 상황을 처리하지 못하거나 처리할 책임이 없다는 것을 전달한다.
* 달리 표현하면, 반드시 에러를 뜻하는 것이 아니다.
* 오히려 적절한 대응책을 알고 있는 다른 장소에서 처리하는 것에 가깝다고 볼 수 있다.

#### 예외 정의

* 커스텀 예외를 정의할 수도 있다.
* 예외는 `Throwable` 클래스를 확장하는 일반 클래스 혹은 객체 선언이다.
* `throw`를 사용하면 이러한 클래스를 던질 수 있다.

```kotlin
class MyException : Throwable("Some Message")
object MyExceptionObject : Throwable("Some Message")

private fun functionThrowing() {
    throw MyException()
    // throw MyExceptionObject
}
```
* 예외는 `try ~ catch` 구조로 잡을 수 있다.
* 예외가 `try` 블록 안에서 발생했다면 뒤따르는 `catch` 블록에서 잡아 처리할 수 있기 때문에 흐름이 바뀔 수 있다.
* `catch` 블록들은 선언된 순서대로 발생한 예외를 처리할 수 있는지 확인한다.

#### 표현식으로 사용되는 try-catch 블록

* `try ~ catch` 구조는 표현식으로도 사용할 수 있다.
* 예외가 발생하지 않으면 `try` 블록의 결과를 반환하고 예외가 발생한 뒤 잡으면 `catch` 블록의 결과를 반환한다.

```kotlin
fun main() {
    val a = try {
        1
    } catch (e: Error) {
        2
    }
    println(a)  // 1

    val b = try {
        throw Error()
        1
    } catch (e: Error) {
        2
    }
    println(b)  // 2 
}
```

> **try-catch문과 엘비스 연산자 조합**
>
> * 예외가 발생했을 때, 숫자를 반환하기보다 `null`을 반환하게 한 뒤 엘비스 연산자로 기본값을 세팅하거나 함수를 빠져나가는 패턴이 자주 쓰인다.
```kotlin
fun parseIdOrNull(input: String): Int? {
    return try {
        input.toInt()
    } catch (e: NumberFormatException) {
        null // 예외 발생 시 null 반환
    }
}

fun main() {
    val input = "invalid_number"
    
    // try-catch 결과와 엘비스 연산자 조합
    val id = parseIdOrNull(input) ?: throw IllegalArgumentException("올바른 형식이 아닙니다.")
}
```

#### finally 블록

* `try ~ catch` 구조에서 예외가 발생하더라도 항상 실행되어야 하는 동작을 명시하는 `finally` 블록을 사용할 수 있다.
* `finally` 블록 안에서 예외는 잡지 못하지만 예외를 처리하지 못하더라도 블록 안의 코드는 무조건 실행됨을 보장할 수 있다.
* `finally` 블록은 주로 연결을 끊거나 리소스를 해제할 때 사용한다.

> **리소스 해제 : finally 대신 use 확장 함수를 써라**
> 
> * `AutoCloseable`을 구현한 리소스를 다룰 때는 `try-finally`를 직접 작성하기보다 코틀린 표준 라이브러리의 `use` 확장 함수를 사용하는 것이 베스트 프렉티스이다.
> * `use`는 블록 내부 로직이 정상 종료되든, 예외가 발생하든 무조건 리소스를 안전하게 닫아줌을 보장하며 코드가 훨씬 깔끔해진다.
```kotlin
// ❌ 기존 자바 스타일 (try-finally 직접 구현)
val reader = BufferedReader(FileReader("test.txt"))
try {
    println(reader.readLine())
} FINALLY {
    reader.close() // 리소스 해제
}

// ⭕️ 코틀린다운 스타일 (use 확장 함수 활용)
BufferedReader(FileReader("test.txt")).use { reader ->
    println(reader.readLine()) // 블록을 벗어나면 자동으로 close() 호출됨
}
```

#### 중요한 예외 정리

* Kotlin에서는 특정 상황에서 사용되는 예외가 몇 가지 정의되어 있다.
  * `IllegalArgumentException` : 인수의 값이 잘못됐을 때 사용한다.
  * `IllegalStateException` : 시스템의 상태가 잘못됐을 때 사용한다.
* Kotlin에서는 `require()`와 `check()` 함수로 조건을 확인할 수 있으며, 조건이 만족되지 않으면 `IllegalArgumentException`과 `IllegalStateException`을 던진다.
> * **require(Argument 검증)** : 함수의 입력 매개변수가 유효한지 검증할 때 사용한다. 실패 시 `IllegalArgumentException`을 던진다.
> * **check(State 검증)** : 객체 내부의 현재 상태가 아니라 이 로직을 실행하기 적절한지 검증할 때 사용한다. 실패 시 `IllegalStateException`을 던진다.

#### 예외 계층 구조

* `Throwable`의 가장 중요한 서브 타입은 `Error`와 `Exception`이다.
  * `Error`
    * 회복이 불가능해서 잡을 수 없는, 그래서 `catch` 블록에서 잡더라도 다시 던질 수 밖에 없는 예외이다.
    * 회복이 불가능한 예외로는 JVM 힙 공간이 부족할 때 던지는 OOM이 있다.
  * `Exception`
    * `try ~ catch` 블록을 사용해 회복 가능한 예외이다. 
    * Exception에 속하는 예외로는 `IllegalArgumentException`, `IllegalStateException`, `ArithmeticException`, `NumberFormatException` 등이 있다.
* 커스텀 예외를 정의할 때는 대부분 `Exception`을 상속해야 한다. 예외를 잡아 처리하려면 `Exception`의 서브타입이어야 하기 때문이다.
* 코틀린은 특정 예외를 반드시 잡으라고 강제하지 않는다. 즉, 자바와 달리 체크 예외(Checked Exception)가 없다.

> **체크 예외가 없는 코틀린과 자바의 호환성**
> 
> * 코틀린은 자바와 달리 모든 예외가 언체크 예외이다. 즉, 컴파일러가 `try-catch`나 `throws` 명세를 강제하지 않는다.
> * 코틀린 함수에서 예외를 던지는데 자바 컴파일러에게 예외 가능성을 알려주려면 `@Throws` 어노테이션을 명시해야 한다.

```kotlin
// 코틀린에서 정의
@Throws(IOException::class) // 자바 클라이언트를 위한 힌트
fun readFile() {
    throw IOException("파일 없음")
}
```
