# (Effective Kotlin Item 42) Kotlin - equals의 규약을 준수하라

#### equals의 규약을 준수하라

* 코틀린의 모든 객체는 `Any`를 상속한다. `Any`에는 규약이 확실한 메서드들이 존재한다.

#### 동등성 - equals

* 코틀린에는 두 가지 타입의 동등성이 있다.
  * **구조적 동등성** : `equals` 메서드 또는 `==` 연산자로 확인한다. `a == b`는 a가 `null`을 허용하지 않을 때, `a.equals(b)`로 변환되고, `null`을 허용하면 `a?.equals(b) ?: (b == null)`로 변환된다.
  * **참조 동등성** : `===` 연산자로 확인한다. 객체가 동일한 경우 `true`를 반환한다.

#### 왜 equals가 필요한가?

* `equals`는 기본적으로 참조 동등성처럼 두 객체가 동일한 인스턴스인지 확인한다. 모든 객체는 기본적으로 고유한 객체라는 전제가 깔려 있기 때문이다.
* 이런 동작은 데이터를 나타내는 클래스에 적합하므로 데이터 모델 클래스나 데이터 홀더는 `data` 한정자를 주로 사용한다.
* 데이터 클래스의 동등성은 전체 프로퍼티가 아닌 일부 프로퍼티를 비교해야 할 때에도 도움이 된다.

```kotlin
data class Money(
    val amount: Long,
    val currency: Currency = Currency.KRW
) : Comparable<Money> {

    init {
        require(amount >= 0) { "금액은 음수일 수 없다: $amount" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "통화 불일치: $currency vs ${other.currency}" }
        return copy(amount = amount + other.amount)
    }

    operator fun times(qty: Int): Money = copy(amount = amount * qty)

    override fun compareTo(other: Money): Int = amount.compareTo(other.amount)

    companion object {
        val ZERO = Money(0)
        fun won(amount: Long) = Money(amount, Currency.KRW)
    }
}
```

* `copy`를 사용할 때 기본 생성자에 정의되지 않은 프로퍼티는 복사하지 않는다는 점에 유의해야 한다.
* 추가적인 프로퍼티가 객체의 핵심 기능에 영향을 미치지 않아야 `copy`가 제대로 동작할 수 있다.
* 기본 클래스와 데이터 클래스의 동등성이 각기 다른 방식으로 동작하기 때문에 코틀린에서는 동등성을 직접 구현할 필요가 거의 없다.
* 동등성을 직접 구현해야 하는 경우가 있다면 하나의 프로퍼티만 비교하는 것을 들 수 있다. 예를 들어, `User` 클래스는 `id`가 동일할 때 두 사용자가 동일하다고 가정할 수 있다.

```kotlin
@JvmInline
value class UserId(val value: Long) {
    init { require(value > 0) { "id는 양수여야 한다: $value" } }
}

class User(
    val id: UserId,
    var name: String,      // 바뀌어도 같은 사용자다
    var email: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id          // 식별자만 비교
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "User(id=${id.value}, name=$name)"
}
```

* 결론적으로 `equals`를 직접 구현하는 경우는 다음과 같다.
  * 기본 로직과 다른 로직이 필요한 경우
  * 프로퍼티의 하위집합만 비교하면 되는 경우
  * 객체를 데이터 클래스로 만들고 싶지 않거나 기본 생성자에는 없는 프로퍼티를 비교해야 한다.

#### equals의 규약

* 코틀린 라이브러리 주석에서는 `equals`를 다음과 같이 설명하고 있다.

```kotlin
public actual open class Any {
    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * Implementations must fulfil the following requirements:
     * * Reflexive: for any non-null value `x`, `x.equals(x)` should return true.
     * * Symmetric: for any non-null values `x` and `y`, `x.equals(y)` should return true if and only if `y.equals(x)` returns true.
     * * Transitive: for any non-null values `x`, `y`, and `z`, if `x.equals(y)` returns true and `y.equals(z)` returns true, then `x.equals(z)` should return true.
     * * Consistent: for any non-null values `x` and `y`, multiple invocations of `x.equals(y)` consistently return true or consistently return false, provided no information used in `equals` comparisons on the objects is modified.
     * * Never equal to null: for any non-null value `x`, `x.equals(null)` should return false.
     *
     * Read more about [equality](https://kotlinlang.org/docs/reference/equality.html) in Kotlin.
     */
    public actual open operator fun equals(other: Any?): Boolean
}
```
* `equals`의 규약
  * 재귀적(reflexive) : `null`이 아닌 값 x에 대해서 `x.equals(x)`는 `true`를 반환해야 한다.
  * 대칭적(symmetric) : `null`이 아닌 값 x와 y에 대해 `y.equals(x)`가 `true`를 반환하면 `x.equals(y)`는 `true`를 반환해야 한다.
  * 전이적(transitive) : `null`이 아닌 값 x, y, 그리고 z에 대해 `x.equals(y)`가 `true`를 반환하고 `y.equals(z)`가 `true`를 반환하면 `x.equals(z)`는 `true`를 반환해야 한다.
  * 일관적(consistent) : `null`이 아닌 값 x와 y에 대해 `x.equals(y)`를 여러 번 호출하면 일관되게 `true`나 `false`를 반환해야 한다. 단, `equals`에서 비교하는 데이터가 변경되지 않아야 한다.
  * `null`과 같지 않음 : `null`이 아닌 값 x에 대해 `x.equals(null)`은 `false`를 반환해야 한다.
* `equals`, `toString`, `hashCode` 연산은 빠르게 처리되는 것이 일반적이다. 공식 규약의 일부는 아니지만 두 요소를 비교하는 데 몇 초씩 걸리면 안 된다.

**1. 객체 동등성은 재귀적이어야 한다.**

```kotlin
class User(val id: Long?) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true          // ① 반사성 보장 + 성능 최적화
        if (other !is User) return false         // ② 타입 확인 (null도 여기서 걸러진다)
        return id != null && id == other.id      // ③ 식별자 비교
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
```
* `x.equals(y)`는 `true`를 반환한다.
* 자기 자신과 비교할 때 불일치하는 객체가 컬렉션에 있다면 `contains` 메서드를 사용하더라도 객체를 찾지 못할 수 있다. 단위 테스트(assertion)에서도 올바르게 작동하지 않는다.

**2. 결과가 일관되지 않으면 신뢰할 수 없다.**

```kotlin
data class Team(
    val name: String,
    val members: List<String>
) {
    // 방어적 복사 — 외부에서 원본 리스트를 바꿔도 영향받지 않는다
    constructor(name: String, members: Collection<String>) : this(name, members.toList())

    init {
        require(name.isNotBlank()) { "팀 이름은 비어 있을 수 없다" }
    }
}
```
* 결과가 올바른지 아니면 단지 불일치의 결과인지 판단이 불가능하다.

**3. 객체 동등성은 대칭적이어야 한다.**

```kotlin
data class Point(val x: Int, val y: Int)

data class ColorPoint(
    val point: Point,      // 상속이 아니라 포함
    val color: String
) {
    val x: Int get() = point.x
    val y: Int get() = point.y

    fun asPoint(): Point = point   // 필요하면 뷰를 제공
}
```
* 즉, `x == y`와 `y == x`의 결과는 항상 동일해야 한ㄴ다.
* 객체를 비교할 때 다른 타입의 객체도 비교할 수 있다면 이런 규칙이 위반될 가능성이 높다.
* 대칭성이 없는 상태로 객체의 순서를 다르게 비교하면 그 결과를 신뢰할 수 없게 되며, x를 y와 비교하는 것과 y를 x와 비교하는 것의 결과가 달라지게 된다. 객체 작성자는 동등성 비교가 대칭적이라 생각했기 때문에, 순서에 따라 비교 결과가 달라지는 것을 문서화하지 않았고 규약으로 명시하지도 않았다.

**4. 객체 동등성은 전이적이어야 한다.**

* 즉, `null`이 아닌 참조 값 x, y 및 z에 대해 `x.equals(y)`가 `true`를 반환하고, `y.equals(z)`가 `true`를 반환하면 `x.equals(z)`가 `true`를 반환해야 한다.
* 서브타입과 비교를 허용하여 서브타입의 프로퍼티를 확인하게 되면 전이적이어야 한다는 원칙을 위반하게 된다.

**5. 객체 동등성은 일관되어야 한다.**

* 두 객체 중 하나가 확정되지 않는 한, 두 객체에 대해 호출된 메서드는 항상 동일한 결과를 반환해야 함을 의미한다.
* 불변 객체의 경우 비교 결과가 항상 동일해야 한다. 즉, `equals()`는 순수 함수로 입력값과 리시버의 상태에 따라 결괏값을 반환하는 함수이며, 객체의 상태를 수정해선 안 된다.
* `null`이 아닌 객체는 `null`과 같아선 안 된다. `null`이 아닌 값 x에 대해 `x.equals(null)`은 `false`를 반환해야 한다. `null`은 고유하기 때문에 `null`과 동일한 다른 객체가 있어선 안 된다.

#### java.net.URL의 equals 문제

* `java.net.URL.equals`는 `equals`를 잘못 구현한 대표적인 사례이다. 두 호스트의 IP 주소로 동일한지 비교하므로 `java.net.URL` 객체 간의 동등성은 네트워크 작업에 따라 달라진다.
* `java.net.URL.equals`의 문제는 다음과 같다.
  * 일관성이 없다 : 예를 들어, 두 URL을 비교할 때, 인터넷 연결이 가능하다면 동일하다는 결과가 나오지만 불가능한 경우 결과가 달라질 수 있다. 또한 URL로 확인된 IP 주소가 나중에 바뀌게 되는 경우에도 결과가 달라지게 된다.
  * 일반적으로 `equals()`와 `hashCode()`는 빠르게 처리될 것이라 생각하지만 네트워크 호출로 인해 느릴 수 있다 : URL이 목록에 있는지 확인하는 경우 목록의 각 요소로 네트워크 호출을 하게 되어 필연적으로 속도가 느려지게 된다. 또한 안드로이드와 같은 일부 플랫폼에서는 메인 쓰레드에서의 네트워크 작업이 금지된다. 따라서 안드로이드에 목록에 URL을 추가하고 싶은 경우에도 별도의 쓰레드에서 처리해야 한다.
  * HTTP 가상 호스팅에서는 IP 주소로 비교하는 것이 일관된 결과를 가져오지 않는다 : 동일한 IP 주소가 동일한 콘텐츠를 의미하지 않는다. 가상 호스팅을 사용하면 관련 없는 사이트가 IP 주소를 공유할 수 있다. 따라서 같은 서버에서 서로 관련없는 두 개의 URL을 호스팅하게 되면 동등하다는 결과가 나오게 된다.

#### equals 구현

* 결론부터 말하자면 타당한 이유가 없다면 `equals`를 스스로 구현하는 것은 권장하지 않는다. 대신 기본 구현이나 데이터 클래스의 동등성을 사용하자.
* 사용자 정의 `equals`가 `final`인 클래스를 만들면 안 되며, 서브 클래스가 동등성 동작을 변경해선 안 된다. 상속이 가능한 클래스에서 사용자 정의 `equals`를 직접 구현하는 것은 여간 어려운 일이 아니다. 데이터 클래스가 상속 불가능하게 설계된 데에는 이런 이유가 있다.
