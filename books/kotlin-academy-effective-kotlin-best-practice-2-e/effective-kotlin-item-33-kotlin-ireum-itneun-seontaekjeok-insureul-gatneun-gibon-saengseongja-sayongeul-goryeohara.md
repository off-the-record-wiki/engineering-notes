# (Effective Kotlin Item 33) Kotlin - 이름 있는 선택적 인수를 갖는 기본 생성자 사용을 고려하라

#### 이름 있는 선택적 인수를 갖는 기본 생성자 사용을 고려하라

* 객체를 정의하고 생성 방식을 지정할 때 가장 많이 사용하는 방법은 기본 생성자를 사용하는 것이다.
```kotlin
class User(var name: String, var surname: String)

val user = User("Marchin", "Moskala")
```
* 기본 생성자 방식은 매우 편리할 뿐만 아니라 대부분의 경우 가장 적합한 방식이다.
* 객체 초기 상태를 정하기 위해 인수를 전달하는 경우는 아주 흔하다.

#### 점층적 생성자 패턴(Telescoping Constructor Pattern)

```kotlin
// ❌ Bad - 생성자가 계속 늘어난다.
class Pizza {
    val size: String
    val cheese: Boolean
    val pepperoni: Boolean
    val mushroom: Boolean

    constructor(size: String) : this(size, false)

    constructor(size: String, cheese: Boolean) : this(size, cheese, false)

    constructor(size: String, cheese: Boolean, pepperoni: Boolean)
        : this(size, cheese, pepperoni, false)

    constructor(size: String, cheese: Boolean, pepperoni: Boolean, mushroom: Boolean) {
        this.size = size
        this.cheese = cheese
        this.pepperoni = pepperoni
        this.mushroom = mushroom
    }
}
```
* 위의 코드는 코틀린에서 의미가 없다. 코틀린에서는 디폴트 인수를 사용해 생성자 정의 없이도 인수의 개수를 조절할 수 있다.
```kotlin
class Pizza(
    val size: String,
    val cheese: Int = 0,
    val olives: Int = 0,
    val bacon: Int = 0
)
```
* 코틀린의 기본값을 사용하면 코드가 짧고 간결해지며, 점층적 생성자보다 더 많은 기능을 제공한다.
* 다음과 같이 매개변수를 명시하지 않고도 지정할 수 있다.
```kotlin
val myFavorite = Pizza("L", olives = 3)
```
* 또한 olives 앞뒤로 다른 이름 있는 인수를 추가할 수 있다.
```kotlin
val myFavorite = Pizza("L", olives = 3, cheese = 1)
```
* 디폴트 인수가 점층적 생성자보다 더 강력한 이유는 다음과 같다.
  * **사용하고 싶은 디폴트 인수만 지정하여 매개변수의 하위집합을 정의할 수 있다.**
  * **인수를 어떤 순서로든 제공할 수 있다.**
  * **인수의 이름을 명시적으로 지정할 수 있어 각 값의 의미를 명확하게 할 수 있다.**
* 디폴트 인수가 있는 생성자는 점층적인 생성자 패턴보다 훨씬 좋다.

#### 빌더 패턴

* 빌더 패턴을 사용하면 다음과 같은 이점이 있다.
  * **매개변수 이름 지정**
  * **매개변수를 원하는 순서로 지정**
  * **기본값 지정**

```kotlin
// ❌ Bad - 단순 객체에 자바식 빌더를 그대로 이식 — 코틀린에선 과한 보일러플레이트
class User private constructor(
    val name: String,
    val email: String,
    val age: Int,
) {
    class Builder {
        private var name: String = ""
        private var email: String = ""
        private var age: Int = 0

        fun name(name: String) = apply { this.name = name }
        fun email(email: String) = apply { this.email = email }
        fun age(age: Int) = apply { this.age = age }

        fun build() = User(name, email, age)
    }
}
```
```kotlin
// ⭕ Good
class User(
    val name: String,
    val email: String,
    val age: Int = 0,
)
```
* 빌더 패턴을 코틀린의 기본 생성자와 비교하면 빌더 대신 이름 있는 매개변수를 사용했을 때의 이점을 확인할 수 있다.
  * **이름 있는 매개변수는 짧다** - 디폴트 인수가 있는 생성자나 팩토리 메서드는 빌더 패턴보다 훨씬 쉽게 구현할 수 있다. 코드를 개발하는 개발자와 코드를 읽는 개발자 모두 시간을 절약할 수 있다.
  * **이름 있는 매개변수는 더 깔끔하다** - 객체가 어떻게 생성되는지 확인하고 싶을 때 하나의 메서드만 보면 된다. 반면에 빌더 클래스에는 여기저기 정보가 흩어져있다.
  * **이름 있는 매개변수는 사용하기가 더 쉽다** - 기본 생성자는 내장된 개념이다. 빌더 패턴은 언어에서 제공하는 것이 아니기에 이해하기가 더 어렵다.
  * **이름 있는 매개변수는 동시성 문제가 없다** - 드물게 발생하는 문제이다. 다만, 코틀린에서 함수의 매개변수는 항상 불변인 반면, 대부분의 빌더에서는 프로퍼티 변경이 가능하다. 따라서 쓰레드 안전한 빌더 패턴을 구현하는 것은 어려운 일이다.

#### Kotlin DSL

```kotlin
// ❌ Bad - 중첩 구조를 체이닝 빌더로 표현하면 어색하고 읽기 힘들다
val menu = Menu.Builder()
    .addItem(MenuItem.Builder().name("피자").price(20000).build())
    .addItem(MenuItem.Builder().name("파스타").price(15000).build())
    .build()
```
```kotlin
// ⭕ Good - DSL 빌더로 중첩 구조를 자연스럽게 표현
class Menu(val items: List<MenuItem>) {
    class Builder {
        private val items = mutableListOf<MenuItem>()
        fun item(block: MenuItem.Builder.() -> Unit) {
            items.add(MenuItem.Builder().apply(block).build())
        }
        fun build() = Menu(items.toList())
    }
}
```
* 명확하지 않은 정의를 감수하더라도 더 나은 표현을 위해 시간을 들이기로 결정했다면 DSL을 사용해 많은 유연성과 가독성을 얻을 수 있다.

> 결론 : 코틀린에서는 기본 생성자를 사용해 객체를 생성하는 것이 가장 좋은 방법이다. 코틀린에서 점층적 생성자 패턴을 더 이상 사용하지 않길 권장한다. 대부분은 이름 있는 인수를 사용하는 기본 생성자만으로 충분하고 더 복잡한 객체를 생성할 때는 DSL 빌더를 사용해 정의하는 것이 좋다.
