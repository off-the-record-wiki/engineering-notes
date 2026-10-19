# (Effective Java Item 58) Java ‐ 전통적인 for문보다는 for‐each문을 사용하라

* for-each문의 정식 이름은 향상된 for문(enhanced for statement)이다.
* 반복자와 인덱스 변수를 사용하지 않으니 코드가 깔끔해지고 오류가 날 일도 없다.
* 하나의 관용구로 컬렉션과 배열을 모두 처리할 수 있어 어떤 컨테이너를 다루는지는 신경쓰지 않아도 된다.

```java
for (Element e: elements) {
    // ...
}
```

#### for-each문을 사용할 수 없는 3가지 상황

* 파괴적인 필터링(destructive filtering) : 컬렉션을 순회하면서 선택된 원소를 제거해야 한다면 반복자의 remove 메서드를 호출해야 한다. 자바 8부터는 Collection의 removeIf 메서드를 사용해 컬렉션을 명시적으로 순회하는 일을 피할 수 있다.
* 변형(transforming) : 리스트나 배열을 순회하면서 그 원소의 값 일부 혹은 전체를 교체해야 한다면 리스트의 반복자나 배열의 인덱스를 사용해야 한다.
* 병렬 반복(parallel iteration) : 여러 컬렉션을 병렬로 순회해야 한다면 각각의 반복자와 인덱스 변수를 사용해 엄격하고 명시적으로 제어해야만 한다.
* 위와 같은 3가지 상황 중 하나에 속할 때는 일반적인 for문을 사용하되 언급한 문제들을 경계해야 한다.

#### Iterable

```java
public interface Iterable<E> {
    Iterator<E> iterator();
}
```
* `Iterable`을 처음부터 직접 구현하기는 까다롭지만, 원소들의 묶음을 표현하는 타입을 작성해야 한다면 `Iterable`을 구현하는 쪽으로 고민해보는 것을 권장한다.
