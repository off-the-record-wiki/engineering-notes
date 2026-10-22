# (Effective Java Item 47) Java - 반환 타입으로는 스트림보다 컬렉션이 낫다

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/3e9a262c-730c-4240-8dc2-496dfac50ed7.png)

* `Stream`을 `Iterable`로 변환하는 과정에서 어댑터 메서드가 필요하다.
* `Stream`의 경우 클래스 다이어그램을 보듯이 `Iterable`을 구현하지 않기 때문에 별도의 어댑터 메서드가 필요한 것이다.

```java
public static <E> Iterable<E> iterableOf(Stream<E> stream) {
    return stream::iterator;
}
```

* 별도의 어댑터 메서드를 구현해야하기 때문에 가능한 `Collection`을 사용하는 것이 좋다.
* `Collection` 인터페이스는 `Stream`, `Iterator`를 모두 지원할 수 있기 때문이다.
* 원소의 개수가 많아진다면 전용 컬렉션을 고려해보는 방법도 있다.
