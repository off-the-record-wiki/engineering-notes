# 개발자가 반드시 정복해야 할 객체 지향과 디자인 패턴 - 객체 지향

#### 절차 지향(Procedural Oriented)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/5a5710ca-84aa-4a6d-9a2a-6545a6392547.png)

* 한 데이터를 사용하는 프로시저가 많을수록 그 데이터 타입을 변경하기 어려워진다.
* 예를 들어, 전자 장비의 꺼짐/켜짐 상태를 저장하기 위해 `boolean` 타입이고, 이름이 `isOn`이라는 데이터를 사용한다고 가정할때, 만약 요구사항이 변경되어서 꺼짐/켜짐, 그리고 대기 사앹까지 추가해야 한다고 가정하면 `boolean` 타입 대신 열거 타입으로 변경하면 이 데이터를 사용하는 모든 프로시저를 전부 변경해야 한다.
* 절차 지향적으로 프로그램을 구성할 때 매우 흔하게 발생하는 문제들이 있다. 이로 인해 새로운 요구사항이 생겨서 프로그램의 한 곳을 수정하게 되면 다른 곳에서 문제가 발생하고 다시 그 곳을 수정하면 또 다른 곳에서 문제가 발생하는 악순환이 발생한다. 이는 결국 코드 수정을 어렵게 만들며, 새로운 기능을 추가하는데 많은 구현 시간을 투입하게 만든다.

#### 객체 지향(Object Oriented)

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/46e68706-b197-4042-a640-9b4fc04ccfaf.png)

* 절차 지향과 달리 객체 지향은 데이터 및 데이터와 관련된 프로시저를 객체(Object)라고 불리는 단위로 묶는다.
* 객체는 프로시저를 수행하는데 필요한 만큼의 데이터를 가지며, 객체들이 모여 프로그램을 구성한다.
* 하지만 객체 지향적으로 설게하는 것은 매우 어렵다. 하지만 객체 지향적으로 만든 코드에서는 객체의 데이터를 변경하더라도 해당 객체로만 변화가 집중되고 다른 객체에는 영향을 주지 않기 때문에 요구 사항의 변화가 발생했을 때 절차 지향 방식보다 프로그램을 더 쉽게 변경할 수 있다는 장점을 갖는다.

#### 객체(Object)

* 객체 지향의 기본은 객체(Object)이다. 객체는 데이터와 그 데이터를 조작하는 프로시저로 구성된다고 했는데, 이는 객체의 물리적인 특징일 뿐이다.
* 실제로 객체를 정의할 때 사용되는 것은 객체가 제공해야 할 기능이며, 객체가 내부적으로 어떤 데이터를 갖고 있는 지로는 정의되지 않는다.
* 예를 들어, 소리 크기 제어 객체가 있다고 하자. 이 객체는 소리 크기를 제어하는 기능을 제공하며, 이 객체가 제공하는 기능은 다음과 같다.
  * 소리 크기 증가
  * 소리 크기 감소
  * 음소거

#### 인터페이스와 클래스

* 객체는 객체가 제공하는 기능으로 정의된다고 했는데 객체가 제공하는 기능을 오퍼레이션이라고 부른다.

```java
/**
 * 소리 크기 제어 인터페이스.
 *
 * 설계 원칙
 *  - 상태(현재 볼륨, 음소거 여부)를 setter로 열지 않는다.
 *    외부는 "무엇을 해달라"는 의도가 담긴 명령/질문 메시지만 보낸다. (Tell, Don't Ask)
 *  - 불변식은 구현체가 스스로 지킨다. 호출자가 범위를 검사할 필요가 없다.
 *
 * 불변식(구현체가 반드시 보장)
 *  1. 볼륨 레벨은 항상 [Level.MIN, Level.MAX] 안에 있다.
 *  2. 최대에서 turnUp, 최소에서 turnDown 해도 범위를 넘지 않는다(포화/clamp).
 *  3. mute()는 직전 레벨을 기억하고, unmute()는 그 레벨로 정확히 복원한다.
 *  4. 음소거 중 effective()(실제 출력)는 0이지만 current()(설정값)는 보존된다.
 */
public interface VolumeControl {

    /** 한 단계 올린다. 이미 최대면 아무 변화 없음(불변식 2). */
    void turnUp();

    /** 지정한 단계만큼 올린다. 결과가 최대를 넘으면 최대로 포화된다. */
    void turnUp(int steps);

    /** 한 단계 내린다. 이미 최소면 아무 변화 없음. */
    void turnDown();

    /** 지정한 단계만큼 내린다. 결과가 최소 미만이면 최소로 포화된다. */
    void turnDown(int steps);

    /**
     * 특정 레벨로 설정한다. Level 값 객체가 생성 시점에 [0,100]을 강제하므로
     * 잘못된 값 자체가 만들어질 수 없다(원시 int를 받지 않는 이유).
     */
    void setTo(Level level);

    /** 음소거한다. 현재 레벨은 보존되며 unmute()로 복원된다(불변식 3). */
    void mute();

    /** 음소거를 해제하고 직전 레벨로 복원한다. 음소거가 아니었다면 변화 없음. */
    void unmute();

    /** 음소거 상태인지 묻는다(질문형 메시지). */
    boolean isMuted();

    /** 사용자가 설정한 현재 레벨(음소거 여부와 무관하게 보존된 값). */
    Level current();

    /** 실제로 스피커에 전달되는 유효 레벨. 음소거 중이면 0을 반환한다. */
    Level effective();

    /**
     * 볼륨 레벨을 나타내는 불변 값 객체.
     *  - 식별자가 없고 값 자체로 동일성이 결정된다 → 값 객체.
     *  - 생성자에서 스스로 유효성을 검증하므로 유효하지 않은 Level은 존재할 수 없다.
     */
    final class Level {
        public static final int MIN = 0;
        public static final int MAX = 100;

        private final int value;

        public Level(int value) {
            if (value < MIN || value > MAX) {
                throw new IllegalArgumentException(
                    "볼륨은 " + MIN + "~" + MAX + " 범위여야 합니다: " + value);
            }
            this.value = value;
        }

        public int value() { return value; }

        public boolean isMax() { return value == MAX; }
        public boolean isMin() { return value == MIN; }

        @Override public boolean equals(Object o) {
            return (o instanceof Level) && ((Level) o).value == this.value;
        }
        @Override public int hashCode() { return Integer.hashCode(value); }
        @Override public String toString() { return "Level(" + value + ")"; }
    }
}
```

#### 객체의 책임과 크기⭐

* 객체는 객체가 제공하는 기능으로 정의된다고 했는데, 이는 다시 말하자면 객체마다 자신만의 책임(Responsibility)이 있다는 의미를 갖는다.
* 한 객체가 가지는 책임을 정의한 것이 바로 인터페이스이다. 머리로만 하든, 테스트 코드를 TDD처럼 만들든, 종이에 적어서 나열을 하든 완벽하지 않더라도 정리가 필요하다.
* 객체지향적으로 프로그래밍을 할 때, 가장 어려우면서 가장 중요한 것이 바로 객체마다 기능을 할당하는 과정이다. 기능이 몇 개 안 되는 경우에도 다양한 조합의 구성이 가능하기 때문에 이런 다양한 조합 중에서 알맞은 구성을 찾는 것은 쉽지 않다.
* 상황에 따라 객체가 가져야 할 기능의 종류와 개수가 달라지기 때문에 모든 상황에 들어맞는 객체 책임 구성 규칙이 존재하는 것은 아니다. 하지만 객체가 얼마나 많은 기능을 제공할 것인가에 대한 확실한 규칙이 하나 존재하는데, 그 규칙은 바로 객체가 갖는 책임의 크기는 작을수록 좋다는 것이다.
* 객체가 갖는 책임이 작아야 한다는 것은 객체가 제공하는 기능의 개수가 적다는 것을 의마한다. 만약 객체가 갖는 책임이 커질수록 절차 지향적으로 구조가 변질되며 절차 지향의 가장 큰 단점인 기능 변경의 어려움 문제가 발생하게 된다.

#### 의존

* 객체 지향적으로 프로그램을 구현하다 보면, 다른 객체가 제공하는 기능을 이용해서 기능을 완성하는 객체가 출현하게 된다.
* 한 객체가 다른 객체를 생성하거나 다른 객체 메서드를 호출할 때, 이를 그 객체에 의존(dependency)한다고 표현한다.

```java
public void process(ByteEncryptor encryptor) {
    // 내부에서 encryptor를 사용한다.
}
```
* 객체를 생성하든 메서드를 호출하든 또는 파라미터로 전달받든 다른 타입에 의존을 한다는 것은 의존하는 타입에 변경이 발생할 때 같이 변경될 가능성이 높다는 것을 생각해야 한다.
* 의존 영향은 꼬리에 꼬리를 문 것처럼 전파되는 특징을 가진다. 의존의 이런 특징 때문에 의존이 순환해서 발생할 경우 다른 방법이 없는지 고민해야 한다.

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/1c93780b-b02d-4b07-b668-69eaabf4ea84.png)

#### 캡슐화⭐

* 캡슐화(Encapsulation)는 객체가 내부적으로 기능을 어떻게 구현하는지를 감추는 것이다.
* 이를 통해 내부 기능이 변경되더라도 그 기능을 사용하는 코드는 영향을 받지 않도록 만들어 준다. 즉, 내부 구현 변경의 유연함을 주는 기법이다.

```kotlin
/**
 * 멤버십 유효 기간 — 값 객체(불변, 자기 유효성 검증).
 * 식별자가 없고 값 자체로 동일성이 결정되므로 값 객체다.
 */
data class MembershipPeriod(
    val startedOn: LocalDate,
    val expiresOn: LocalDate,
) {
    init {
        require(!expiresOn.isBefore(startedOn)) {
            "만료일은 시작일보다 앞설 수 없습니다: $startedOn ~ $expiresOn"
        }
    }
 
    /** 주어진 날짜가 이 기간 안에 포함되는가. */
    fun includes(date: LocalDate): Boolean =
        !date.isBefore(startedOn) && !date.isAfter(expiresOn)
 
    /** 만료일을 늘린 '새' 기간을 반환한다(자신은 바꾸지 않음 — VO는 불변). */
    fun extendedBy(days: Long): MembershipPeriod =
        copy(expiresOn = expiresOn.plusDays(days))
}
```

```kotlin
/**
 * 회원 — 엔티티. 만료 판단이라는 도메인 규칙을 캡슐화한다.
 * period가 private이라 외부는 만료일 자체를 알 수 없고, 행동을 통해서만 상호작용한다.
 */
class Member(
    val id: MemberId,
    private var period: MembershipPeriod,
) {
 
    /** 만료됐는가? — 만료 판단 규칙이 이 안에 모여 있다(외부로 새지 않음). */
    fun isExpired(clock: Clock): Boolean =
        LocalDate.now(clock).isAfter(period.expiresOn)
 
    /** 유효한 회원인가? '묻지 말고 시켜라'에 맞춰 질문형 메서드로 제공. */
    fun isActive(clock: Clock): Boolean = !isExpired(clock)
 
    /** 만료까지 남은 일수(음수면 이미 만료된 지 지난 일수). */
    fun daysUntilExpiry(clock: Clock): Long =
        ChronoUnit.DAYS.between(LocalDate.now(clock), period.expiresOn)
 
    /**
     * 멤버십 연장 — 상태 변경도 규칙을 지키며 스스로 수행한다.
     * setter로 만료일을 통째로 열어주지 않는 이유: 규칙 없는 변경을 막기 위해.
     */
    fun renew(days: Long) {
        require(days > 0) { "연장 일수는 양수여야 합니다: $days" }
        period = period.extendedBy(days)
    }
}
```

#### 캡슐화를 위한 2개의 규칙을 명심⭐

#### 1. Tell, Don't Ask

* 데이터를 물어보지 않고, 기능을 실행해달라고 말하라는 규칙이다.
* 데이터를 읽는 것은 데이터를 중심으로 코드를 작성하게 만드는 원인이 되며, 절차지향적인 코드를 유도하게 된다.
* Good 예시와 같이 데이터 대신 기능을 실행해달라고 명령을 하는 것이 좋다.
```kotlin
// ❌ Bad(getter로 잔액을 꺼내 바깥에서 검사하고, setter로 되돌려 넣는다.)
class BadAccount(var balance: Long)
 
fun badWithdraw(account: BadAccount, amount: Long) {
    // 규칙(잔액 부족 검사)이 호출자 쪽에 있다. 출금하는 모든 곳에 이 if가 복제된다.
    if (account.balance >= amount) {
        account.balance = account.balance - amount   // 무방비 setter로 상태 변경
    } else {
        throw IllegalStateException("잔액 부족")
    }
}
```

```kotlin
// ⭕ Good(규칙은 계좌가 스스로 지킨다.)
class GoodAccount(private var balance: Long) {
 
    fun withdraw(amount: Long) {
        require(amount > 0) { "출금액은 양수여야 합니다: $amount" }
        check(balance >= amount) { "잔액 부족: 잔액=$balance, 요청=$amount" }
        balance -= amount
    }
 
    // 상태를 통째로 노출하는 대신 필요한 '질문'만 제공
    fun canWithdraw(amount: Long): Boolean = amount in 1..balance
    fun balance(): Long = balance   // 표시용 조회는 열되, 변경은 오직 withdraw로
}
```

#### 2. 데미테르의 법칙(Law of Demeter)

* 데미테르의 법칙은 Tell, Don't Ask 규칙을 따를 수 있도록 만들어 주는 또 다른 규칙이다.

```kotlin
// ⭕ Good(한 메서드 안에서는 아래 네 부류의 객체에게만 메시지를 보낸다.)
@JvmInline
value class Money(val amount: Long) {
    operator fun plus(other: Money) = Money(amount + other.amount)
    companion object { val ZERO = Money(0) }
}
 
class Address(private val city: String, private val zipCode: String) {
    // Address의 내부(city/zip)를 통째로 노출하지 않고, 완성된 결과만 돌려준다.
    fun formatted(): String = "[$zipCode] $city"
}
 
class Customer(private val name: String, private val address: Address) {
    // 호출자가 address를 꺼내가지 않도록, 필요한 결과를 Customer가 조립해 준다(위임).
    fun shippingLabel(): String = "$name / ${address.formatted()}"   // (4) 필드 객체의 메서드
}
 
class OrderLine(private val unitPrice: Money, private val quantity: Int) {
    fun subtotal(): Money = Money(unitPrice.amount * quantity)
}
 
/** 영수증 — checkout 안에서 생성해 쓰는 지역 객체 */
class Receipt(private val total: Money) {
    fun print(): String = "결제 완료: ${total.amount}원"
}
 
interface PaymentGateway {
    fun charge(amount: Money): Boolean
}
 
class Order(
    private val lines: List<OrderLine>,   // 필드로 참조
    private val customer: Customer,        // 필드로 참조
) {
 
    /** (4) 필드로 참조하는 객체(lines, line)의 메서드만 호출 — 낯선 객체로 넘어가지 않는다. */
    fun total(): Money =
        lines.fold(Money.ZERO) { acc, line -> acc + line.subtotal() }
 
    /** 배송지 정보가 필요하면 customer에게 시킨다. address까지 파고들지 않는다. */
    fun shippingLabel(): String = customer.shippingLabel()   // (4) 필드 객체의 메서드
 
    /**
     * checkout — 세 부류의 호출이 모두 등장하는 예.
     * @param gateway 파라미터로 받은 객체
     */
    fun checkout(gateway: PaymentGateway): String {
        val amount = total()                       // (1) 자기 자신의 메서드
        require(gateway.charge(amount)) {          // (3) 파라미터로 받은 객체의 메서드
            "결제 실패"
        }
        val receipt = Receipt(amount)              // (2) 메서드에서 생성한 객체 ...
        return receipt.print()                     //     ... 의 메서드만 호출
    }
}
```
