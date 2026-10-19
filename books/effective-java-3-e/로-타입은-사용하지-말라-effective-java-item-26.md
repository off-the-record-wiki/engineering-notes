# (Effective Java Item 26) Java ‐ 로 타입은 사용하지 말라

#### 로(Raw) 타입은 사용하지 말라

* 클래스와 인터페이스 선언에 타입 매개변수(type parameter)가 쓰이면 이를 제네릭 클래스 혹은 제네릭 인터페이스라 한다.
* 제네릭 클래스와 제네릭 인터페이스 타입을 통틀어 제네릭 타입(Generic Type)이라 한다.
* 각각의 제네릭 타입은 일련의 매개변수화 타입을 정의한다.
* 예컨대 `List<String>`은 원소의 타입이 String인 리스트를 뜻하는 매개변수화 타입이다. 여기서 String이 정규 타입 매개변수 E에 해당하는 실제 타입 매개변수이다.
* 로 타입(Raw Type)이란, 제네릭 타입에서 타입 매개변수를 전혀 사용하지 않을 때를 말한다. 예컨대 `List<E>`의 로 타입은 List이다.

```java
// Bad
private final Collection stamps = ...;

// Ex. 실수로 다른 타입을 넣었다!
stomps.add(new Coin());

// Bad
for (Iterator i = stamps.iterator(); i.hasNext(); ) {
    Stamp stamp = (Stamp) i.next(); // 실수로 Coin을 넣게 된 상태에서 다운캐스팅을 하면 ClassCastException을 던진다.
    stamp.cancel();
}
```
```java
// Good
private final Collection<Stomp> stomps = ...; // 매개변수화된 컬렉션 타입을 통해 타입 안전성을 확보해야 한다.
```
* 로 타입을 쓰는 걸 언어 차원에서 막지 않았지만 절대로 써선 안 된다.
* 로 타입을 쓰면 제네릭이 안겨주는 안전성과 표현력을 모두 잃게 된다. 그렇다면 절대 써서는 안되는 로 타입을 왜 만들어놓은걸까? 그것을 기존과의 하위 호환을 위함이다.
* 로 타입을 사용하는 것은 권장하지 않으나 그럴 때 비한정적 와일드카드 타입을 대신 사용하는 것이 좋다.
* 제네릭 타입을 쓰고는 싶지만 실제 타입 매개변수가 무엇인지는 신경 쓰고 싶지 않다면 물음표(?)를 사용한다.
```java
static int numElementsInCommon(Set<?> s1, Set<?> s2) { ... }
```

#### 로 타입을 사용하지 말라고는 했으나 예외 사항이 있다.
* class 리터럴에는 로 타입을 써야 한다.
  * 자바 명세는 class 리터럴에 매개변수화 타입을 사용하지 못하게 했다.(단, 배열과 기본 타입은 허용한다.)
  * 예를 들어, `String[].class`, `int.class`는 허용되나 `List<?>.class`, `List<String>.class`는 불허한다.
* `instanceof` 연산자와의 연관성
  * 런타임에는 제네릭 타입 정보가 지워지기 때문에 `instanceof` 연산자는 비한정적 와일드카드 타입 이외의 매개변수화 타입에는 적용할 수 없다.(비한정적 와일드카드 타입은 타입 매개변수가 무엇인지 신경을 안 쓰기 때문에 기본적으로 최상위 객체인 Object로 간주된다.)
