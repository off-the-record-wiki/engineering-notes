# (Effective Kotlin Item 46) Kotlin - 멤버 확장 함수를 피하라

#### 멤버 확장 함수를 피하라

* 클래스 안에 확장 함수를 정의하지 말라. 확장으로 뺄 것이라면 클래스 밖으로 완전히 빼야 한다.

#### 멤버 확장 함수란 무엇인가?

* 확장 함수는 보통 파일 최상단, 즉 톱레벨에 선언한다.

```kotlin
// 톱레벨 확장 함수
fun String.isPhoneNumber(): Boolean =
    length == 7 && all { it.isDigit() }
```

* 그런데 코틀린은 이 확장 함수를 클래스 본문 안에 선언하는 것도 허용한다. 이것을 멤버 확장 함수라고 부른다.

```kotlin
class PhoneBook {
    // 멤버 확장 함수 — 피해야 할 형태
    fun String.isPhoneNumber(): Boolean =
        length == 7 && all { it.isDigit() }
}
```
* 이 함수는 리시버를 두 개 갖는다.

| 종류 | 정체 | 예시 |
|---|---|---|
| 디스패치 리시버 (dispatch receiver) | 함수가 선언된 클래스의 인스턴스 | `PhoneBook` |
| 확장 리시버 (extension receiver) | 확장 대상 타입의 인스턴스 | `String` |

* 리시버가 두 개라는 사실이 이 아이템의 모든 문제의 원인이다. 호출하려면 두 인스턴스가 **동시에** 스코프에 있어야 한다.

```kotlin
"01012345678".isPhoneNumber()   // 컴파일 에러 — PhoneBook 인스턴스가 없다
 
PhoneBook().apply {
    "01012345678".isPhoneNumber()   // 이렇게만 호출 가능
}
```

#### 왜 이런 코드를 쓰게 되는가?

* 대부분 **가시성을 제한하려는 의도**에서 출발한다. 스프링 프로젝트에서 흔히 나오는 형태는 다음과 같다.

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
) {
    fun place(command: PlaceOrderCommand): Order =
        orderRepository.save(command.toEntity())
 
    // "이 서비스 안에서만 쓰는 변환이니 여기 두자"
    private fun PlaceOrderCommand.toEntity() = Order(
        memberId = memberId,
        totalAmount = totalAmount,
        status = OrderStatus.CREATED,
    )
}
```

* 의도 자체는 타당하다. `toEntity()`가 프로젝트 전역의 자동완성에 뜨는 것을 원하지 않았을 것이다.
* 문제는 **이것이 그 목적을 위한 올바른 도구가 아니라는 점**이다. 가시성을 제한하고 싶다면 아래처럼 같은 파일의 private 톱레벨 확장으로 두면 된다.

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
) {
    fun place(command: PlaceOrderCommand): Order =
        orderRepository.save(command.toEntity())
}
 
// 같은 파일 안에서만 보인다
private fun PlaceOrderCommand.toEntity() = Order(
    memberId = memberId,
    totalAmount = totalAmount,
    status = OrderStatus.CREATED,
)
```
* 동일한 가시성 제한을 얻으면서, 아래에 설명할 부작용은 전부 사라진다.

#### 무엇이 문제인가?

**3.1 함수 참조를 만들 수 없다**

* 톱레벨 확장 함수는 참조가 가능하다.

```kotlin
fun String.isPhoneNumber(): Boolean = length == 7 && all { it.isDigit() }
 
val ref = String::isPhoneNumber                  // 정상
val bound = "01012345678"::isPhoneNumber         // 정상
val filtered = numbers.filter(String::isPhoneNumber)   // 정상
```

* 멤버 확장 함수는 리시버가 두 개이므로 참조 문법 자체가 존재하지 않는다.

```kotlin
class PhoneBook {
    fun String.isPhoneNumber(): Boolean = length == 7 && all { it.isDigit() }
}
 
val ref = PhoneBook::isPhoneNumber   // 컴파일 에러
```

* `filter`, `map`, `sortedBy` 같은 고차 함수에 넘길 수 없다는 뜻이다. 코틀린을 쓰는 이유 중 하나를 스스로 포기하는 셈이다.

**3.2 두 리시버의 이름이 충돌한다**

* 가장 위험한 부분이다. 다음 코드의 출력을 예측해 보자.
 
```kotlin
class A {
    val a = 10
}
 
class B {
    val a = 20
    val b = 30
 
    fun A.test(): Int = a + b
}
 
fun main() {
    with(B()) {
        println(A().test())
    }
}
```
 
* 정답은 **40**이다. `a`는 확장 리시버인 `A`의 값 `10`으로, `b`는 디스패치 리시버인 `B`의 값 `30`으로 해석된다.
* **확장 리시버가 디스패치 리시버보다 우선순위가 높다**는 규칙 때문이다. 코드만 보아서는 어느 쪽 `a`인지 알 수 없으며, 나중에 `A`에 `b`라는 프로퍼티가 추가되는 순간 **아무 경고 없이 결과가 바뀐다.** 
* 디스패치 리시버를 명시하려면 레이블이 필요하다.

```kotlin
fun A.test(): Int = this@B.a + b   // 50
```
 
* 이런 코드를 읽어야 하는 상황 자체가 바람직하지 않다.

**3.3 어느 객체를 수정하는지 드러나지 않는다**

```kotlin
class OrderContext {
    var processedCount = 0
 
    fun Order.markProcessed() {
        status = OrderStatus.PROCESSED   // Order를 수정
        processedCount++                 // OrderContext를 수정
    }
}
```

* 한 함수가 두 객체의 상태를 동시에 바꾸고 있지만, 시그니처만으로는 이를 알 수 없다. 부수 효과를 추적하기가 어려워진다.

**중첩된 스코프 함수 안에서 통제 불능이 된다**

```kotlin
PhoneBook().apply {
    someString.apply {
        // 여기서 isPhoneNumber()를 호출하면
        // 어느 String에 대한 호출인가?
    }
}
```

* `apply`, `with`, `run`, `let` 같은 스코프 함수는 스프링 코드에서 흔히 쓰인다. 여기에 멤버 확장이 끼어들면 암묵적 리시버가 여러 겹으로 쌓이면서 사람이 추론하기 어려운 코드가 된다.

**3.5 자바에서 호출할 수 없다**

* 멤버 확장 함수는 인스턴스 메서드로 컴파일되며 첫 인자로 확장 리시버를 받는다. 
* 자바 코드에서 이를 자연스럽게 호출할 방법이 사실상 없다. 
* 레거시 자바 모듈과 함께 가는 프로젝트라면 고려해야 할 지점이다.

#### 올바른 대안

**4.1 private 톱레벨 확장 함수**

* 가시성 제한이 목적이었다면 이것이 정답이다.

```kotlin
private fun PlaceOrderCommand.toEntity() = Order(...)
```

* 같은 파일 안에서만 보이며, 함수 참조도 정상적으로 동작한다.

**4.2 internal 톱레벨 확장 함수**

* 모듈 단위로 공개하고 싶다면 `internal`을 사용한다.

```kotlin
internal fun PlaceOrderCommand.toEntity() = Order(...)
```

**4.3 확장 대신 일반 함수로 파라미터를 받는다**

* 디스패치 리시버의 상태가 정말로 필요하다면, 그것은 애초에 확장이 아니라 **평범한 멤버 함수**여야 한다는 신호다.

```kotlin
@Service
class OrderService(
    private val policy: OrderPolicy,
) {
    // 확장이 아니라 파라미터로 받는다
    private fun toEntity(command: PlaceOrderCommand): Order =
        Order(
            memberId = command.memberId,
            totalAmount = policy.applyDiscount(command.totalAmount),
            status = OrderStatus.CREATED,
        )
}
```
* 무엇을 입력받아 무엇을 반환하는지가 시그니처에 그대로 드러난다.

#### 예외: DSL을 설계할 때

* **타입 안전한 빌더, 즉 DSL을 만들 때**다. 이때는 "특정 스코프 안에서만 이 문법이 유효하다"는 제약 자체가 설계 의도이므로, 멤버 확장이 오히려 올바른 도구가 된다.
* 스프링 WebFlux의 라우터 DSL이 대표적이다.

```kotlin
@Bean
fun routes(handler: OrderHandler) = coRouter {
    "/orders".nest {                        // String의 멤버 확장
        GET("", handler::findAll)
        POST("", handler::create)
    }
}
```
* `nest`는 `CoRouterFunctionDsl` 클래스 안에 정의된 `String`의 멤버 확장 함수다. `coRouter { }` 블록 바깥에서는 호출할 수 없어야 하므로 이 설계가 타당하다.
* Gradle Kotlin DSL, Exposed, Kotest, MockK 등도 같은 방식을 사용한다. 다만 이런 코드는 **DSL을 설계하는 입장**에서 쓰는 것이며, 일반적인 서비스·도메인 코드를 작성할 때 나올 일은 거의 없다. DSL을 만들 때는 `@DslMarker`를 함께 적용해 리시버가 겹치는 문제를 차단하는 것이 권장된다.

#### 판단 기준 정리

* **의도가 가시성 제한인가?** → `private` 또는 `internal` 톱레벨 확장을 쓴다.
* **디스패치 리시버의 상태가 정말 필요한가?** → 확장이 아닌 일반 멤버 함수로 만들고 파라미터로 받는다.
* **특정 스코프 안에서만 유효해야 하는 DSL 문법인가?** → 이때만 멤버 확장이 정당하다.
* 위 어디에도 해당하지 않는다면 → **톱레벨 확장 함수**로 뺀다.
