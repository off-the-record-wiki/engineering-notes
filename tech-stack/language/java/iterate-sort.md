# Java ‐ Iterate & Sort

#### Iterable, Iterator

* Iterable : 반복 가능한
* Iterator : 반복자

```java
public interface Iterable<T> {
    Iterator<T> iterator();
}
```

```java
public interface Iterator<E> {
    boolean hasNext();
    E next();
}
```

![](https://raw.githubusercontent.com/wiki/thought-corner/study/images/attachments/eb41d89d-f88f-41f2-be29-06fde0d40908.png)

* 최상위 인터페이스에 Iterable이 있고 컬렉션 인터페이스가 이 인터페이스를 상속받고 하위 클래스가 구현을 하기 때문에 컬렉션을 구현한 클래스들은 전부 순회가 가능하다. 

>❗Iterator(반복자) 디자인 패턴
>
> * 객체 지향 프로그래밍에서 컬렉션 요소를 순회할 때 사용되는 디자인 패턴이다.
> * 이 패턴은 컬렉션 내부 표현 방식을 노출시키지 않으면서도 그 안의 각 요소에 순차적으로 접근할 수 있게 해준다.
> * Iterator 패턴은 컬렉션 구현과는 독립적으로 요소들을 탐색할 수 있는 방법을 제공하며, 코드 복잡성을 줄이고 재사용성을 높일 수 있다.

#### Comparable, Comparator

* 객체의 기본 정렬 방법은 객체에 Comparable 인터페이스를 직접 구현해서 정의한다.
* 이렇게 하면 객체는 이름 그대로 비교할 수 있는 객체가 되고 기본 정렬 방법을 가진다.
* 그러나 기본 정렬 방법 외에 다른 정렬 방법을 사용해야 하는 경우 비교자(Comparator)를 별도로 구현해서 정렬 메서드에 전달하면 된다.

```java
public class IdComparator implements Comparator<UserV2> {

	@Override
	public int compare(UserV2 o1, UserV2 o2) {
		return o1.getId().compareTo(o2.getId());
	}
}

Arrays.sort(list1, new IdComparator());	// 별도로 구현해서 전달
```

```java
public class UserV1 implements Comparable<UserV1> { // 객체에 Comparable 직접 구현
    // ...

    @Override
    public int compareTo(UserV1 o) {
	return this.age < o.age ? -1 : (this.age == o.age) ? 0 : 1;
    }
}
```

#### 컬렉션 프레임워크 전체 개요

* 자바 컬렉션 프레임워크는 데이터 그룹을 저장하고 처리하기 위한 통합 아키텍처를 제공한다.
* 여기서 컬렉션이란 객체의 그룹이나 집합을 말한다.
* 일관성
  * 모든 컬렉션 타입들이 Collection 인터페이스를 구현, 모든 컬렉션들이 기본 동작을 공유한다는 것을 보장한다.
  * 다양한 타입의 컬렉션을 다룰 때 일관된 방식으로 접근할 수 있게 해준다.
* 재사용성
  * Collection 인터페이스에 정의된 메서드들은 다양한 컬렉션 타입들에 공통으로 적용된다.
  * 코드 재사용성을 높이고 유지 보수를 용이하게 한다.
* 확장성
  * 새로운 컬렉션 타입을 만들 때, Collection 인터페이스를 구현함으로써 기존에 정의된 알고리즘과 도구를 사용할 수 있게 된다.
  * 이는 프레임워크 확장성을 향상시킨다.
* 다형성
  * Collection 인터페이스를 사용함으로써, 다양한 컬렉션 타입들을 같은 타입으로 다룰 수 있다.
