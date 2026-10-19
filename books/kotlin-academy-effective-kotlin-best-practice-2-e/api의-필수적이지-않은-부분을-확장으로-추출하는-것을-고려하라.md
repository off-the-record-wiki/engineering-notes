# (Effective Kotlin Item 45) Kotlin - API의 필수적이지 않은 부분을 확장으로 추출하는 것을 고려하라

#### API의 필수적이지 않은 부분을 확장으로 추출하는 것을 고려하라

* 클래스의 본질적 동작은 멤버 함수로 두고, 부가적 편의 기능은 확장 함수로 클래스 바깥에 분리하라는 원칙이다.

#### 무엇이 "필수적"인가?

* 판단의 기준은 다음 질문으로 정리할 수 있다.

> 이 동작이 없으면 해당 타입이 자기 역할을 못 하는가?

| 구분 | 성격 | 예시 |
|---|---|---|
| 필수적 (멤버) | 타입의 정체성, 불변식, 상태 전이 | `Order.cancel()`, `Money.plus()`, `compareTo()` |
| 필수적 (멤버) | 다형성이 필요한 동작 | 인터페이스 구현, 오버라이드 대상 |
| 비필수적 (확장) | 표현·포맷팅 | `Order.displayAmount()` |
| 비필수적 (확장) | 계층 간 변환 | `Order.toResponse()`, `OrderEntity.toDomain()` |
| 비필수적 (확장) | 특정 상황에서만 쓰이는 편의 기능 | `Order.isRefundable()` |

* `Order`가 자신의 상태를 어떻게 전이시키는지는 도메인 불변식이므로 클래스가 직접 책임져야 한다.
* 반면 그 주문을 화면에 어떻게 보여줄지, API 응답으로 어떻게 바꿀지는 `Order`가 알 필요가 없는 문제다.

#### 확장으로 분리했을 때 얻는 것

**3.1 사용자가 선택할 수 있다**

* 멤버 함수는 클래스를 쓰는 모든 곳에 자동으로 노출된다. 확장 함수는 **임포트해야만** 보인다. 따라서 어떤 모듈에서만 필요한 기능을 다른 모듈의 자동완성 목록에서 치울 수 있다.
```kotlin
import com.example.order.presentation.toResponse   // 이 임포트가 있어야만 사용 가능
```

**3.2 클래스 API가 작아진다**

* 편의 메서드가 20개 붙은 엔티티는 읽기 어렵다. 핵심 동작만 클래스에 남기면, 그 클래스가 무엇을 책임지는지 한눈에 드러난다.

**3.3 이름 충돌을 해결할 수 있다**

* 서로 다른 라이브러리가 같은 이름의 확장을 제공해도, 임포트 시점에 이름을 바꿀 수 있다. 멤버 함수로는 불가능한 일이다.
```kotlin
import com.libA.toJson
import com.libB.toJson as toJsonB
```

**3.4 nullable 타입에도 정의할 수 있다**

```kotlin
fun Order?.isRefundableOrFalse(): Boolean = this?.isRefundable() ?: false
```

* 멤버 함수는 인스턴스가 있어야 호출되므로 이런 형태가 불가능하다.

**3.5 계층 간 의존성 방향을 지킬 수 있다**

* 이것이 스프링 프로젝트에서 가장 실용적인 이점이다. 도메인 모델이 프레젠테이션 계층의 DTO를 알지 못하도록 유지하면서도, 변환 코드를 자연스러운 문법으로 쓸 수 있다.

```kotlin
// domain 모듈 — DTO를 전혀 모른다
package com.example.order.domain
 
class Order(val id: Long, val totalAmount: BigDecimal, var status: OrderStatus) {
    fun cancel() { ... }
}
```
 
```kotlin
// presentation 모듈 — 여기서 domain을 향해 단방향 의존한다
package com.example.order.presentation
 
fun Order.toResponse() = OrderResponse(
    orderId = id,
    amount = totalAmount,
    status = status.name
)
```
* 만약 `toResponse()`를 `Order`의 멤버 함수로 만들었다면, 도메인 모듈이 프레젠테이션 모듈을 역방향으로 참조하게 된다. 
* 헥사고날 아키텍처나 멀티모듈 구성에서 흔히 마주치는 문제이며, 확장 함수는 이를 깔끔하게 해소한다.

#### 확장으로 분리했을 때 잃는 것

**4.1 가상 디스패치가 일어나지 않는다(가장 중요)**

* 확장 함수는 **정적으로 결정**된다. 런타임 타입이 아니라 컴파일 시점의 선언 타입을 기준으로 호출 대상이 정해진다.

```kotlin
open class Payment
class CardPayment : Payment()
 
fun Payment.fee() = "기본 수수료"
fun CardPayment.fee() = "카드 수수료"
 
val p: Payment = CardPayment()
println(p.fee())   // "기본 수수료"
```

* 멤버 함수였다면 `"카드 수수료"`가 출력된다. 따라서 **하위 타입마다 다르게 동작해야 하는 기능은 절대 확장으로 만들면 안 된다.** 다형성이 필요한 순간 그것은 이미 필수적 API다.

**4.2 리플렉션에 잡히지 않는다**

* 확장 함수는 클래스의 멤버가 아니므로 `KClass.members`에 나타나지 않는다. 다음 도구들이 확장 함수를 인식하지 못한다.
  * Jackson 직렬화(`@JsonProperty` 붙은 게터로 동작하지 않음)
  * JPA / Hibernate 프로퍼티 매핑
  * 스프링 AOP 프록시(`@Transactional`, `@Cacheable` 등이 적용되지 않음)
  * 스프링 데이터의 프로젝션 인터페이스
* 특히 **확장 함수에 `@Transactional`을 붙여도 트랜잭션이 걸리지 않는다.** 프록시가 가로챌 대상 자체가 없기 때문이다. 실무에서 자주 나오는 함정이다.

**4.3 private·protected 멤버에 접근할 수 없다**

```kotlin
class Order(private val internalCode: String) { ... }
 
fun Order.debugInfo() = internalCode   // 컴파일 에러
```

* 캡슐화된 내부 상태를 다뤄야 하는 로직이라면 멤버가 될 수밖에 없다. 이 역시 "필수적"인지를 가르는 좋은 신호다.

**4.4 자바에서 호출하면 어색하다**

```java
OrderExtensionsKt.toResponse(order);
```
* 자바와 코틀린을 함께 쓰는 프로젝트라면 이 점을 고려해야 한다.

**4.5 멤버가 항상 이긴다**

* 시그니처가 같은 멤버 함수가 나중에 추가되면, 기존 확장 함수는 **아무 경고 없이 무시된다.**
* 내가 통제하지 못하는 외부 라이브러리 타입에 확장을 정의할 때는 이런 위험이 있다는 점을 알아두어야 한다.

```kotlin
class Foo {
    fun bar() = println("member")
}
fun Foo.bar() = println("extension")
 
Foo().bar()   // "member"
```

#### 주의: 확장 남용도 문제다

* "가능한 한 확장으로 빼라"는 말이 **아니다.**
* 도메인 로직을 전부 확장 함수로 뽑아내면, 클래스는 데이터만 담은 빈껍데기가 되고 로직은 프로젝트 전역에 흩어진다. 자바에서 흔히 비판받는 **빈약한 도메인 모델(Anemic Domain Model)** 로 되돌아가는 셈이다.
* 핵심은 균형이다. **타입의 정체성을 이루는 동작은 반드시 클래스 안에 남기고, 그 바깥의 편의 기능만 확장으로 덜어낸다.** 아이템 제목이 "추출하라"가 아니라 "추출하는 것을 **고려하라**"인 이유가 여기에 있다.
