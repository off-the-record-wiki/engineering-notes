# (Effective Kotlin Item 5) Kotlin - control Flow: if, when, try, and while

#### Kotlin에서의 if문

```kotlin
fun checkNumber(number: Int) {
    if (number > 0) {
        println("양수입니다.")
    } else if (number < 0) {
        println("음수입니다.")
    } else {
        println("0입니다.")
    }
}
```

* if, when, try-catch는 문(statement)이 아니라 식(expression)이다.
* 코틀린에서 제어문과 예외 처리 블록의 가장 큰 특징은 값을 반환하는 식이라는 점이다.

#### switch와 when

* Kotlin에는 자바의 switch문이 존재하지 않으며, 그 역할을 훨씬 강력하고 유연하게 대처하는 것이 바로 when 표현식이다.
* when은 단순한 값 비교를 넘어 타입 검사, 범위 확인 등 다양한 조건을 다룰 수 있다.

```java
// Java Version
String dayName;
switch (day) {
    case 1:
        dayName = "월요일";
        break;
    case 2:
        dayName = "화요일";
        break;
    default:
        dayName = "기타";
        break;
}
```
```kotlin
// Kotlin Version
val dayName = when (day) {
    1 -> "월요일"
    2 -> "화요일"
    else -> "기타"
}
```

* when이 값을 반환하는 식으로 사용될 때, 컴파일러는 모든 경우의 수가 커버되었는지 검사한다.
* when을 식으로 쓸 때 일반적인 값 비교는 컴파일러가 모든 경우를 알 수 없으므로 else문이 필수이다. 단, enum class나 sealed class를 판별할 때는 모든 조건을 나열하면 else문이 없이도 컴파일되며, 나중에 새로운 조건이 추가되었을 때 컴파일 에러로 알려주므로 컴파일 시점에 버그를 예방할 수 있다.

#### Kotlin에서의 Progression과 Range

* Range는 값의 범위(start부터 end까지)**를 나타내는 개념이며, Progression은 Range를 기반으로 값을 순차적으로 생성하는 등차수열과 같은 개념이다.

#### Kotlin에서 예외를 다루는 방법

* 예외가 발생할 가능성이 있는 코드는 try 블록에, 예외 처리 코드는 catch 블록에 작성한다. 예외 발생 여부와 상관없이 항상 실행되어야 할 코드는 finally 블록에 넣어야 한다.
```kotlin
fun convertToInt(s: String): Int {
    try {
        return s.toInt() // 예외가 발생할 수 있는 코드
    } catch (e: NumberFormatException) {
        println("숫자 형식이 아닙니다: ${e.message}") // 예외 처리
        return 0
    } finally {
        println("변환 시도 완료") // 항상 실행
    }
}
```

* Kotlin에서 예외를 직접 던지고자 할 떄는 throw 키워드를 사용한다. throw 표현식의 타입은 Nothing이다. 이는 해당 코드가 절대로 완료되지 않음을 의미한다.
```kotlin
fun checkAge(age: Int) {
    if (age < 0) {
        throw IllegalArgumentException("나이는 음수가 될 수 없습니다.")
    }
}
```
* Kotlin에는 try with resources 구문이 없다. 대신 코틀린의 언어적 특징을 활용해 close를 호출해준다.
* Kotlin에는 자바의 try with resources와 동일한 역할을 하는 표준 라이브러리 확장 함수 use를 사용한다.
* Kotlin에서 모든 예외는 Unchecked Exception이다.

```kotlin
fun main() {
    val fileName = "file.txt"
    try {
        // use 함수를 사용하여 자원 자동 해제
        BufferedReader(FileReader(fileName)).use { reader ->
            val line = reader.readLine()
            println(line)
        }
    } catch (e: Exception) {
        println("파일을 읽는 도중 오류 발생: ${e.message}")
    }
}
```

* use 함수를 사용하면 다음과 같은 장점을 누릴 수 있다.
  * 가독성 : 코틀린의 람다 문법을 활용해 더욱 간결
  * 안전성 : 자원 누수를 방지하는 가장 안전한 방법이다.
  * 확장성 : AutoCloseable을 구현하는 모든 객체에 대해 작동하므로, 파일 스트림뿐만 아니라 데이터베이스 커넥션 등 다양한 리소스에 적용할 수 있다.

#### Kotlin에서 함수 선언 문법

```kotlin
// 매개변수 없이 실행되는 함수
fun sayHello() {
    println("안녕하세요!")
}

fun main() {
    sayHello() // 함수 호출
}
```

```kotlin
// 함수 이름 뒤에 괄호를 사용하여 매개변수와 타입을 정의
fun greet(name: String) {
    println("안녕하세요, $name님!")
}

fun main() {
    greet("김민준") // 인자 전달
}
```

```kotlin
// 반환하는 값의 타입을 매개변수 목록 뒤에 콜론(:)과 함께 명시합니다. return 키워드를 사용해 값을 반환
fun add(a: Int, b: Int): Int {
    return a + b
}

fun main() {
    val result = add(5, 3)
    println("덧셈 결과: $result") // 덧셈 결과: 8
}
```

#### Kotlin - default parameter

```kotlin
fun displayBorder(character: Char = '=', length: Int = 15) {
    for (i in 1..length) {
        print(character)
    }
    println()
}

fun main() {
    displayBorder() // ======
    displayBorder(character = '*') // ***************
    displayBorder(length = 5) // =====
    displayBorder(character = '+', length = 10) // ++++++++++
}
```

#### Kotlin - named argument(parameter)

```kotlin
fun reformat(
    str: String,
    normalizeCase: Boolean = true,
    upperCaseFirstLetter: Boolean = true,
    divideByCamelHumps: Boolean = false,
    wordSeparator: Char = ' ',
) { /*...*/ }
```

#### Kotlin - 같은 타입의 여러 파라미터 받기(가변인자)

* 동일한 타입의 여러 파라미터를 받으려면 `vararg`를 사용할 수 있다.

```kotlin
fun <T> asList(vararg ts: T): List<T> {
    val result = ArrayList<T>()
    for (t in ts) // ts is an Array
        result.add(t)
    return result
}
```

* 코틀린에서는 이미 존재하는 배열을 가변인자 함수에 넘길 때 배열 앞에 `*` 스프레드 연산자를 붙여서 펼쳐주어야 한다.
