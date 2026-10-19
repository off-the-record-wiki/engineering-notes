# Kotlin - Object-Oriented Programming in Kotlin

## 📚 코틀린에서 클래스를 다루는 방법

```kotlin
// Kotlin Version
class Person(val name: String, val age: Int)
```
* 코틀린에서 롬복(Lombok)을 사용하는 것은 일반적으로 권장되지 않는다.
* 그 이유는 코틀린 자체가 롬복이 제공하는 대부분의 기능을 언어 차원에서 내장하고 있기 때문이다.

```kotlin
class Person(val name: String, val age: Int) {
    init {
        println("Person 객체가 생성되었습니다: $name, $age")
    }
}
```
* 보통 생성자를 명시해주지 않는데 초기화 로직이 필요한 경우 `init` 블록을 사용한다.
* `init` 블록에서 값을 검증하거나 기타 등등의 로직을 작성할 수 있다.
* 부생성자를 사용할 순 있으나 그보다는 정적 팩토리 메서드를 추천한다.

## 📚 코틀린에서 상속을 다루는 방법

* Java의 추상클래스와 동일한 개념
* Kotlin에서 기본적으로 모든 클래스와 멤버는 final이다. 재정의를 허용하려면 open 키워드를 명시해야 한다.

```kotlin
abstract class Person(val name: String, val age: Int)
```

> Kotlin Documentation
> * In Kotlin, abstract classes are classes that can't be instantiated directly. They are designed to be inherited by other classes which define their actual behavior. This behavior is called an implementation. 
```kotlin
// Abstract class with a primary constructor that declares name and age
abstract class Person(
    val name: String,
    val age: Int
) {
    // Abstract member 
    // Doesn't provide implementation,
    // and it must be implemented by subclasses
    abstract fun introduce()

    // Non-abstract member (has an implementation)
    fun greet() {
        println("Hello, my name is $name.")
    }
}

// Subclass that provides an implementation for the abstract member
class Student(
    name: String,
    age: Int,
    val school: String
) : Person(name, age) {
    override fun introduce() {
        println("I am $name, $age years old, and I study at $school.")
    }
}

fun main() {
    // Creates an instance of the Student class
    val student = Student("Alice", 20, "Engineering University")
    
    // Calls the non-abstract member
    student.greet()
    // Hello, my name is Alice.
    
    // Calls the overridden abstract member
    student.introduce()
    // I am Alice, 20 years old, and I study at Engineering University.
}
```

* Java의 Interface와 동일한 개념

```kotlin
interface MyInterface {
    fun bar()
    fun foo() {
      // optional body
    }
}
```
> Kotlin Documentation
> * Interfaces in Kotlin can contain declarations of abstract methods, as well as method implementations. What makes them different from abstract classes is that interfaces cannot store state. They can have properties, but these need to be abstract or provide accessor implementations.

```kotlin
class Child : MyInterface {
    override fun bar() {
        // body
    }
}
```

## 📚 코틀린에서 접근 제어를 다루는 방법

* public : 모든 곳에서 접근 가능
* protected : 선언된 클래스 또는 하위 클래스에서만 접근 가능
* internal : 같은 모듈에서만 접근 가능
* private : 선언된 클래스 내에서만 접근 가능
> Kotlin Documentation
> * There are four visibility modifiers in Kotlin: private, protected, internal, and public. The default visibility is public.
> * private means that the member is visible inside this class only (including all its members).
> * protected means that the member has the same visibility as one marked as private, but that it is also visible in subclasses.
> * internal means that any client inside this module who sees the declaring class sees its internal members.
> * public means that any client who sees the declaring class sees its public members.

## 📚 코틀린에서 object 키워드를 다루는 방법

* Java는 static 키워드를 제공해 클래스가 인스턴스화 될 때 새로운 값이 복제되는게 아니라 정적으로 인스턴스끼리 값을 공유하는 부분을 제공해주나 Kotlin에는 static 키워드가 없다.
* 그러나 Kotlin에는 이를 대체하는 키워드로 companion object가 있는데 이는 클래스와 동행하는 유일한 오브젝트를 말한다.
> Kotlin Documentation
> * In Kotlin, each class can have a [companion object](https://kotlinlang.org/docs/object-declarations.html#companion-objects). Companion objects are a type of object declaration that allows you to access its members using the class name without creating a class instance.
```kotlin
// Class with a primary constructor that declares the name property
class Person(
    val name: String
) {
    // Class body with a companion object
    companion object {
        fun createAnonymous() = Person("Anonymous")
    }
}

fun main() {
    // Calls the function without creating an instance of the class
    val anonymous = Person.createAnonymous()
    println(anonymous.name)
    // Anonymous
}
```

* Java에서 Singleton을 구현하는 방법과는 달리 Kotlin에서는 Object declarations를 사용해 싱글톤 객체를 만든다.
```kotlin
// Declares a Singleton object to manage data providers
object DataProviderManager {
    private val providers = mutableListOf<DataProvider>()

    // Registers a new data provider
    fun registerDataProvider(provider: DataProvider) {
        providers.add(provider)
    }

    // Retrieves all registered data providers
    val allDataProviders: Collection<DataProvider> 
        get() = providers
}
```
> Kotlin Documentation
> * Using singletons for shared resources: You need to ensure that only one instance of a class exists throughout the application.
> * Creating factory methods: You need a convenient way to create instances efficiently. [Companion objects](https://kotlinlang.org/docs/object-declarations.html#companion-objects) allow you to define class-level functions and properties tied to a class, simplifying the creation and management of these instances.
> * Modifying existing class behavior temporarily: You want to modify the behavior of an existing class without the need to create a new subclass. For example, adding temporary functionality to an object for a specific operation.
> * Type-safe design is required: You require one-time implementations of interfaces or [abstract classes](https://kotlinlang.org/docs/classes.html#abstract-classes) using object expressions. This can be useful for scenarios like a button click handler.
> * You can create single instances of objects in Kotlin using object declarations, which always have a name following the object keyword. This allows you to define a class and create an instance of it in a single step, which is useful for implementing singletons:

## 📚 코틀린에서 다양한 클래스를 다루는 방법

* data class는 계층 간의 데이터를 전달하기 위한 DTO(Data Transfer Object)로 사용되며 데이터(필드), 생성자와 Getter, equals, hashCode, toString이 구현되어 있다.
```kotlin
data class User(val name: String, val age: Int)
```
> Kotlin Documentation
> * The compiler automatically derives the following members from all properties declared in the primary constructor:
> * equals()/hashCode() pair.
> * toString() of the form "User(name=John, age=42)".
> * [componentN() functions](https://kotlinlang.org/docs/destructuring-declarations.html) corresponding to the properties in their order of declaration.
> * copy() function (see below).
---
> * To ensure consistency and meaningful behavior of the generated code, data classes have to fulfill the following requirements:
> * The primary constructor must have at least one parameter.
> * All primary constructor parameters must be marked as val or var.
> * Data classes can't be abstract, open, sealed, or inner.

* enum은 Java와 Kotlin 둘 다 동일하게 제공된다.
> Kotlin Documentation
> * The most basic use case for enum classes is the implementation of type-safe enums:
```kotlin
enum class Direction {
    NORTH, SOUTH, WEST, EAST
}
```
> * Each enum constant is an object. Enum constants are separated by commas.
```kotlin
enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}
```
---
* sealed 클래스와 인터페이스는 상속을 제한하여 특정 하위 클래스들만 상속하거나 구현할 수 있도록 '봉인'하는 기능을 한다.
* 이를 통해 클래스 계층 구조를 명확하게 제어하고, 컴파일러가 모든 하위 타입을 미리 알 수 있도록 한다.
* Kotlin의 sealed class와 sealed interface는 제한된 계층 구조를 만들 수 있게 해주는 강력한 기능이다.
* sealed class는 자신을 상속하는 클래스들을 같은 파일 내에서만 정의할 수 있도록 제한하는 추상 클래스이다.
```kotlin
sealed class Result {
    data class Success(val data: String) : Result()
    data class Error(val message: String) : Result()
    data object Loading : Result()
}

fun handleResult(result: Result) = when (result) {
    is Result.Success -> println("성공: ${result.data}")
    is Result.Error -> println("에러: ${result.message}")
    Result.Loading -> println("로딩 중...")
}
```
* sealed interface는 sealed class와 비슷하지만, 다중 상속이 가능하다는 차이가 있다.
```kotlin
sealed interface ApiResponse
sealed interface Cacheable

data class UserData(val name: String) : ApiResponse, Cacheable
data class ErrorResponse(val code: Int) : ApiResponse
```
