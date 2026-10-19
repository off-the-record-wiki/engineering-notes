# (Effective Kotlin Item 6) Kotlin - function

#### Unit 반환 타입

* 타입을 명시하지 않으면 기본 결과 타입은 `Unit`이 되며, 기본 결괏값은 `Unit` 객체가 된다.
* `Unit`은 아무것도 반환되지 않을 때 자리 표시자로 사용되는 아주 간단한 객체이다.
* `return`이 없는 함수를 정의하면 결과 타입이 `Unit`이 된다.

#### vararg 매개변수

* vararg 매개변수는 여러 값의 묶음으로 실제로는 선언한 타입의 배열이 만들어진다.
* 그래서 for문을 사용해 순회를 할 수 있다.

#### 함수 오버로딩(overloading)

* 코틀린에서는 매개변수 타입이 다르거나 매개변수 개수가 다르다면 같은 스코프에서라도 이름이 같은 함수를 정의할 수 있다.
* 이를 함수 오버로딩(function overloading)이라고 한다.
* 코틀린은 주어진 인수를 보고 어떤 함수를 호출할지 결정한다.

```kotlin
fun main() {
    // 1. 기본 함수 호출
    sayHello()

    // 2. 매개변수와 반환 타입이 있는 함수 호출
    val sumResult = addNumbers(10, 20)
    println("10 + 20 = $sumResult")

    // 3. 단일 표현식 함수 호출
    val maxVal = findMax(5, 12)
    println("더 큰 값: $maxVal")

    // 4. 이름 있는 인자 활용 (가독성 향상)
    printUserInfo(name = "Kotlin", age = 5, city = "Seoul")

    // 5. 확장 함수 호출
    val greeting = "Hello".addExclamation()
    println(greeting) // Hello!

    // 6. 고차 함수 및 람다식 활용 (컬렉션 처리)
    val numbers = listOf(1, 2, 3, 4, 5)
    val squared = numbers.map { it * it }
    println("제곱 값 리스트: $squared")
}

// 1. 반환 값이 없는(Unit) 기본 함수 (Unit은 생략 가능)
fun sayHello(): Unit {
    println("Hello, Kotlin!")
}

// 2. 매개변수와 반환 타입이 있는 함수
fun addNumbers(a: Int, b: Int): Int {
    return a + b
}

// 3. 단일 표현식 함수 (Expression Body Syntax)
// 본문이 하나의 식으로 구성될 경우, 중괄호와 return을 생략할 수 있습니다.
fun findMax(a: Int, b: Int): Int = if (a >= b) a else b

// 4. 기본값(Default arguments)과 이름 있는 인자(Named arguments)를 사용한 함수
fun printUserInfo(name: String, age: Int, city: String = "Unknown") {
    println("이름: $name, 나이: $age, 도시: $city")
}

// 5. 확장 함수 (Extension Function)
// 기존 String 클래스에 새로운 함수를 추가합니다.
fun String.addExclamation(): String {
    return "$this!"
}
```

* 단일 표현식 함수와 같이 반환 타입을 생략할 수 있더라도 외부로 노출되는 Public API나 복잡한 로직을 가진 단일 표현식 함수는 반드시 반환 타입을 명시해야 한다.
* 추론된 타입이 개발자의 의도와 다를 수 있고, 코드를 읽는 사람의 가독성을 떨어뜨리기 때문이다.
