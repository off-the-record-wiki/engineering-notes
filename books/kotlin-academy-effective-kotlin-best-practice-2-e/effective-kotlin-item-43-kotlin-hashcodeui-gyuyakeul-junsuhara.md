# (Effective Kotlin Item 43) Kotlin - hashCode의 규약을 준수하라

#### hashCode의 규약을 준수하라

* `hashCode` 함수는 해시 테이블이라는 데이터 구조에서 사용된다.
* 해시 테이블은 컬렉션이나 알고리즘 등 곳곳에서 사용된다.

#### 해시 테이블

* 배열이나 링크된 요소들에 기반한 컬렉션은 요소의 존재 유무를 판단하는데 오랜 시간이 걸린다.
* 리스트에 있는 모든 요소를 해당 요소와 차례대로 비교해야 하기 때문이다.
* 수백만 개의 텍스트로 구성된 배열이 있고, 이 중 특정 텍스트가 있는지 확인해야 한다고 가정하자. 확인하고 싶은 텍스트를 수백만 개의 모든 텍스트와 일일이 비교하는 데 `O(N)`의 시간복잡도가 나오게 된다.
* 이런 경우 해시 테이블이 사용된다. 각각의 요소와 숫자를 할당할 수 있는 함수만 있으면 된다. 해시 함수는 다음과 같은 특징을 추가로 가지면 좋다.
  * 빠르다.
  * 서로 다른 요소라면 서로 다른 값을 반환한다.(또는 적어도 충돌을 최소화할만큼의 충분히 다양한 값을 반환한다.)
* 해시 함수는 요소 각각에 숫자를 할당하고 이를 기반으로 여러 버킷으로 분류한다. 또한 해시 함수의 필수 조건에 따라 같은 요소는 항상 같은 버킷에 배치된다.
* 버킷들은 해시 테이블이라는 구조에 저장되며, 해시 테이블은 버킷의 수만큼 원소를 가진 배열이다. 요소가 추가되면 해시 함수를 사용해서 배치될 위치를 계산한 후 해당 위치에 추가한다.
* 해시 값을 계산하는 과정은 아주 빠르며, 계산된 해시 값을 배열의 인덱스로 사용해 버킷을 찾으면 되므로 원소를 추가하는 과정 또한 아주 빠르다.
* 요소를 찾을 때도 같은 방식으로 버킷을 찾은 다음, 해당 버킷 내에 같은 요소가 있는지만 확인하면 된다.
* 해시 함수는 같은 요소라면 동일한 값을 반환하기에 다른 버킷을 확인할 필요가 없다.

![](https://raw.githubusercontent.com/wiki/off-the-record-wiki/engineering-notes/images/attachments/85f8540b-edf2-41ff-a478-ef7a000a68b3.png)

#### 가변성과 관련된 문제

* 해시는 요소가 추가될 때만 계산한다. 컬렉션에 포함된 요소가 변경되더라도 변경된 요소는 다른 버킷으로 이동되지 않는다.
* 따라서 요소가 변경되었을 경우 잘못된 버킷에 있을 수 있다. 따라서 `LinkedHashSet`와 `LinkedHashMap`에 포함된 객체가 변경되면 기능이 제대로 작동하지 않는다.
* 가변 객체는 해시 기반 데이터 구조나 객체의 프로퍼티 값을 기반으로 요소를 구성하는 데이터 구조에서는 사용하면 안 된다. 세트나 맵의 키로 가변 요소를 사용하지 않아야 하며, 사용하더라도 이미 컬렉션에 포함되어 있는 요소를 변경해선 안 된다. 따라서 해시 테이블을 사용하는 컬렉션에서는 불변 객체를 사용해야 한다.

#### 해시 코드 규약

```kotlin
public actual open class Any {

    /**
     * Returns a hash code value for the object.
     *
     * The general contract of `hashCode` is:
     * * Whenever it is invoked on the same object more than once, the `hashCode` method must consistently return the same integer, provided no information used in `equals` comparisons on the object is modified.
     * * If two objects are equal according to the `equals()` method, then calling the `hashCode` method on each of the two objects must produce the same integer result.
     */
    public actual open fun hashCode(): Int
}
```
* `equals` 비교에 사용되는 정보가 수정되지 않는 한, 같은 객체에 대해 여러 번 `hashCode` 메서드를 호출하면 일관되게 같은 정수를 반환해야 한다.
  * `hashCode`가 일관성이 있어야 함을 의미한다.
* `equals` 메서드로 두 객체가 같다고 판단되면 두 객체의 각각에서 `hashCode` 메서드를 호출할 경우 같은 정수가 반환되어야 한다.
  * `hashCode`는 항상 `equals`와 일관성을 가져야 하며, 같은 요소들은 같은 해시 코드를 가져야 한다는 의미이다.
  * 이 규칙을 위반하면 내부적으로 해시 테이블을 사용하는 컬렉션에서 요소를 찾을 수 없게 된다.
* 따라서, 코틀린에서 `equals`를 오버라이드하면 `hashCode`도 오버라이드해야 한다.

#### 해시 코드 구현

* 코틀린에서 `hashCode`를 정의하는 경우는 `equals`를 커스텀하게 정의할 때뿐이다.
* `data` 한정자를 사용하면 `equals`와 이에 상응하는 `hashCode`가 생성된다.
* `equals` 메서드를 재정의하지 않았다면, 특별한 이유가 없는 이상 `hashCode`를 재정의하지 않는 것을 권장한다.

```kotlin
class DateTime(
    /** The millis from 1970-01-01T00:00:00Z */
    private var millis: Long = 0L,
    private var timeZone: TimeZone? = null
) {
    private var asStringCache = ""
    private var changed = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DateTime) return false
        return millis == other.millis &&
               timeZone == other.timeZone
    }

    override fun hashCode(): Int {
        var result = millis.hashCode()
        result = 31 * result + (timeZone?.hashCode() ?: 0)
        return result
    }
}
```
* 코틀린/JVM에서 유용한 함수 중 하나는 `Objects.hash` 함수로 같은 알고리즘으로 여러 객체의 해시를 계산한다.

```kotlin
override fun hashCode(): Int = Objects.hash(firstName, lastName, age)
```
* 코틀린 표준 라이브러리에는 이런 함수가 따로 없지만, 다른 플랫폼에서 필요한 경우 다음과 같은 함수를 직접 구현할 수 있다.
```kotlin
inline fun hashCodeOf(vararg values: Any?): Int =
    values.fold(0) { acc, value ->
        (acc * 31) + value.hashCode()
    }
```
* 표준 라이브러리에 이런 함수가 없는 이유는 `hashCode`를 직접 구현할 일이 거의 없기 때문이다.
* `hashCode`를 구현할 때 가장 중요한 규칙인 `equals`와 일관성을 유지해야 하며, 동일한 요소는 항상 같은 값을 반환해야 한다는 것이다.
