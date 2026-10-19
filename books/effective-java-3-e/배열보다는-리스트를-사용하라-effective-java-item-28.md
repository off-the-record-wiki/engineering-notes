# (Effective Java Item 28) Java ‐ 배열보다는 리스트를 사용하라

#### 배열보다는 리스트를 사용하라

* 배열은 공변이나 제네릭은 불공변이다.
  * 공변 : 함께 변한다. S가 T의 하위 타입이면, S[] 는 T[]의 하위 타입이다.
    * `List<S> 는 List<T> 의 하위 타입이다.`
  * 불공변 : 함께 변하지 않는다. S가 T의 하위 타입이면, T[]는 S[]의 하위 타입이다. 
    * `List<T> 는 List<S> 의 하위 타입이다.`
* 배열은 실체화된다. 그러나 제네릭은 타입 정보가 런타임에는 소거된다.
  * 배열은 런타임에도 자신이 담기로 한 원소의 타입을 인지하고 확인한다.
  * 원소 타입을 컴파일 타임에만 검사하며 런타임에는 알수조차 없다.
  * 이러한 사유로 인해 제네릭 타입의 배열을 만들지 못하게 되는데 타입이 안전하지 않기 때문이다.

```java
// Bad - 사용 불가
public class Chooser<T> {
    private final T[] choiceArray; // 제네릭 타입 배열은 런타임 시 타입이 소거되어 타입 안전성을 보장할 수 없다.

    public Chooser(Collection<T> choices) {
        choiceArray = choices.toArray();
    }
}
```
```java
// Good - 컬렉션 기반
public class Chooser<T> { 
    private final List<T> choiceList;

    public Chooser(Collection<T> choices) {
        choiceList = new ArrayList<>(choices);
    }

    public T choose() {
        Random rnd = ThreadLocalRandom.current();
        return choiceList.get(rnd.nextInt(choiceList.size());
    }
}
```
* Good Version의 코드와 같이 비검사 형변환 경고를 제거하려면 배열 대신 리스트를 사용하면 된다.
* 코드 양이 다소 늘고 조금 더 느릴 수 있으나 런타임 시 `ClassCastException`이 발생할 일이 없으니 그만한 가치가 있다.

>💡고려사항
>
> **1. 성능이 정말 중요한가?**
> * 배열이 리스트보다 약간 더 빠를 수 있지만, JVM 성능과 비즈니스 로직의 복잡도를 고려할 때는 그 차이는 미미하다. 안전성이 성능보다 우선이다.
> * 추가적으로 배열은 런타임에 문제를 발견하지만, 리스트는 컴파일 타임에 문제를 예방한다. 돌아가는 코드보다 안전한 코드를 지향하도록 한다.
>
> **2. API 설계 시**
> * 메서드 매개변수나 반환 타입으로 배열을 쓰면 클라이언트는 타입 안정성을 보장받기 어렵다. 항상 `List<T>`를 사용하는 것이 정석이다.
