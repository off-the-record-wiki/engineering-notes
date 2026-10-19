# (Effective Java Item 1) Java ‐ 생성자 대신 정적 팩토리 메서드를 고려하라

#### 정적 팩토리 메서드 장점 1 - 이름을 가질 수 있다.
* 생성자에 넘기는 매개변수와 생성자 자체만으로는 반환될 객체의 특성을 제대로 설명하지 못한다. 반면 정적 팩토리는 이름만 잘 지으면 반환될 객체의 특성을 쉽게 묘사할 수 있다.
* 예컨대 생성자인 `BigInteger(int, int, Random)`과 정적 팩토리 메서드인 `BigInteger.probablePrime()` 중 어느 쪽이 소수인 BigInteger를 반환하는가에 대해 잘 설명된 것인지를 생각해보자.

#### 정적 팩토리 메서드 장점 2 - 호출될 때마다 인스턴스를 새로 생성하지 않아도 된다.
* 불변 클래스는 인스턴스를 미리 만들어 놓거나 새로 생성한 인스턴스를 캐싱하여 재활용하는 식으로 불필요한 객체 생성을 피할 수 있다.
* 생성 비용이 큰 객체가 자주 요청되는 상황이라면 이런 정적 팩토리 메서드 기법이 성능을 상당히 끌어올려 줄 수 있다.
* 플라이웨이트(Flyweight Pattern)도 이와 비슷한 기법이라고 할 수 있다.
* 반복되는 요청에 같은 객체를 반환하는 식으로 정적 팩토리 방식의 클래스는 언제 어느 인스턴스를 살아 있게 할지를 철저히 통제할 수 있다. 이러한 클래스를 인스턴스 통제 클래스라고 한다.
* 인스턴스를 통제하면 클래스를 싱글턴(Singleton)으로 만들 수도 있고 인스턴스화 불가로도 만들 수 있다.
* 또한 불변 값 클래스에서 동치인 인스턴스가 단 하나뿐임을 보장할 수 있다.(단 a == b일만 a.equals(b)가 성립)

#### 정적 팩토리 메서드 장점 3 - 반환 타입의 하위 타입 객체를 반환할 수 있는 능력이 있다.
* 반환할 객체의 클래스를 자유롭게 선택할 수 있다는 엄청난 유연성을 지닌다.

#### 정적 팩토리 메서드 장점 4 - 입력 매개변수에 따라 매번 다른 클래스의 객체를 반환할 수 있다.
* 반환 타입의 하위이기만 하면 어떤 클래스의 객체를 반환하든 상관없다.

#### 정적 팩토리 메서드 장점 5 - 정적 팩토리 메서드를 작성하는 시점에는 반환할 객체의 클래스가 존재하지 않아도 된다.
* 대표적인 예시로 지금은 사용되지 않는 JDBC가 있다.

#### 정적 팩토리 메서드 단점 1 - 상속을 하려면 public이나 protected 생성자가 필요하니 정적 팩토리 메서드만 제공하면 하위 클래스를 만들 수 없다.
* 자식 클래스가 부모 생성자를 호출해야 하므로 생성자가 공개(public) 또는 보호(protected)되어야 한다.
* 생성자를 private으로 막고 static 메서드로만 객체를 만들게 하면, 자식 클래스에서 부모 생성자를 쓸 수 없으니 상속 자체가 불가능하다.

#### 정적 팩토리 메서드 단점 2 - 정적 팩토리 메서드는 프로그래머가 찾기 어렵다.
* 생성자처럼 API 설명에 명확히 드러나지 않으니 사용자는 정적 팩토리 메서드 방식 클래스를 인스턴스화할 방법을 찾아내야 한다.

#### 정적 팩토리 메서드 명명

* **from** : 매개변수를 하나 받아서 해당 타입의 인스턴스를 반환하는 형변환 메서드
```java
Date d = Date.from(instant);
```
* **of** : 여러 매개변수를 받아 적합한 타입의 인스턴스를 반환하는 집계 메서드
```java
Set<Rank> faceCards = EnumSet.of(JACK, QUEEN, KING);
```
* **valueOf** : from과 of의 더 자세한 버전 → 타입 변환의 의미가 강함. 매개변수의 값을 해당 타입으로 변환
```java
BigInteger prime = BigInteger.valueOf(Integer.MAX_VALUE);
```
* **instance 혹은 getInstance** : (매개변수를 받는다면) 매개변수로 명시한 인스턴스를 반환하지만 같은 인스턴스임을 보장하진 않는다.
```java
StackWalker luke = StackWalker.getInstance(options);
```
* **create 혹은 newInstance** : instance 혹은 getInstance와 같지만, 매번 새로운 인스턴스를 생성해 반환함을 보장한다.
```java
Object newArray = Array.newInstance(classObject, arrayLen);
```
* **getType** : getInstance와 같으나, 생성할 클래스가 아닌 다른 클래스에 펙토리 메서드를 정의할 때 쓴다.
```java
FileStore fs = Files.getFileStore(path);
```
* **newType** : newInstance와 같으나, 생성할 클래스가 아닌 다른 클래스에 팩토리 메서드를 정의할 때 쓴다.
```java
BufferReader bf = Files.newBufferedReader(path);
```
* **type** : getType과 newType의 간결한 버전
```java
List<Complaint> litany = Collections.list(legacyLitany);
```
