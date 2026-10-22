# (Effective Kotlin Item 37) Kotlin - 데이터 묶음을 표현할 때 data 한정자를 사용하라

#### 데이터 묶음을 표현할 때 data 한정자를 사용하라

* **서비스/컨트롤러/리포지토리와 같은 활성 객체** : 이런 객체들은 `Any`의 기본 동작으로 충분하기 때문에 `Any`의 메서드를 오버라이드할 필요가 없다. 두 객체가 우연히 동일한 상태를 가지더라도 각 객체의 상태는 각각 독립적으로 변경된 것이므로 활성 객체는 각기 유일한 객체로 간주된다. 따라서 `equals()`와 `hashCode()` 메서드를 오버라이드할 필요가 전혀 없다. 객체의 내부 상태를 무방비하게 노출하면 안되므로 `toString()`도 오버라이드할 필요가 없다.
* **데이터 모델 클래스 객체** : 데이터 모델 객체의 경우 `data` 한정자를 사용한다. `data` 한정자는 `toString()`, `equals()`, `hashCode()` 메서드를 오버라이드한다. 따라서 두 객체가 같은 데이터를 가졌다면 동일한 객체로 판단한다. 그리고 `toString()` 메서드로 클래스의 이름, 기본 생성자에 포함된 모든 프로퍼티의 이름과 값을 표시한다. 또한 `hashCode()`와 `equals()` 메서드와 일관되게 동작한다. `data` 한정자는 또한 `copy()`와 `componentN()` 메서드를 구현하여 이러한 객체의 수정과 구조 분해를 편리하게 만들어준다.

#### data 한정자가 오버라이드하는 메서드

* `data` 한정자를 사용하면 다음과 같은 메서드가 생성된다.
  * **toString**
  * **equals와 hashCode**
  * **copy**
  * **componentN(component1, component2 등)**
* `toString()` : 클래스의 이름과 기본 생성자의 모든 프로퍼티의 값과 이름을 표시한다. 디버깅할 때 유용하게 사용한다.
* `equals()` : 기본 생성자의 모든 프로퍼티가 같은지 확인한다.
* `hashCode()` : `equals()`와 일관되게 동작한다.
* `copy()` : 불변인 데이터 클래스에서 유용하다. 기본적으로 기본 생성자의 프로퍼티 값이 동일한 객체를 생성하는데 이름 있는 인수를 사용하여 일부 프로퍼티의 값이 변경된 객체를 생성할 수 있다. `copy()` 메서드는 얕은 복사를 수행하지만 객체가 불변일 경우 깊은 복사를 할 필요가 없으므로 문제가 되지 않는다.
* `componentN(component1, component2)` : 위치에 따른 위치 기반 구조 분해를 가능하게 한다.

#### 구조 분해는 언제, 어떻게 사용해야 할까?

* 코틀린이 제공하는 위치 기반 프로퍼티 구조 분해는 장단점이 있다.
* 가장 큰 장점은 원하는 방식으로 변수의 이름을 지정할 수 있다는 것이다.
* `componentN` 함수를 사용하면 구조 분해를 할 수 있다.

```kotlin
data class User(val name: String, val age: Int)

val user = User("철수", 20)
val (a, b) = user
```
* 구조 분해를 사용할 때는 신중해야 한다. 데이터 클래스의 기본 생성자 프로퍼티와 같은 이름을 할당하는 것이 좋다.
```kotlin
// ❌ Bad
users.map { (name, age) -> name.uppercase() }
```

#### 튜플 대신 데이터 클래스 사용하기

* 데이터 클래스는 일반적으로 튜플보다 더 많은 것을 제공한다.
* 역사적으로 코틀린에서 데이터 클래스는 튜플을 대체하는 더 나은 방법으로 여겨졌다.
* 코틀린이 제공하는 튜플은 `Pair`, `Triple`밖에 없으며, 이들 또한 데이터 클래스로 구현되었다.

```kotlin
public data class Pair<out A, out B>(
    public val first: A,
    public val second: B
) : Serializable {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    public override fun toString(): String = "($first, $second)"
}
```
```kotlin
public data class Triple<out A, out B, out C>(
    public val first: A,
    public val second: B,
    public val third: C
) : Serializable {

    /**
     * Returns string representation of the [Triple] including its [first], [second] and [third] values.
     */
    public override fun toString(): String = "($first, $second, $third)"
}
```
* 값의 순서가 분명하지 않기 때문에 안전성과 가독성을 높이려면 다음과 같이 데이터 클래스를 사용하는 것을 적극 권장한다.
```kotlin
// ⭕ Good - 의미를 명확히 드러낸다.
data class FullName(
    val firstName: String,
    val lastName: String
)
```
* 비용을 거의 들이지 않으면서도 함수를 다음과 같은 면에서 크게 향상시킬 수 있었다.
  * 함수의 반환 타입이 더 명확해진다.
  * 반환 타입이 더 짧아지고 전달하기 쉬워진다.
  * 사용자가 데이터 클래스에서 사용된 이름과 같은 이름으로 구조 분해할 때, 순서가 잘못되었다면 경고가 표시될 것이다.
* 데이터 클래스가 더 좁은 스코프를 갖게 하고 싶다면 가시성을 제한할 수 있다.
* 코틀린에서 클래스는 비용이 저렴한 편이므로 데이터 클래스 사용을 주저하지 말자.
