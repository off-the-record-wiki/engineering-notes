# (Effective Kotlin Item 14) Kotlin - Enum Classes

#### 열거형 클래스

* Kotlin에서 정해진 값의 집합을 표현하는 가장 기본적인 수단은 열거형 클래스이다.
* 열거형 클래스는 모든 값을 쉼표로 구분한다.
* 열거형의 값은 UPPER_SNAKE_CASE 표기법으로 명명한다.

```kotlin
enum class PaymentOption {
    CASH,
    CARD,
    TRANSFER
}
```
* 모든 열거형 클래스는 다음 컴패니언 객체 함수를 가진다.
  * `entries` : 열거형 클래스의 모든 값을 리스트로 가지는 프로퍼티이다. 원소의 배열을 반환하는 `values` 함수를 최신 방식으로 교체한 개념이다.
  * `valueOf` : 입력받은 문자열의 이름과 일치하는 열거형 원소를 반환한다. 이 때, 대소문자를 구분하며 해당 원소가 없다면 예외를 던진다.
* 위 메서드들 대신 `enumValues`와 `enumValueOf`를 사용할 수도 있다.
* 열거형은 값들의 순서를 유지한다.
  * `name` : 값의 이름
  * `ordinal` : 값의 위치(0부터 시작한다.)
* 열거형 클래스는 `toString`, `equals`, `hashCode`를 구현하는 프로퍼티도 제공하며, 데이터 클래스에 없는 `compareTo` 메서드도 제공한다.
* `compareTo` 메서드는 원소들의 크기를 비교해주는 메서드로 선언된 순서가 곧 크기가 된다. → 나중에 선언된 원소가 더 크다.
* 열거형 값은 `when`문의 조건 브랜치에서도 사용할 수 있다.
```kotlin
fun transactionFee(paymentOption: PaymentOption) : Double =
    when (paymentOption) {
        PaymentOption.CASH -> 0.0
        PaymentOption.CARD, PaymentOption.TRANSFER -> 0.05
    }
```
* 열거형 클래스는 파싱하거나 문자열로 만들기 쉬워서 매우 편리하다.
* 그래서 원소 개수가 '한정된' 집합을 표현할 때 사용한다.

> * `when`문이 식으로 사용될 때, 열거형의 모든 원소를 커버하지 않으면 컴파일 에러가 발생한다. 자바의 `switch`문처럼 실수로 브랜치를 누락해서 발생하는 런타임 버그를 컴파일 시점에 원천 차단한다.
> * `else` 브랜치를 무분별하게 추가하는 것보다 모든 원소를 명시하는 것이 추후 새로운 열거형 원소가 추가될 때 컴파일 에러로 알려주므로 훨씬 안전하다.
```kotlin
fun printPayment(option: PaymentOption) {
    // 값을 반환하지 않는 '문(Statement)'
    when (option) {
        PaymentOption.CASH -> println("현금 결제")
        PaymentOption.CARD -> println("카드 결제")
        // TRANSFER 브랜치가 누락되었고 else도 없지만 컴파일 성공!
    }
}
```
```kotlin
// 식(Expression)으로 사용되는 예시
fun getCommission(option: PaymentOption): BigDecimal =
    when (option) {
        PaymentOption.CASH -> BigDecimal.ONE
        PaymentOption.CARD -> BigDecimal.TEN
        PaymentOption.TRANSFER -> BigDecimal.ZERO 
        // ⭕️ else가 없어도 모든 원소(CASH, CARD, TRANSFER)를 다 적었으므로 컴파일 성공!
    }
```

#### 열거형 값들의 데이터

* 열거형 값에는 상태가 있을 수 있다.
* 열거형 클래스에도 주 생성자를 정의할 수 있으며 이 때, 각 열거형 값은 이름 옆에 데이터를 지정해야 한다.
* 단, 열거형 값의 상태는 절대 변하지 않도록 만들어야 한다.

```kotlin
enum class PaymentOption(val commission: BigDecimal) {
    CASH(BigDecimal.ONE),
    CARD(BigDecimal.TEN),
    TRANSFER(BigDecimal.ZERO)
}
```

#### 커스텀 메서드를 가진 열거형 클래스

* 열거형 클래스에서 추상 메서드를 정의하고 원소마다 다르게 구현할 수 있다. → 컨센서스 주기 타입을 받을 때, 이 방식을 사용

```kotlin
enum class PaymentOption {
    CASH {
        override fun startPayment(transaction: Transaction) {
            showCashPaymentInfo(transaction)
        }
    },
    CARD {
        override fun startPayment(transaction: Transaction) {
            moveToCardPaymentPage(transaction)
        }
    },
    TRANSFER {
        override fun startPayment(transaction: Transaction) {
            showMoneyTransferInfo()
            setupPaymentWatcher(transaction)
        }
    };

    abstract fun startPayment(transaction: Transaction)     
}
```
