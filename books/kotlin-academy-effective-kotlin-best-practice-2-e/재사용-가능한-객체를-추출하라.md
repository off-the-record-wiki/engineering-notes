# (Effective Kotlin Item 50) Kotlin - 재사용 가능한 객체를 추출하라

#### 재사용 가능한 객체를 추출하라

* 무거운 연산을 외부 스코프로 옮겨 재사용하는 방법은 성능 최적화를 위해 아주 효과적인 방법이다.
* 예를 들어, 다음 예제는 최댓값과 동일한 값의 개수를 계산하는 함수이다.

```kotlin
fun <T: Comparable<T>> Iterable<T>.countMax(): Int =
    count { it == this.MaxOrNull() }
```

* 반복할 때마다 리시버의 가장 큰 요소를 찾지 않아도 되기 때문에 성능이 향상될 수 있다.
* 확장 리시버(`this`, 즉 `Iterable<T>`를 가리킨다.)가 `maxOrNull`을 호출하고 있으므로 반복할 때도 `max`값은 항상 동일하다는 점이 강조되어 가독성 또한 높아지게 된다.
* 불필요한 계산 과정이 없어서 외부 스코프로 값을 추출하는 것 또한 중요한 최적화 방법이다. 다음 예시로 문자열에 유효한 IP 주소가 있는지 확인하기 위해 정규표현식이 포함된 함수를 예시로 들 수 있다.

```kotlin
// ❌ Bad
fun isIpv4(value: String): Boolean =
    Regex("""(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(?:\.(?:...)){3}""").matches(value)

fun isIpv4(value: String): Boolean {
    val regex = Regex(IPV4)
    return regex.matches(value)
}
```

* 위의 함수는 호출될 때마다 `Regex` 객체를 새로 생성하고 있다.
* 정규식 패턴 객체를 생성하는 것은 아주 복잡한 연산이므로 부하를 줄 수 있는 연산이다.
* 이런 이유로, 이 함수는 성능이 제한된 환경에서 반복적으로 사용하기에 적합한 형태는 아니다. 이런 것을 최상위 수준에서 정규식으로 정의하면 성능 문제를 해결할 수 있다.

```kotlin
// ⭕ Good
private const val OCTET = """(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)"""
private const val IPV4 = """$OCTET(?:\.$OCTET){3}"""

private val IPV4_EXACT = Regex(IPV4)
private val IPV4_EMBEDDED = Regex("""(?<![\w.])$IPV4(?![\w.])""")

/** 문자열 전체가 IPv4 주소인지 */
fun isIpv4(value: String): Boolean = IPV4_EXACT.matches(value)

/** 문자열 안에 IPv4 주소가 포함돼 있는지 */
fun containsIpv4(text: String): Boolean = IPV4_EMBEDDED.containsMatchIn(text)

/** 문자열 안의 IPv4 주소를 모두 추출 */
fun findIpv4(text: String): List<String> =
    IPV4_EMBEDDED.findAll(text).map { it.value }.toList()
```
* 정규식을 사용하는 함수와 정규식 패턴 객체, 정규식을 사용하지 않는 다른 함수가 같은 파일에 있으며, 정규식 패턴 객체를 필요한 순간에 생성하고 싶다면 초기화는 지연시키면 된다.
* 클래스에서도 프로퍼티를 `lazy`로 지정하는 경우가 많다.

```kotlin
private const val OCTET = """(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)"""
private const val IPV4 = """$OCTET(?:\.$OCTET){3}"""

private val IPV4_EXACT: Regex by lazy { Regex(IPV4) }
private val IPV4_EMBEDDED: Regex by lazy { Regex("""(?<![\w.])$IPV4(?![\w.])""") }

fun isIpv4(value: String): Boolean = IPV4_EXACT.matches(value)

fun containsIpv4(text: String): Boolean = IPV4_EMBEDDED.containsMatchIn(text)

// 정규식과 무관한 함수 — 호출해도 위 패턴은 컴파일되지 않는다
fun defaultPort(scheme: String): Int = when (scheme.lowercase()) {
    "http" -> 80
    "https" -> 443
    else -> throw IllegalArgumentException("unknown scheme: $scheme")
}
```

#### 지연 초기화

* 무거운 클래스라면 생성을 지연하는 것이 좋다.
* 예를 들어, 클래스 A에 포함되어 있는 B, C, D가 모두 무거운 인스턴스라고 가정하자. A 클래스는 생성하면 B, C, D가 먼저 생성되어야 하기 때문에 아주 오랜 시간이 걸릴 것이다.
* 무거운 객체들의 초기화를 지연시키면 성능 향상을 시킬 수 있다.

```kotlin
class A {
    val b by lazy { B() }
    val c by lazy { C() }
    val d by lazy { D() }
}
```
* `lazy`가 지정된 객체는 필요한 순간에 초기화된다. 따라서 객체를 한꺼번에 생성하지 않게 되어, 객체 생성에 드는 부하가 분산되게 된다.
