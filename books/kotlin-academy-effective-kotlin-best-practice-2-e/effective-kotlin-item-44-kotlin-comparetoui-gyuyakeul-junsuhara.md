# (Effective Kotlin Item 44) Kotlin - compareTo의 규약을 준수하라

#### compareTo의 규약을 준수하라

* `Any` 클래스에 `compareTo` 메서드는 없다.
* `compareTo` 메서드는 `Comparable<T>` 인터페이스에도 있다. `Comparable` 인터페이스를 구현하거나, 매개변수 하나가 있는 `compareTo` 연산자 메서드가 있다면 객체에 자연스러운 순서가 있다는 것이다. 그러한 순서는 다음과 같다.
  * 반대칭(antisymmetric), 즉 `a >= b`이고, `b >= c`인 경우 `a >= c`임을 의미한다. 마찬가지로, `a > b`이고, `b > c`이면 `a > c`이다. 전이성이 없을 때 정렬하게 되면 정렬 알고리즘이 문자 그대로 영원히 실행될 수 있다.
  * 관계 정립(connex), 즉, 모든 요소(`a >= b` 또는 `b >= a`) 사이에 관계가 있어야 함을 의미한다. 
    * 코틀린은 타입 시스템으로 순서를 보장하고 있는데, `compareTo`가 양수, 음수, 또는 0이 확실한 `Int`를 반환하기 때문이다.
    * 두 요소 사이에 관계가 없으면 퀵 정렬이나 삽입 정렬과 같은 기존 정렬 알고리즘을 사용할 수 없기 때문에 관계 정립은 중요하다.
    * 원소 사이에 관계가 없다면 위상 정렬처럼 일부 순서만 정하는 특별한 알고리즘을 사용해야 한다.

#### compareTo가 필요한가?

* 코틀린에서는 `compareTo`를 직접 구현하는 경우가 거의 없다.
* 전체 객체를 대상으로 자연스러운 순서를 정하는 것보다 경우에 따라 순서를 지정하는 것이 훨씬 편리하다.

#### compareTo 구현

* `compareTo`를 직접 구현할 때 사용할 수 있는 최상위 함수가 있다.
* `compareValues` 함수는 비교하는 값이 두 가지밖에 없을 때 사용한다.

```kotlin
class DateTime(
    private val millis: Long = 0L,
    private val timeZone: TimeZone? = null
) : Comparable<DateTime> {

    override fun compareTo(other: DateTime): Int =
        compareValues(millis, other.millis)
}
```
