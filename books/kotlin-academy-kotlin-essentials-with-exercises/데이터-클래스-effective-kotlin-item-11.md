# (Effective Kotlin Item 11) Kotlin - Data Class

#### 데이터 클래스(data class)

* 코틀린에서는 모든 클래스가 클래스 계층구조 최상위에 있는 `Any` 슈퍼 클래스를 상속한다.
* 따라서 `Any`에 정의된 메서드를 모든 객체에서 사용할 수 있다.
* 제공되는 메서드는 다음과 같다.
  * `equals` : `==`를 사용해 두 객체를 비교한다.
  * `hashCode` : 해시 테이블 기반 컬렉션에서 사용한다.
  * `toString` : 문자열 템플릿 또는 print 함수와 같이 객체를 문자열로 표현할 때 사용한다.

```kotlin
open class Any {
    open operator fun equals(other: Any?): Boolean
    open fun hashCode(): Int
    open fun toString(): String
}

class A // Any를 암묵적으로 상속받는다.
```

* `equals`, `hashCode`, `toString`의 기본적인 구현 방식은 메모리 상에서의 객체 주소에 기반한다.
* 코틀린에서는 `==`를 이용해서 두 객체의 **동등성**을 확인한다. `==`는 `Any`의 `equals` 메서드를 사용한다.

```kotlin
/**
 * Indicates whether some other object is "equal to" this one. Implementations must fulfil the following
 * requirements:
 *
 * * Reflexive: for any non-null value `x`, `x.equals(x)` should return true.
 * * Symmetric: for any non-null values `x` and `y`, `x.equals(y)` should return true if and only if `y.equals(x)` returns true.
 * * Transitive:  for any non-null values `x`, `y`, and `z`, if `x.equals(y)` returns true and `y.equals(z)` returns true, then `x.equals(z)` should return true.
 * * Consistent:  for any non-null values `x` and `y`, multiple invocations of `x.equals(y)` consistently return true or consistently return false, provided no information used in `equals` comparisons on the objects is modified.
 * * Never equal to null: for any non-null value `x`, `x.equals(null)` should return false.
 *
 * Read more about [equality](https://kotlinlang.org/docs/reference/equality.html) in Kotlin.
 */
public open operator fun equals(other: Any?): Boolean
```

#### 객체 동등성

* 코틀린에서는 `==`를 이용해서 두 객체의 동등성을 확인한다.
* `==`는 `Any`의 `equals` 메서드를 사용한다.

> * 코틀린의 `==` 연산자는 내부적으로 `equals()`를 호출하여 값의 동등성을 비교한다. 반면, 두 변수가 메모리 상에서 완전히 같은 객체를 가리키고 있는지 확인하려면 `===` 연산자를 사용해야 한다.
> * 데이터 클래스는 `equals()`를 오버라이딩하므로, 내용이 같으면 `==`는 true를 반환하지만, `copy()`로 생성한 객체와 원본을 `===`로 비교하면 false가 나온다.

#### 해시 코드

* `Any`는 객체를 `Int`로 변환하는 `hashCode` 메서드를 제공한다.
* 이 메서드 덕분에 객체 인스턴스를 HashSet이나 HashTable같은 해시 테이블 자료구조에 저장할 수 있다.
* `hashCode`의 가장 중요한 규칙은 다음과 같다.
  * **`equals`와 일관되어야 한다. 동등한 객체들은 모두 같은 `Int` 값을 반환해야 하며, 같은 객체에서는 언제 호출해도 매번 똑같은 해시 값을 반환해야 한다.**
  * **`Int` 표현 범위 내에서 객체들을 가능한 한 균등하게 배분해야 한다.**
* `hashCode`는 기본적으로 객체 메모리 주소를 반환한다.
* 하지만 `data` 제어자에 의해 생성되는 `hashCode`는 객체의 주 생성자 프로퍼티 해시 값들을 조합해 만든 해시 값을 반환한다.
* 객체의 프로퍼티 값이 모두 동일하다면 `hashCode`는 같은 값을 생성한다.

> * 데이터 클래스의 `hashCode()`는 주 생성자에 선언된 프로퍼티들의 값을 조합해서 생성된다. 만약 프로퍼티를 `var`로 선언하고 `HashSet`, `HashMap`의 키로 넣은 뒤, 그 프로퍼티의 값을 변경해버리면 해시코드가 달라진다. 결과적으로 컬렉션 안에 자신의 객체를 영영 찾을 수 없게 되어 메모리 누수 버그가 발생한다.

#### 객체 복사

* data 제어자는 copy라는 메서드도 지원한다.
* 이 메서드는 데이터 객체로부터 일부 프로퍼티 값을 변경한 새로운 인스턴스를 생성할 때 사용된다.

```kotlin
data class Player {
    val id: Int,
    val name: String,
    val points: Int
}

fun main() {
    val p = Player(0, "Gecko", 9999)
    println(p.copy())

    println(p.copy(id = 1, name = "new name"))

    println(p.copy(points = p.points + 1))
}
```

* `copy`는 객체를 얕은 복사로 생성한다. 따라서 객체 상태가 변경가능하다면 한 객체 변경 사항이 다른 모든 복사본에도 반영된다.
* 모든 프로퍼티가 읽기 전용인 `val`로 선언된 불변 클래스에서는 `copy`를 사용해도 문제가 되지 않지만 `copy`는 불변 객체 활용을 권장하기 위해 도입되었기 때문에 취지에 맞게 사용을 해야 한다.

#### 구조 분해

* 코틀린은 위치 기반 구조 분해(destructuring)라는 기능을 제공한다.
* 객체를 분해하여 그 요소들을 여러 변수에 나눠 할당하는 기능이다.

```kotlin
data class Player {
    val id: Int,
    val name: String,
    val points: Int
}

fun main() {
    val player = Player(0, "Gecko", 9999)
    val (id, name, pts) = player
    println(id)
    println(name)
    println(pts)
}
```

#### 구조 분해를 사용하는 경우와 방법

* 위치 기반 구조 분해는 장점과 단점이 있다.
* 가장 큰 장점은 다음과 같이 우리가 원하는 대로 변수 이름을 정할 수 있다는 것이다.

```kotlin
data class Player(
    val id: Int,
    val name: String,
    val points: Int
)

val trip = mapOf(
    "Spain" to "Gran Canaria",
    "Morocco" to "Taghazout",
    "India" to "Rishikesh"
)
```

* 하지만 데이터 클래스를 구성하는 요소 순서나 수가 바뀌면 모든 구조 분해를 적절히 수정해야 한다.
* 구조 분해를 사용하면 주 생성자 프로퍼티 순서가 바뀌어 에러로 이어질 가능성이 있다.

> * 데이터 클래스 구조 분해는 이름이 아니라 선언된 위치를 기반으로 동작한다.
> * 만약 `User(val name: String, val email: String)`에서 순서를 실수로 바꾸거나 사이에 다른 문자열 필드를 추가하더라도 타입이 동일하면 컴파일 에러가 발생하지 않아 런타임에 심각한 논리적 버그를 유발한다. 따라서 데이터 클래스를 구조 분해할 때는 변수의 이름을 명확히 쓰거나 2~3개 이하의 프로퍼티를 가질 때만 제한적으로 사용해야 한다.

#### 데이터 클래스의 제약

* 데이터 클래스는 데이터 묶음을 효과적으로 표현하기 위해 존재한다.
* 생성자에서 모든 데이터를 명시할 수 있으며, 구조 분해로 데이터에 접근하거나, `copy` 메서드를 이용해 다른 인스턴스로 복제할 수 있다.
* 데이터 클래스는 상속할 수 없다.

```kotlin
data class Dog(
    val name: String,
) {
    var trained = false // 데이터 클래스에서 가변 프로퍼티 사용은 지양하라.
}
```
