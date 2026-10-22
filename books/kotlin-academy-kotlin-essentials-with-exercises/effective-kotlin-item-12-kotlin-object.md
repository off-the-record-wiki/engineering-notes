# (Effective Kotlin Item 12) Kotlin - Object

#### 객체 표현식

* 표현식으로 빈 객체를 만들려면 object 키워드와 중괄호를 사용한다.
* 객체를 생성하는 이러한 구문을 객체 표현식이라고 한다.

```kotlin
var instance = object {}
```

* 빈 객체는 어떤 객체도 확장하지 않으며, 인터페이스를 구현하지도 않고, 본문에도 아무 내용이 없다.
* 그럼에도 불구하고 빈 객체는 쓸모가 있다.
* 빈 객체의 의미는 유일성에 있다. 즉, 다른 어떤 객체와도 같지 않은 존재라는 뜻이다.
* 그래서 특정 토큰이나 동기화 락을 사용하기에 적합하다.

```kotlin
class Box {
    var value: Any? = NOT_SET

    fun initialized() = value != NOT_SET

    companion object {
        private val NOT_SET = object {}
    }
}

private val LOCK = object {}
fun synchronizedOperation() = synchronized(LOCK) {
    // ...
}
```

* 빈 객체는 `Any`의 생성자로도 만들 수 있기 때문에 `object {}` 대신 `Any()`를 사용해도 된다.

```kotlin
private val NOT_SET = Any()
```

* 하지만 객체 표현식으로는 만드는 객체가 반드시 비어 있을 필요가 없다.
* 본문을 가질 수 있으며 클래스를 상속하거나 인터페이스를 구현할 수도 있다.
* 다만 객체 표현식은 class 대신 object 키워드를 사용하며, 이름이나 생성자를 정의해선 안 된다.

#### 객체 선언

* 객체 표현식에 이름을 부여하면 객체 선언이 된다.
* 객체 표현식과 똑같이 하나의 객체를 생성하지만, 참조할 수 있는 이름이 있기 때문에 익명 객체는 아니라고 볼 수 있다.

```kotlin
object Point {
    var x = 0
    var y = 0
}

fun main() {
    println(Point.x)  // 0
    Point.y = 10
    println(Point.y)  // 10

    val p = Point
    p.x = 20
    println(Point.x)  // 20
    println(Point.y)  // 10
}
```

* 객체 선언은 싱글톤 패턴을 구현한 것으로, 인스턴스가 하나뿐인 클래스를 만든다.
* 이 클래스를 사용할 때마다 오직 이 하나의 인스턴스를 쓰게 된다는 뜻이다.
* 객체 선언은 다른 클래스를 확장하거나 인터페이스를 구현하는 등, 보통의 클래스가 지원하는 기능을 모두 지원한다.

```kotlin
data class User(val name: String)

interface UserProducer {
    fun produce(): User
}

object FakeUserProducer : UserProducer {
    override fun produce() : User = User("fake")
}

fun setUserProducer(producer: UserProducer) {
    println(producer.produce())
}

fun main() {
    setUserProducer(FakeUserProducer)
}
```

> * 코틀린의 `object` 싱글톤 객체는 애플리케이션이 시작될 때 미리 생성되는 것이 아니라 코드에서 해당 객체에 처음 접근할 때 메모리에 올라간다.
> * JVM의 클래스 로더 메커니즘을 타기 때문에 별도의 처리 없이도 완벽한 쓰레드 안전성을 보장받는다.

#### 컴패니언 객체(companion object)

* 객체 선언 앞에 companion 키워드를 붙이면 객체를 건너뛰고 그 안의 요소들을 곧바로 호출할 수 있다.

```kotlin
class User {
    companion object Producer { 
        fun empty() = User()
    }
}

// 사용 예시 코드
val user: User = User.empty()
val user: User = User.Producer.empty()
```

* 이처럼 companion 키워드가 붙은 객체를 컴페니언 객체라 하며, 객체 이름을 생략할 수 있다.
* 기본 이름은 Companion이다.

> * 컴패니언 객체는 바이트코드 레벨에서 진짜 `static`이 아니라 `Companion`이라는 이름의 정적 인스턴스를 통해 접근하는 구조이다. 따라서 자바 프레임워크에서 코틀린의 정적 메서드나 필드를 리플렉션으로 찾게 하려면, 반드시 `@JvmStatic`이나 `@JvmField` 어노테이션을 붙여주어야 완벽한 호환성이 보장된다.

```kotlin
class User {
    companion object {  // 객체 이름은 생략해도 된다.
        fun empty() = User()
    }
}
```

#### 데이터 객체 선언

* Kotlin 1.8부터는 객체 선언 시 data 제어자를 사용할 수 있다.
* data 제어자는 객체의 이름을 문자열로 반환하는 toString 메서드를 만들어준다.

```kotlin
data object ABC

fun main() {
    println(ABC)
}
```

#### 상수

* Kotlin에서 상수는 일반적으로 컴패니언 객체의 프로퍼티로 정의하고 이름은 `UPPER_SNAKE_CASE`처럼 대문자와 밑줄만 사용하여 짓는다.
* 이 방식을 이용하면 상수에 이름을 붙일 수도 있고 추후에 값을 바꾸기도 쉽다.
* 이런 특징적인 방법은 상수임을 명확하게 표시하는 기능도 한다.

```kotlin
class Product(val code: String, val price: Double) {
    
    init {
        require(price > MIN_AMOUNT)
    }
 
    companion object {
        val MIN_AMOUNT = 5.00
    }
}
```
* 컴패니언 객체 프로퍼티나 최상위 프로퍼티가 원시 타입이나 String인 상수라면 const 제어자를 추가할 수 있다. 이는 최적화에 해당된다.
* `const`가 붙은 변수를 사용하는 코드는 컴파일할 때 모두 해당 상수값으로 치환된다.
```kotlin
class Product(val code: String, val price: Double) {
    
    init {
        require(price > MIN_AMOUNT)
    }
 
    companion object {
        const val MIN_AMOUNT = 5.00
    }
}
```

> * `val`은 런타임에 값이 할당될 수 있지만, `const val`은 반드시 컴파일 타임에 값을 알아야 한다.
> * 그래서 원시 타입과 `String`만 가능하다. 컴파일러는 `const val`을 참조하는 모든 코드를 변수 접근 방식이 아니라 해당 값 자체로 복사해서 치환해버린다. 덕분에 런타임에 변수를 조회하는 메모리 오버헤드 자체가 사라진다.
