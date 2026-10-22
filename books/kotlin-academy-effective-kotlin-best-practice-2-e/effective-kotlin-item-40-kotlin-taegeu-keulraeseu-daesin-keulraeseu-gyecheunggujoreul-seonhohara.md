# (Effective Kotlin Item 40) Kotlin - 태그 클래스 대신 클래스 계층구조를 선호하라

#### 태그 클래스 대신 클래스 계층구조를 선호하라

* 클래스의 동작 방식을 지정하는 상수 모드가 있는 클래스를 쉽게 찾아볼 수 있다.
* 이러한 클래스에는 작동 모드를 지정하는 태그를 포함하고 있어 태그 클래스라고 부른다. 하지만 태그 클래스의 문제점은 모드에 따른 동작이 서로 다름에도 불구하고 동일한 클래스가 모든 것을 책임진다는 것이다.

```kotlin
class Figure {
    enum class Shape { RECTANGLE, CIRCLE }

    // 태그 필드 — 현재 모양을 나타낸다
    private val shape: Shape

    // RECTANGLE일 때만 쓰이는 필드
    private var length: Double = 0.0
    private var width: Double = 0.0

    // CIRCLE일 때만 쓰이는 필드
    private var radius: Double = 0.0

    constructor(radius: Double) {
        this.shape = Shape.CIRCLE
        this.radius = radius
    }

    constructor(length: Double, width: Double) {
        this.shape = Shape.RECTANGLE
        this.length = length
        this.width = width
    }

    fun area(): Double = when (shape) {
        Shape.RECTANGLE -> length * width
        Shape.CIRCLE -> PI * radius * radius
    }
}
```
* 태그 클래스에는 여러 가지 단점이 있다.
  * 단일 클래스에서 여러 모드를 처리하기에 보일러플레이트 코드가 추가된다.
  * 모드에 따라 사용하지 않는 프로퍼티들이 있게 된다. 모든 모드를 처리하기 때문에 태그 클래스에는 각각의 모드를 처리할 때 필요한 것보다 더 많은 프로퍼티를 포함한다. 특정 모드에서 사용하지 않는 프로퍼티가 다른 모드에서 필요할 수 있기 때문이다. 위의 예시를 보면 RECTANGLE일 때만 쓰이는 필드가 있고 CIRCLE일 때만 쓰이는 필드가 있다.
  * 한 가지 요소가 여러 목적을 가지고 있고 다양한 방법으로 정의된다면 상태의 일관성 및 정확성이 보장되지 않는다.
  * 팩토리 메서드를 사용해야 하는 경우가 종종 있다. 그렇지 않으면 객체가 올바르게 생성되었는지 확인하기 어렵다.
* 이런 태그 클래스 대신 코틀린에는 `sealed` 클래스라는 더 나은 대안이 있다. 단일 클래스에 여러 모드를 축적하는 대신 각각의 모드에 해당하는 클래스를 별도로 정의하고 다형성 사용을 허용하는 타입 시스템을 사용해야 한다. 그리고 `sealed` 한정자를 추가하고 클래스들을 한 곳에 모은다.
* `sealed` 클래스를 사용해 리팩토링하면 다음과 같아진다.

```kotlin
package com.example.geometry

import kotlin.math.PI

sealed class Figure(val name: String) {          // 공통 상태를 상위가 소유
    abstract val area: Double
    abstract val perimeter: Double

    // 모든 하위 타입이 공유하는 구현 — 한 곳에만 존재
    override fun toString(): String =
        "%s(area=%.2f, perimeter=%.2f)".format(name, area, perimeter)
}

class Circle(val radius: Double) : Figure("Circle") {
    init { require(radius > 0) { "radius must be positive: $radius" } }

    override val area: Double get() = PI * radius * radius
    override val perimeter: Double get() = 2 * PI * radius
}

open class Rectangle(val length: Double, val width: Double) : Figure("Rectangle") {
    init { require(length > 0 && width > 0) { "sides must be positive" } }

    override val area: Double get() = length * width
    override val perimeter: Double get() = 2 * (length + width)
}

// 계층구조의 이점 — 태그 클래스로는 표현 자체가 불가능했던 관계
class Square(val side: Double) : Rectangle(side, side)
```
* 위와 같이 각각의 클래스가 책임지는 부분이 다르기에 훨씬 더 깔끔해진다.
* 각 객체에는 필수 데이터만 있으며, 예상되는 매개변수를 정의할 수 있게 된다.

#### 태그 클래스는 상태 패턴을 사용하는 클래스와 동일하지 않다

* 태그 클래스와 혼동되는 인기 있는 패턴이 있는데 바로 상태 패턴(state pattern)이다.
* 상태 패턴은 객체 내부의 상태가 변경될 때, 객체의 동작을 변경할 수 있도록 하는 행동 소프트웨어 디자인 패턴이다.
* 이 패턴은 프론트엔드 컨트롤러, 프레젠터 또는 뷰 모델에서 자주 사용된다.

```kotlin
package com.example.order

// ── State: 상태별 행동과 전이를 함께 정의
sealed interface OrderState {
    val label: String

    // 기본 구현 = 허용되지 않는 전이. 각 상태는 '가능한 것'만 오버라이드한다
    fun pay(order: Order): OrderState = deny("결제")
    fun ship(order: Order, trackingNo: String): OrderState = deny("배송 시작")
    fun deliver(order: Order): OrderState = deny("배송 완료")
    fun cancel(order: Order, reason: String): OrderState = deny("취소")

    private fun deny(action: String): Nothing =
        throw IllegalStateException("'$label' 상태에서는 $action 할 수 없다")
}

data object Pending : OrderState {
    override val label = "결제대기"
    override fun pay(order: Order): OrderState {
        order.recordPayment()
        return Paid
    }
    override fun cancel(order: Order, reason: String): OrderState = Cancelled(reason)
}

data object Paid : OrderState {
    override val label = "결제완료"
    override fun ship(order: Order, trackingNo: String): OrderState = Shipped(trackingNo)
    override fun cancel(order: Order, reason: String): OrderState {
        order.refund()                       // 결제 후 취소만 환불이 필요하다
        return Cancelled(reason)
    }
}

data class Shipped(val trackingNo: String) : OrderState {
    override val label = "배송중"
    override fun deliver(order: Order): OrderState = Delivered
}

// 종착 상태 — 아무것도 오버라이드하지 않는다. 모든 전이가 자동으로 거부된다
data object Delivered : OrderState { override val label = "배송완료" }
data class Cancelled(val reason: String) : OrderState { override val label = "취소됨" }
```
* 상태 패턴 클래스는 변경 가능한 상태를 가지고 있으며, 다른 클래스에도 상태를 유지하고 있다는 것이 가장 큰 차이점이다.
* 상태 패턴을 태그 클래스로 구현할 수도 있지만, `sealed` 클래스를 사용하는 게 더 낫다.
* 결론적으로 상태를 가진 클래스는 더 많은 역할이 부여되며, 상태에 따라 클래스의 동작 방식에 달라진다.
