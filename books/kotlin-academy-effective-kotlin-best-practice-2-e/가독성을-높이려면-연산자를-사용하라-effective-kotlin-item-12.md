# (Effective Kotlin Item 12) Kotlin - 가독성을 높이려면 연산자를 사용하라

#### 가독성을 높이려면 연산자를 사용하라

* 연산자를 사용하면 일반 숫자와 유사하게 `BigDecimal`과 `BigInteger`에 대해 작업을 수행할 수 있다.
```kotlin
val netPrice = BigDecimal("10")
val tax = BigDecimal("0.23")
val currentBalance = BigDecimal("20")
val newBalance = currentBalance - netPrice * tax
println(newBalance)
```
* 시간에 기간을 추가할수도 있다.
```kotlin
val now = ZonedDateTime.now()
val duration = Duration.ofDays(1)
val sameTimeTomorrow = now + duration
```
* 명시적인 메서드를 사용하면 다음과 같다.
```kotlin
val newBalance =  currentBalance.minus(netPrice.times(tax))
val sameTimeTomorrow = now.plus(duration)
```
* 모든 `Comparable` 클래스는 비교 연산자 또는 범위 확인을 사용하여 비교할 수 있다.
* 이런 타입을 처리하고 비교하는 경우가 많기 때문에 연산자는 아주 중요하다.
* 경우에 따라서 연산자를 쓰는 것이 도움이 될 때가 있고, 명시적으로 작성하는 것이 도움이 될 때가 있으니 무조건 연산자를 사용하는 것으로 오인하지 않도록 주의한다.

```kotlin
// ❌ Bad
val result = getProduct()
if (result != null && result.price < 5000) {
    return result
} else {
    return null
}
```
```kotlin
// ⭕ Good
return getProduct()?.takeIf { it.price < 5000 }
```
