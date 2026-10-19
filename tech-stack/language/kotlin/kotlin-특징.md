# Kotlin - Key Features of Kotlin

## 📚 Type Alias와 as import

* kotlin에서 타입 별칭은 typealias 키워드를 사용하여 기존 타입에 새로운 이름(별칭)을 부여하는 기능이다.
* 코드 가독성 향상: 복잡하거나 길고 의미가 불분명한 타입에 의미 있는 이름을 부여하여 코드의 가독성을 크게 향상시킨다.
* 긴 타입 이름 축약: 특히 제네릭을 많이 사용하는 복잡한 타입(Map\<String, List<MutableSet\<Int>>>)을 짧고 간결하게 만들 수 있다.
* 의미 명확화: String과 같은 일반적인 타입을 도메인에 맞는 용어로 별칭을 지정하여 코드의 의도를 더 명확하게 표현할 수 있다.
* 리팩토링 용이성: 만약 특정 타입 구조가 변경될 때, 타입 별칭만 수정하면 되므로 관련된 모든 코드를 일일이 수정할 필요가 없어 리팩토링이 쉬워진다.
```kotlin
// 원래 타입
val userCache: HashMap<String, MutableList<String>> = hashMapOf()

// typealias를 사용해 짧고 명확하게 만듦
typealias UserCache = HashMap<String, MutableList<String>>
val userCache2: UserCache = hashMapOf()
```

## 📚 구조분해와 componentN 함수

* componentN은 **구조 분해 선언(Destructuring Declaration)**을 지원하기 위해 사용되는 특별한 함수이다.
* 개발자가 val (a, b) = someObject 와 같은 구문을 사용해 객체의 속성들을 여러 변수에 한 번에 할당할 때, 코틀린 컴파일러는 내부적으로 someObject.component1(), someObject.component2() 와 같은 함수들을 호출하여 각 변수에 값을 할당한다.
* 즉, componentN은 구조 분해 선언의 이면에서 작동하는 규약이다.
```kotlin
data class Person(val name: String, val age: Int)

fun main() {
    val person = Person("Alice", 30)

    // 구조 분해 선언 사용
    val (name, age) = person

    // 컴파일러가 변환하는 내부 코드
    // val name = person.component1()
    // val age = person.component2()

    println("Name: $name, Age: $age") // 출력: Name: Alice, Age: 30
}
```

## 📚 TakeIf와 TakeUnless

* 코틀린의 takeIf와 takeUnless는 수신 객체가 특정 조건을 만족하는지 여부에 따라 수신 객체를 반환하거나 null을 반환하는 표준 라이브러리 함수이다.
> Kotlin Documentation
> * Returns this value if it satisfies the given [predicate](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/take-if.html) or null, if it doesn't.
> * Returns this value if it does not satisfy the given [predicate](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/take-unless.html) or null, if it does.
```kotlin
val number = 10
val evenNumber = number.takeIf { it % 2 == 0 } // 조건이 true이므로 10 반환
val oddNumber = number.takeIf { it % 2 != 0 }  // 조건이 false이므로 null 반환

println(evenNumber) // 10
println(oddNumber)  // null
```
```kotlin
val number = 10
val oddNumber = number.takeUnless { it % 2 == 0 } // 조건이 true이므로 null 반환
val evenNumber = number.takeUnless { it % 2 != 0 }  // 조건이 false이므로 10 반환

println(oddNumber)  // null
println(evenNumber) // 10
```

## 📚 scope function

* 객체의 컨텍스트(context) 내에서 코드 블록을 실행하기 위한 코틀린 표준 라이브러리 함수이다.
* 스코프 함수를 사용하면 객체의 이름을 반복해서 쓰지 않고도 해당 객체의 속성이나 메서드를 간결하게 처리할 수 있다.

|Function|Object Reference|Return Value|Is extension function|
|:---:|:---:|:---:|:---:|
|let|it|Lambda result|Yes|
|run|this|Lambda result|Yes|
|run|-|Lambda result|No: called without the context object|
|with|this|Lambda result|No: takes the context object as an argument.|
|apply|this|Context Object|Yes|
|also|it|Context Object|Yes|

> Kotlin Documentation
> * let : Calls the specified function [block](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/let.html) with this value as its argument and returns its result.
> * run : Calls the specified function [block](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/run.html) and returns its result.
> * with : Calls the specified function [block](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/with.html) with the given [receiver](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/with.html) as its receiver and returns its result.
> * apply : Calls the specified function [block](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/apply.html) with this value as its receiver and returns this value.
> * also : Calls the specified function [block](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/also.html) with this value as its argument and returns this value.
