# Kotlin - Functional Programming in Kotlin

## 📚 Kotlin에서 배열과 컬렉션 프레임워크를 다루는 방법

* Kotlin의 Collection 프레임워크 
![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/5228384a-00ae-4fc4-ab09-b4e4a6792fd3.png)

* 가변 컬렉션 - 컬렉션에 element를 추가, 삭제할 수 있다.
* 불변 컬렉션 - 컬렉션에 element를 추가, 삭제할 수 없다.

## 📚 Kotlin에서 다양한 함수를 다루는 방법

* Kotlin에서 확장함수라는 개념을 제공한다.
* 확장함수란, 기존 클래스의 소스 코드를 수정하거나 상속하지 않고도 새로운 함수를 추가할 수 있는 기능이다.

> Kotlin Documentation
> * Kotlin provides the ability to extend a class or an interface with new functionality without having to inherit from the class or use design patterns such as Decorator. This is done via special declarations called extensions.
> * For example, you can write new functions for a class or an interface from a third-party library that you can't modify. Such functions can be called in the usual way, as if they were methods of the original class. This mechanism is called an extension function. There are also extension properties that let you define new properties for existing classes.
```kotlin
fun String.lastChar(): Char {
    return this.get(this.length - 1)
}

fun main() {
    val myString = "Kotlin"
    println(myString.lastChar()) // 출력: n
}
```

* 코틀린의 infix 함수는 함수 호출 시 점(.)과 괄호(())를 생략하고 마치 연산자처럼 사용할 수 있게 해주는 기능
> Kotlin Documentation
> * Functions marked with the infix keyword can also be called using the infix notation (omitting the dot and the parentheses for the call). Infix functions must meet the following requirements:
```kotlin
// 일반적인 확장 함수
fun Int.plus(x: Int): Int {
    return this + x
}

// infix 확장 함수
infix fun Int.add(x: Int): Int {
    return this + x
}

fun main() {
    val result1 = 3.plus(5) // 일반적인 함수 호출
    val result2 = 3 add 5   // infix 표기법으로 호출

    println("Result 1: $result1") // 출력: Result 1: 8
    println("Result 2: $result2") // 출력: Result 2: 8
}
```


* 코틀린의 inline 함수는 함수가 호출되는 위치에 함수 본문의 코드를 직접 삽입하는 방식으로 동작하는 특별한 함수이다. 
* 컴파일러가 함수 호출을 제거하고 코드 자체를 복사해 넣기 때문에, 함수 호출로 인한 오버헤드를 줄여 성능을 최적화하는 데 사용한다.
> Kotlin Documentation
> * Using [higher-order functions](https://kotlinlang.org/docs/lambdas.html) imposes certain runtime penalties: each function is an object, and it captures a closure. A closure is a scope of variables that can be accessed in the body of the function. Memory allocations (both for function objects and classes) and virtual calls introduce runtime overhead.
```kotlin
// fun <T, R> T.run(block: T.() -> R): R = block()
inline fun <T, R> T.myRun(block: T.() -> R): R = this.block()

fun main() {
    val result = "Hello".myRun {
        println(this)
        length // 마지막 표현식이 반환값
    }
    println("결과: $result") // 출력: 결과: 5
}
```

* 지역함수는 다른 함수 내부에 정의된 함수를 말한다.
```kotlin
fun saveUser(user: User) {
    // 지역 함수: 유효성 검사 로직을 캡슐화
    fun validate(user: User, value: String, fieldName: String) {
        if (value.isEmpty()) {
            throw IllegalArgumentException("Can't save user ${user.id}: empty $fieldName")
        }
    }

    // 지역 함수를 호출하여 유효성 검사
    validate(user, user.name, "Name")
    validate(user, user.address, "Address")

    // 유효성 검사 통과 후 저장 로직 수행
    // ...
}
```
> Kotlin Documentation
> * Kotlin supports local functions, which are functions inside other functions:

## 📚 Kotlin에서 람다를 다루는 방법

* Kotlin에서 Lambda란 이름이 없는 함수, 즉 익명 함수를 간결하게 표현한 것을 말한다.
* 익명 함수: fun 키워드를 사용해 이름을 지정하는 일반 함수와 달리, 람다는 이름이 없다.
* 값처럼 사용: 함수를 값처럼 취급하여 변수에 저장하거나, 함수의 인자로 전달하거나, 함수에서 반환하는 것이 가능하다.
* 고차 함수와 함께 사용: 람다는 함수를 인자로 받거나 함수를 반환하는 고차 함수(higher-order function)와 함께 자주 사용된다.
* 간결한 문법: 불필요한 코드를 줄여 가독성을 높이고 코드를 더 간결하게 작성할 수 있다.
> Kotlin Documentation
> * Lambda expressions and anonymous functions are function literals. Function literals are functions that are not declared but are passed immediately as an expression. Consider the following example:
```kotlin
max(strings, { a, b -> a.length < b.length })
```

## 📚 Kotlin에서 컬렉션을 함수형으로 다루는 방법

### all
* 조건을 모두 만족하면 true, 그렇지 않으면 false
> Kotlin Documentation
> * Returns true if all elements match the given [predicate](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/all.html).
> * Note that if the array contains no elements, the function returns true because there are no elements in it that do not match the predicate. See a more detailed explanation of this logic concept in ["Vacuous truth"](https://en.wikipedia.org/wiki/Vacuous_truth) article.

### none
* 조건을 모두 불만족하면 true, 그렇지 않으면 false
> Kotlin Documentation
> * Returns true if the array has no elements.

### any
* 조건을 하나라도 만족하면 true, 그렇지 않으면 false
> Kotlin Documentation
> * Returns true if array has at least one element.

### count
* 개수를 센다.
> Kotlin Documentation
> * Returns the number of elements in this array.

### sortedBy
* 오름차순 정렬을 한다.
> Kotlin Documentation
> * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/sorted-by.html) function.
> * The sort is stable. It means that elements for which [selector](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/sorted-by.html) returned equal values preserve their order relative to each other after sorting

### sortedByDescending
* 내림차순 정렬을 한다.
> Kotlin Documentation
> * Returns a list of all elements sorted descending according to their natural sort order.

### distinctBy
* 변형된 값을 기준으로 중복을 제거한다.
> Kotlin Documentation
> * Returns a list containing only elements from the given array having distinct keys returned by the given [selector](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/distinct-by.html) function.
> * Among elements of the given array with equal keys, only the first one will be present in the resulting list. The elements in the resulting list are in the same order as they were in the source array.

### first
* 첫 번째 값을 가져온다.(반드시 null이 아니어야 한다.)
> Kotlin Documentation
> * Returns the first element.

### firstOrNull
* 첫 번째 값 또는 null을 가져온다.
> Kotlin Documentation
> * Returns the first element, or null if the array is empty.

### last
* 마지막 값을 가져온다.(반드시 null이 아니어야 한다.)
> Kotlin Documentation
> * Returns the last element.

### lastOrNull
* 마지막 값 또는 null을 가져온다.
> Kotlin Documentation
> * Returns the last element, or null if the array is empty.

### groupBy
* 컬렉션(List, Set 등)의 요소들을 특정 기준에 따라 그룹화하여 Map 형태로 반환하는 표준 라이브러리 확장 함수이다.
> Kotlin Documentation
> * Groups elements of the original array by the key returned by the given [keySelector](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/group-by.html) function applied to each element and returns a map where each group key is associated with a list of corresponding elements.
> * The returned map preserves the entry iteration order of the keys produced from the original array.
