# (Effective Kotlin Item 41) Kotlin - 열거형 클래스를 사용해서 값 목록을 나타내라

#### 열거형 클래스를 사용해서 값 목록을 나타내라

* 열거형 클래스의 원소들은 각각 열거형 클래스의 인스턴스이며, 원소별로 특정 값을 가질 수 있다.
* 원소가 같은 종류라면 가지고 있는 값은 항상 같으며, 다른 종류라면 값이 다를 수 있다.

```kotlin
enum class Operation(val symbol: String) {
    PLUS("+")   { override fun apply(x: Double, y: Double) = x + y },
    MINUS("-")  { override fun apply(x: Double, y: Double) = x - y },
    TIMES("*")  { override fun apply(x: Double, y: Double) = x * y },
    DIVIDE("/") {
        override fun apply(x: Double, y: Double): Double {
            require(y != 0.0) { "0으로 나눌 수 없다" }
            return x / y
        }
    };

    abstract fun apply(x: Double, y: Double): Double   // 각 상수가 반드시 구현

    override fun toString(): String = symbol

    companion object {
        private val BY_SYMBOL: Map<String, Operation> = entries.associateBy { it.symbol }
        fun fromSymbol(symbol: String): Operation? = BY_SYMBOL[symbol]
    }
}
```

* 열거형 클래스에서 가변 변수를 정의하면 안 된다. 열거형 클래스의 멤버 변수는 정적으로 관리되기 때문에 가변 변수로 정의하면 전역적인 정적 가변 상태가 되기 때문이다.
* 하지만 멤버 변수를 정의하면 열거형 클래스의 각 원소에 고정된 값을 할당할 수 있다. 기본 생성자를 사용해 고정된 값을 할당하는 방식으로 멤버 변수를 사용하는 것은 유용할 수 있다.

```kotlin
enum class Planet(
    val mass: Double,      // 단위: kg
    val radius: Double     // 단위: m
) {
    MERCURY(3.302e+23, 2.439e6),
    VENUS  (4.869e+24, 6.052e6),
    EARTH  (5.975e+24, 6.378e6),
    MARS   (6.419e+23, 3.393e6),
    JUPITER(1.899e+27, 7.149e7),
    SATURN (5.685e+26, 6.027e7),
    URANUS (8.683e+25, 2.556e7),
    NEPTUNE(1.024e+26, 2.477e7);

    // 생성자 인자로부터 파생된 값 — 상수당 한 번만 계산된다
    val surfaceGravity: Double = G * mass / (radius * radius)

    fun surfaceWeight(otherMass: Double): Double = otherMass * surfaceGravity

    companion object {
        private const val G = 6.673e-11    // 반드시 const
    }
}
```
* 코틀린의 열거형 클래스에서는 하나의 메서드를 원소별로 다르게 구현할 수 있다.
* 다음과 같이 열거형 클래스에서 추상 메서드를 정의하고, 각 원소에서 추상 메서드를 오버라이드해서 구현하면 된다.

```kotlin
enum class Operation(val symbol: String) {

    PLUS("+") {
        override fun apply(x: Double, y: Double): Double = x + y
    },
    MINUS("-") {
        override fun apply(x: Double, y: Double): Double = x - y
    },
    TIMES("*") {
        override fun apply(x: Double, y: Double): Double = x * y
    },
    DIVIDE("/") {
        override fun apply(x: Double, y: Double): Double {
            require(y != 0.0) { "0으로 나눌 수 없다" }
            return x / y
        }
    };                              // ← 상수 목록의 끝. 세미콜론 필수

    abstract fun apply(x: Double, y: Double): Double

    override fun toString(): String = symbol
}
```

* 하지만 기본 생성자 매개변수로 함수형 타입을 받는 것이 더 나은 방법이다.

```kotlin
enum class Operation(
    val symbol: String,
    private val operate: (Double, Double) -> Double   // private가 핵심
) {
    PLUS  ("+", { x, y -> x + y }),
    MINUS ("-", { x, y -> x - y }),
    TIMES ("*", { x, y -> x * y }),
    DIVIDE("/", { x, y ->
        require(y != 0.0) { "0으로 나눌 수 없다" }      // 여러 줄도 가능
        x / y
    });

    fun apply(x: Double, y: Double): Double = operate(x, y)

    override fun toString(): String = symbol
}
```
* 확장 함수를 정의하는 것이 더 편리한 방법일 수 있다.
* `when` 표현식에서 열거형 값을 전부 나열하면 `else`를 추가할 필요가 없다.
```kotlin
enum class OrderStatus { PENDING, PAID, SHIPPED, DELIVERED, CANCELLED }

fun badgeColor(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING   -> "#9E9E9E"
    OrderStatus.PAID      -> "#2196F3"
    OrderStatus.SHIPPED   -> "#FF9800"
    OrderStatus.DELIVERED -> "#4CAF50"
    OrderStatus.CANCELLED -> "#F44336"
}   // else 없음 — 다섯 개를 모두 덮었으므로 컴파일된다
```
* 열거형 클래스의 각 요소들이 나타내는 바는 분명하며, 일관성을 유지하는 것이 장점이라 할 수 있다.
* 열거형 클래스는 동반 객체의 `values()` 함수 또는 `enumValueOf()`라는 최상위 함수로 모든 항목을 가져올 수 있다.
* 또한 동반 객체의 `valueOf(String)`나 최상위 함수인 `enumValueOf(String)`을 통해서 `String`으로부터 enum 값을 읽을 수 있다. 모든 열거형 클래스는 `Enum<T>`의 하위 타입이다.
* 열거형 클래스의 원소들을 순회하는 것은 쉽다. 모든 열거형 클래스는 `ordinal` 프로퍼티를 가지고 있으며, `Comparable` 인터페이스를 구현한다.
* 또한 열거형 클래스는 `toString`, `hashCode`, `equals`를 자동 구현한다. 열거형 클래스의 직렬화와 역직렬화는 간단하고 효율적이며, 대부분의 직렬화 라이브러리가 이를 지원한다.

#### 열거형 또는 sealed 클래스

* 제한된 계층구조를 표현하기 위해 `sealed` 클래스와 `sealed` 인터페이스를 사용해 값의 집합을 나타낼 수 있지만, `sealed` 클래스와 `sealed` 인터페이스에서 값의 집합을 나타내기 위해서 모든 서브 클래스가 `object`로 선언되어야 한다.
* 값의 집합을 표현할 때는 enum을 사용하는 것이 좋다. `sealed` 클래스는 자동으로 직렬화하거나 역직렬화할 수 없고, 순회하기 쉽지 않으며(리플렉션을 사용하면 가능하지만) 순서도 없다.
* 하지만 `sealed` 클래스의 서브 클래스를 `object`로 선언해야 하는 경우도 있다. 클래스는 값을 가질 수 있으므로 `object`를 클래스로 바꾸는 경우도 있을 수 있다. 이런 가능성이 조금이라도 존재한다면 `sealed` 클래스를 사용해야 한다.
* 메시지가 데이터를 포함해야 하는 경우라면 열거형 클래스가 아닌 일반적인 클래스를 사용해야 한다.
