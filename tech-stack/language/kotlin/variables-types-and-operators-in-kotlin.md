# Kotlin - Variables, Types, and Operators in Kotlin

## 📚 변수 선언 키워드 - var vs val의 차이점

* 바꿀 수 있는 부분에 대해선 var
* 바꿀 수 없는 부분에 대해선 val

> Kotlin Documentation
> * In Kotlin, you declare a variable starting with a keyword, val or var, followed by the name of the variable.
> * Use the val keyword to declare variables that are assigned a value only once. These are immutable, read-only local variables that can't be reassigned a different value after initialization:
> * Use the var keyword to declare variables that can be reassigned. These are mutable variables, and you can change their values after initialization:
> * Kotlin supports type inference and automatically identifies the data type of a declared variable. When declaring a variable, you can omit the type after the variable name:

## 📚 Kotlin에서 Primitve Type

* Kotlin은 Java와 달리 모든 타입이 객체로 간주된다.
* 컴파일러가 알아서 JVM의 원시 타입 또는 래퍼 클래스로 변환해준다. 하지만 코틀린 컴파일러의 최적화 덕분에 일반적인 상황에서 큰 차이를 느끼기는 어렵다.

> Kotlin Documentation
> * In Kotlin, everything is an object in the sense that you can call member functions and properties on any variable. While certain types have an optimized internal representation as primitive values at runtime (such as numbers, characters, and booleans), they appear and behave like regular classes to you.

## 📚 Kotlin에서 Nullable 변수

* Kotlin에서는 변수에 null이 들어갈 수 있다면 "타입?"를 사용해야 한다.

> Kotlin Documentation
> * To allow null values, declare a variable with a ? sign right after the variable type. For example, you can declare a nullable string by writing String?. This expression makes String a type that can accept null:

## 📚 Elvis operator﻿와 Safe Call

* Safe Call이란 null이 될 수 있는 타입 객체에 안전하게 접근하기 위한 연산자이다.
* 기호로 `?.`기호를 사용하며 null Safety을 보장하는 코틀린의 핵심 기능 중 하나이다.

```kotlin
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {

    var name: String? = null
    val nickname: String? = "kotlin"

    println(name?.length)
    println(nickname?.length)
}
```

> Kotlin Documentation
> * The safe call operator ?. allows you to handle nullability safely in a shorter form. Instead of throwing an NPE, if the object is null, the ?. operator simply returns null:
> * You can use the ?. operator with both [var and val variables](https://kotlinlang.org/docs/basic-syntax.html#variables) in Kotlin:

* Elvis operator이란, 연산 결과가 Null이면 뒤의 값을 사용할 수 있는 것을 말한다.

> Kotlin Documentation
> * When working with nullable types, you can check for null and provide an alternative value. For example, if b is not null, access b.length. Otherwise, return an alternative value:

```kotlin
val str: String? = "ABC"
str.length  // (X)
str?.length // (O)
```
* 혹시나 null이 들어오면 NPE가 발생할 수 있기 때문에 정말 null이 아닌게 확실한 경우에만 null이 아님을 단언하는 `!!`을 사용해야 한다.

## 📚 기본 타입

* Java와 동일한 타입을 제공한다.
* 코틀린은 선언된 기본값을 보고 타입을 추론한다.

```kotlin
val number1 = 3   // Int
val number2 = 3L  // Long 
```
* 위와 같이 L을 붙이지 않으면 Int로 간주되고 L이 붙으면 Long으로 간주된다.

```kotlin
val number1 = 3.0f   // Float
val number2 = 3.0    // Double
```
* 위와 같이 f을 붙이면 Float으로 간주되고 f가 붙지 않으면 Double로 간주된다.

## 📚 타입 캐스팅

* Java에서 타입 캐스팅을 위해 instanceof를 사용했다면 Kotlin에서는 타입 캐스팅을 위해 is를 사용한다.

```kotlin
fun printAgeIfPerson(obj: Any) {
    if (obj is Person) {
        val person = obj
    }
}
```

## 📚 Kotlin에서 제공되는 3가지 타입

### Any

* Java의 Object 역할
* 모든 Primitve Type의 최상위 타입도 Any이다.
* Any 자체로는 null을 포함할 수 없어 null을 포함시키고 싶다면 Any?로 표현해야한다.
* Any에 equals, hashCode, toString이 존재한다.

> Kotlin Documentation
> * The root of the Kotlin class hierarchy. Every Kotlin class has [Any](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-any/index.html) as a superclass.

### Unit

* Unit은 Java의 void와 동일한 역할
* void와 다르게 Unit은 그 자체로 타입 인자로 사용 가능하다.
* 함수형 프로그래밍에서 Unit은 단 하나의 인스턴스만 갖는 타입을 의미한다. 즉, Kotlin의 Unit은 실제 존재하는 타입이라는 것을 표현한다.

> Kotlin Documentation
> * The type with only one value: the Unit object. This type corresponds to the void type in Java.

### Nothing

* Nothing은 함수가 정상적으로 끝나지 않았다는 사실을 표현하는 역할
* 무조건 예외를 반환하는 함수 / 무한 루프 함수 등

> Kotlin Documentation
> * Nothing has no instances. You can use Nothing to represent "a value that never exists": for example, if a function has the return type of Nothing, it means that it never returns (always throws an exception).

## 📚 String Interpolation, String indexing

* ${변수}를 사용하면 값이 들어간다.
