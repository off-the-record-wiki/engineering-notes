# (Effective Kotlin Item 3) Kotlin - variable

#### 변수(variable)

* 코틀린에서 변수를 선언하려면 `val`이나 `var` 키워드로 시작하고 변수 이름, 등호, 초깃값을 차례로 입력하면 된다.
* `variable(변수)`을 의미하는 `var` 키워드는 읽고 쓸 수 있는 변수를 뜻하며, 초기화 이후에도 언제든 값을 변경할 수 있는 변수를 정의할 때 사용된다.
* `value(값)`을 의미하는 `val` 키워드는 읽기만 가능한 변수를 뜻하며, 초기화 이후에는 절대 바뀌지 않는 값을 정의할 때 사용된다.

```kotlin
var name = "홍길동"
println(name)  // 출력: 홍길동

name = "김철수"  // 값 변경 가능
println(name)  // 출력: 김철수

var age = 25
age = 26  // 값 변경 가능
```

```kotlin
val pi = 3.14159
println(pi)  // 출력: 3.14159

// pi = 3.14  // 컴파일 에러!  val은 재할당 불가능

val country = "대한민국"
// country = "한국"  // 컴파일 에러! 
```

#### 타입 명시
```kotlin
// 타입을 명시적으로 지정
var score: Int = 100
val message: String = "안녕하세요"

// 타입 추론 (코틀린이 자동으로 타입 결정)
var count = 10  // Int로 추론
val greeting = "Hello"  // String으로 추론
```
