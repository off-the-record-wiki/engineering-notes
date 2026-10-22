# (Effective Java Item 21) Java ‐ 인터페이스는 구현하는 쪽을 생각해 설계하라

#### 인터페이스는 구현하는 쪽을 생각해 설계하라

* 자바 8전에는 기존 인터페이스에 메서드를 추가했을 때, 존재할 가능성이 낮기 때문에 컴파일 오류가 발생한다.
* 그러나 자바 8 이후로 default가 추가되었다. 하지만 기존에 존재하는 구현체들과 매끄럽게 연동되리라는 보장은 없다.

```java
public interface Collection<E> extends Iterable<E> {

    // ...

    default boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<E> each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.next())) {
                each.remove(); 
	        removed = true;
            }
        }
        return removed;
    }

    // ...
}
```

* 디폴트 메서드는 컴파일에 성공헀다하더라도 기존 구현체에 런타임 오류를 일으킬 수 있다.
* 기존 인터페이스에 디폴트 메서드로 새로운 메서드를 추가하는 일은 정말로 꼭 필요한 경우가 아니라면 피해야 한다.
* 디폴트 메서드라는 도구가 생겼더라도 인터페이스를 설계할 때는 여전히 세심한 주의에 기울여야 한다.
* 인터페이스에 내재된 작은 결함도 불편한 일인데 심각하게 잘못된 인터페이스라면 이를 포함한 API가 어떤 재앙을 몰고 올지 알 수 없다.
