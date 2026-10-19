# (Effective Kotlin Item 25) Kotlin - 각각의 함수는 하나의 추상화 수준으로 작성하라

#### 추상화 설계

* 추상화는 프로그래밍 세계에서 가장 중요한 개념 중 하나이다.
* 추상화란, 개념의 일반화, 속성 제거, 구체적인 객체에서 아이디러를 분리하는 과정 또는 결과이다.
* 추상화란 복잡도를 줄이고 실제 대상을 단순화하는 것이다. 구체적으로 표현하면 추상화는 메서드와 프로퍼티의 집합이라고 할 수 있다.
* 추상화의 핵심은 숨길 대상과 노출할 대상을 결정하는 것이다.

#### 프로그래밍에서의 추상화⚠️

```kotlin
// ❌ Bad - 'Utils'라는 이름 아래 서로 무관한 것들이 모여 응집도가 0에 가깝다
object Utils {
    fun formatDate(d: Date): String { ... }
    fun sendEmail(to: String) { ... }
    fun calculateTax(amount: Int): Int { ... }
    fun resizeImage(img: Image) { ... }
}
```
```kotlin
// ⭕ Good - 관련 있는 것끼리 의미 있는 경계로 묶는다 — 각 추상화가 하나의 책임을 가짐
object DateFormatter { fun format(d: Date): String { ... } }
class EmailSender { fun send(to: String) { ... } }
class TaxCalculator { fun calculate(amount: Int): Int { ... } }
class ImageResizer { fun resize(img: Image): Image { ... } }
```

#### 동일한 수준의 추상화

* 동일한 수준의 추상화 원칙에 따르면 새로운 함수를 만들 때에도 단순하게 만들어야 한다.
* 모듈식 시스템을 설계할 때 이 부분이 상당히 중요하다.

```kotlin
// ❌ Bad - 하나에 고수준 절차와 저수준 세부 동작이 전부 들어가 있다.
class CoffeeMachine {
    fun makeCoffee() {
        // 변수 선언
        // 상태 확인
        // 물 온도 센서 읽기
        // 히터 켜서 물 끓이기 (저수준)
        // 끓는 물을 원두에 통과시키기 (저수준)
        // 추출 시간 계산
        // 컵으로 붓기 (저수준)
        // 우유 거품기 작동 (저수준)
        // 우유 붓기 (저수준)
        // ... 수백 줄
    }
}
```
```kotlin
// ⭕ Good - 단계가 같은 레벨의 함수로 쪼개지면 변형을 조합으로 쉽게 만들 수 있다.
class CoffeeMachine {
    fun makeCoffee() {
        boilWater()    // 물을 끓인다.
        brewCoffee()   // 커피를 내린다.
        pourCoffee()
        pourMilk()
    }

    // 근데 그 물을 어떻게 끓일건데?
    private fun boilWater() {

    }

    // 근데 그 커피를 어떻게 내릴건데?
    private fun brewCoffee() {

    }

    private fun pourCoffee() {

    }

    private fun pourMilk() {

    }
}
```
