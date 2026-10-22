# (Effective Java Item 17) Java ‐ 변경 가능성을 최소화하라

#### 변경 가능성을 최소화하라

* 불변 클래스란, 그 인스턴스 내부 값(물리적 값)을 수정할 수 없는 클래스이다.
* 불변 인스턴스에 간직된 정보는 고정되어 객체가 파괴되는 순간까지 절대로 달라지지 않는다.
* 클래스를 불변으로 만들고자 한다면 다음 다섯 가지 규칙을 따르면 된다.

#### 불변 클래스 생성을 위한 다섯 가지 규약이란?

* 객체 상태를 변경하는 메서드(Setter와 같은 변경자)를 제공하지 않는다.
* 클래스를 확장할 수 없도록 한다.
  * 하위 클래스에서 부주의하게 혹은 나쁜 의도롤 객체 상태를 변경하는 시도를 막아준다. 
* 모든 필드를 final로 선언한다.
  * 시스템이 강제하는 수단을 이용해 설계 의도를 명확히 드러낸다.
* 모든 필드를 private으로 선언한다.
  * 필드가 참조하는 가변 객체를 public으로 열어두면 클라이언트에서 직접 접근해 수정할 수 있는 문제가 발생한다.
  * 기술적으로는 Primitive Type이나 불변 객체를 참조하는 필드를 public final로만 하더라도 불변 객체가 되나 다음 릴리스에서 내부 표현을 바꾸지는 못하므로 권장하지는 않는다.
* 자신 외에는 내부 가변 컴포넌트에 접근할 수 없도록 한다.

```java
public final class Money {
    private final String currency;
    private final int amount;
    private final String[] cards;

    public Money(String currency, int amount) {
        this.currency = currency;
        this.amount = amount;
    }

    public Money plus(int additionMoney) {
        return new Money(this.name, this.amount + additionMoney);
    }

    public String[] getCards() {
        return cards.clone();
    }
}
```
* 이런 불변 객체는 생성된 시점의 상태가 파괴될 때까지 간직된다.
* 불변 객체는 근본적으로 Thread-Safe하기에 따로 동기화를 할 필요가 없다.
* 허나 불변 객체가 장점만 있는 것은 아니다. 단점도 존재한다.
* 값이 다르면 무조건 독립된 객체를 생성해야 한다.

```java
public Member(String name, Address address) {
    this.name = name;
    this.address = new Address(address.getPost()); // 새로운 address를 생성
}
```
