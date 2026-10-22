# (Effective Java Item 54) Java - null이 아닌, 빈 컬렉션이나 배열을 반환하라

* 아래와 같이 컬렉션이 비었으면 `null`을 반환하는 패턴은 좋지 못하다.
```java
// Bad
private final List<Cheese> cheesesInStock = ...;

public List<Cheese> getCheeses() {
    return cheesesInStock.isEmpty() ? null : new ArrayList<>(cheesesInStock);
}
```
* 컬렉션이나 배열과 같은 컨테이너가 비었을 경우에는 컨테이너가 비었을 때, `null`을 반환하는 메서드를 사용할 때면 항시 방어 코드를 넣어줘야 한다.
* 만약 클라이언트에서 방어 코드를 뺴먹으면 오류가 발생할 수 있다.
* 실제로 객체가 0개일 가능성이 거의 없는 상황에서는 수년 뒤에 가서야 오류가 발생하기도 한다.
* 때로는 빈 컨테이너를 할당하는 데 비용이 드니 `null`을 반환하는 쪽이 낫다는 주장도 있으나 이는 2가지 측면에서 틀린 주장이다.
  * 첫 번째, 성능 분석 결과 이 할당이 성능 저하 주범이라고 확인되지 않는 한, 이 정도 성능 차이는 신경 쓸 수준이 못 된다.
  * 두 번째, 빈 컬렉션과 배열은 굳이 새로 할당하지 않고도 반환할 수 있다.
* 가능성은 미미하나, 사용 패턴에 따라 빈 컬렉션 할당이 성능을 눈에 띄게 떨어뜨릴 수도 있다.
* 그러나 해법은 간단하다. 바로, 매번 똑같은 빈 "불변" 컬렉션을 반환하는 것이다.
* 예시를 들어, `Collections.emptyList()`, `Collections.emptySet()`, `Collections.emptyMap()`이 있다.
* 단, 이 역시 최적화에 해당되니 꼭 필요할 때만 사용하는 것을 권장한다.

```java
// Good
public List<Cheese> getCheeses() {
    return cheesesInStock.isEmpty() ? Collections.emptyList() : new ArrayList<>(cheesesInStock);
}
```

* 배열의 경우도 마찬가지다.
* 절대 `null`을 반환하지 말고 길이가 0인 배열을 반환하라.
* 길이 0짜리 배열을 미리 선언해두고 매번 그 배열을 반환하면 된다. 길이 0인 배열은 모두 불변이기 때문이다.

```java
// Good
public static final Cheese[] EMPTY_CHEESE_ARRAY = new Cheese[0];

public Cheese[] getCheeses() {
    return cheesesInStock.toArray(EMPTY_CHEESE_ARRAY);
}
```
* 이 최적화 버전의 `getCheeses`는 항상 `EMPTY_CHEESE_ARRAY`를 인수로 넘겨 `toArray()`를 호출한다.
* 따라서 cheesesInStock이 비었을 때면 언제나 `EMPTY_CHEESE_ARRAY`를 반환하게 되는 것을 보장할 수 있다.
* 단순히 성능 개선 목적이라면, `toArray()`에 넘기는 배열을 미리 할당하는 것은 추천하지 않는다. 오히려 성능이 떨어진다는 결과도 있다.

```java
// Bad
return cheesesInStock.toArray(new Cheese[cheesesInStock.size()];
```
