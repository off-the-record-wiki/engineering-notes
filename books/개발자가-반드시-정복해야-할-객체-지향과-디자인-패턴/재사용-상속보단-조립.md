# 개발자가 반드시 정복해야 할 객체 지향과 디자인 패턴 - 재사용: 상속보단 조립

#### 상속을 통한 재사용의 단점 1 - 상위 클래스 변경의 어려움

* 상속은 상위 클래스의 변경을 어렵게 만든다.
* 상속 계층을 따라 상위 클래스 변경이 하위 클래스에 영향을 주기 때문에 최악의 경우 상위 클래스의 변화가 모든 하위 클래스에 영향을 줄 수 있다.
* 이는 클래스 계층도에 있는 클래스들을 한 개의 거대한 단일 구조처럼 만들어 주는 결과를 초래한다.
* 이런 이유 때문에, 클래스 계층도가 커지면 커질수록 상위 클래스를 변경하는 것은 어려워진다.

#### 상속을 통한 재사용의 단점 2 - 클래스의 불필요한 증가

* 상속을 통한 기능 재사용에서 유사한 기능을 확장하는 과정에서 클래스의 개수가 불필요하게 증가할 수 있다.
* 수집기에 부가 기능을 상속으로 얹는다고 가정하자. 예를 들어, 버퍼링(모아서 한 번에 기록), 암호화(기록 전 암호화) 2개를 추가하려면 다음과 같이 늘어나게 된다.
```
FileLogCollector
BufferedFileLogCollector
EncryptedFileLogCollector
BufferedEncryptedFileLogCollector
```

#### 상속을 통한 재사용의 단점 3 - 상속의 오용

* 상속을 통한 재사용의 문제로 상속 자체를 잘못 사용할 수 있다는 점이다.
* 예를 들어, 다음과 같이 3가지 기능을 제공한다고 가정하자.
  * 수화물을 넣는다.
  * 수화물을 뺀다.
  * 수화물을 넣을 수 있는 여력이 되는지 확인한다.
```java
public class Container extends ArrayList<Luggage> {

    private int maxSize;
    private int currentSize;

    public Container(int maxSize) {
        this.maxSize = maxSize;
    }

    public void put(Luggage lug) throws NotEnoughSpaceException {
        if (!canContain(lug)) {
            throw new NotEnoughSpaceException();
        }
        super.add(lug); 
        currentSize += lug.size();
    }

    public void extract(Luggage lug) {
        super.remove(lug);
        this.currentSize -= lug.size();
    }

    public boolean canContain(Luggage lug) {
        return maxSize >= currentSize + lug.size();
    }
}
```
* 상속은 IS-A 관계가 성립할 때만 사용해야 하는데, '컨테이너는 ArrayList이다'는 IS-A 관계가 아니다.

> **❗IS-A vs HAS-A**
>
> * IS-A : 상속으로 표현하는 것. 예를 들어, 고양이는 동물이다. 정사각형은 도형이다. 이렇게 자식이 부모의 한 종류일 때 성립한다.
> * HAS-A : 합성으로 표현하는 것. 예를 들어, 자동차는 엔진을 가진다. 이렇게 대체 가능한 행동이 필요한 경우 성립한다.

#### 조립을 이용한 재사용

* 객체 조립(Composition)은 여러 객체를 묶어서 더 복잡한 기능을 제공하는 객체를 만들어내는 것이다.
* 조립(Composition)을 이용한 재사용은 작은 기능 단위를 조합해서 새로운 기능을 만드는 방식이다. 상속처럼 부모 클래스에 강하게 묶이지 않고, 필요한 객체를 가져다 쓰는 형태라 결합도가 낮고 유연하다.

```java
// 1. 재사용 가능한 작은 부품(역할)들을 인터페이스로 정의
interface PaymentMethod {
    void pay(int amount);
}

interface DiscountPolicy {
    int applyDiscount(int amount);
}

interface Notifier {
    void notifyDone(String message);
}
```

```java
// 2. 각 부품의 구체 구현 — 독립적으로 재사용/교체 가능
class CardPayment implements PaymentMethod {
    public void pay(int amount) {
        System.out.println("카드로 " + amount + "원 결제");
    }
}

class KakaoPayment implements PaymentMethod {
    public void pay(int amount) {
        System.out.println("카카오페이로 " + amount + "원 결제");
    }
}

class CouponDiscount implements DiscountPolicy {
    public int applyDiscount(int amount) {
        return amount - 2000; // 쿠폰 2000원 할인
    }
}

class NoDiscount implements DiscountPolicy {
    public int applyDiscount(int amount) {
        return amount;
    }
}
```

```java
// 3. Order는 부품들을 "조립"해서 사용
class Order {
    private final PaymentMethod payment;
    private final DiscountPolicy discount;
    private final Notifier notifier;

    // 생성자로 부품을 주입받아 조립
    Order(PaymentMethod payment, DiscountPolicy discount, Notifier notifier) {
        this.payment = payment;
        this.discount = discount;
        this.notifier = notifier;
    }

    void checkout(int amount) {
        int finalAmount = discount.applyDiscount(amount);
        payment.pay(finalAmount);
        notifier.notifyDone("결제 완료: " + finalAmount + "원");
    }
}
```

#### 위임(Delegation)

* 위임은 내가 할 일을 다른 객체에게 넘긴다는 의미를 담고 있으며, 보통 조립 방식을 이용해서 위임을 구현한다.

```java
// 실제 일을 담당할 전문가(위임 대상)
class PayCalculator {
    private final double taxRate;

    PayCalculator(double taxRate) {
        this.taxRate = taxRate;
    }

    int calculate(int baseSalary, int workDays) {
        int gross = baseSalary + (workDays * 50000);
        int tax = (int) (gross * taxRate);
        return gross - tax;
    }
}
```
```java
// 2. Employee는 계산을 "직접 하지 않고" 넘긴다
class Employee {
    private final String name;
    private final int baseSalary;
    private final int workDays;
    private final PayCalculator calculator; // 위임 대상을 가짐

    Employee(String name, int baseSalary, int workDays, PayCalculator calculator) {
        this.name = name;
        this.baseSalary = baseSalary;
        this.workDays = workDays;
        this.calculator = calculator;
    }

    // 요청은 받되, 실제 처리는 calculator에게 위임
    public int getPay() {
        return calculator.calculate(baseSalary, workDays);
    }
}
```

#### 상속은 언제 사용하는지?

* 상속은 재사용이라는 관점이 아닌 기능의 확장이라는 관점에서 상속을 적용해야 한다.
