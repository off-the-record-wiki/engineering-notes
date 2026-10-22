# (Effective Java Item 30) Java ‐ 이왕이면 제네릭 메서드로 만들라

#### 이왕이면 제네릭 메서드로 만들라

* 클래스뿐만 아니라 메서드도 제네릭화를 할 수 있다.
* 대표적인 예로 매개변수를 받는 정적 유틸리티 메서드(Collections의 알고리즘 메서드 binarySearch, sort ...)이 보통 제네릭 메서드이다.
```java
public static <T extends Comparable<? super T>> void sort(List<T> list) {
    list.sort(null);
}
```
```java
public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
    if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
        return Collections.indexedBinarySearch(list, key);
    else
        return Collections.iteratorBinarySearch(list, key);
}
```

* 타입 안전성 및 유연성 보장: 제네릭 메서드는 명시적인 형변환(casting) 없이 다양한 타입의 입력 데이터를 처리할 수 있어 코드의 재사용성이 높아진다. 또한, 컴파일 타임에 타입 불일치 오류를 잡아낼 수 있어 런타임 오류 가능성을 줄여준다.
* 유틸리티 클래스에 필수적: java.util.Collections와 같은 유틸리티 클래스는 특정 인스턴스에 종속되지 않는 정적(static) 메서드를 제공하므로, 대부분 제네릭 메서드로 구현되어 어떤 타입의 컬렉션에도 적용될 수 있도록 설계되었다.
* 와일드카드와 혼용: 예시로 드신 sort나 binarySearch 메서드의 복잡한 타입 선언(<? super T>, <? extends Comparable<? super T>>)은 와일드카드를 활용하여 메서드가 받아들일 수 있는 인자의 범위를 최대한 유연하게 확장한 것이다. 이는 제네릭 API 설계 시 유연성을 극대화하는 표준 방식이다.

#### 제네릭 메서드의 정의

```java
public static <T> Box<T> getBox(T o) { ...}
```

* 타입 매개변수 선언 위치: 제네릭 메서드를 정의할 때는 반환 타입(Box\<T>) 앞에 타입 매개변수 리스트(\<T>)를 선언해야 한다. 이는 컴파일러에게 이 메서드가 제네릭임을 알리고, 메서드 본문 내에서 T라는 타입 식별자를 사용할 수 있도록 허용한다.
* 타입 추론: 제네릭 메서드는 일반적으로 호출 시 컴파일러가 인자의 타입을 보고 자동으로 T를 추론한다. 예를 들어, getBox("Hello")를 호출하면 컴파일러는 T를 String으로 추론하여 \<String> Box\<String> getBox(String o) 형태로 동작하게 된다.

#### 불변 객체를 여러 타입으로 활용

* 제네릭은 런타임에 타입 정보가 소거되므로 하나의 객체를 어떤 타입으로든 매개변수화할 수 있다.
* 그러나 요청한 타입 매개변수에 맞게 매번 그 객체의 타입을 바꿔주는 정적 팩토리를 만들어야 한다.
* 이 패턴을 제네릭 싱글턴 팩토리라 한다.
* 타입 소거(Type Erasure)의 활용 : 자바 제네릭은 런타임에 타입 정보를 지워버리는 타입 소거 방식을 사용한다. 덕분에 Set\<String>과 Set\<Integer>는 런타임 시 모두 로(raw) 타입인 Set으로 취급된다.
* 하나의 인스턴스 공유 : 이 특성을 이용하면, 타입에 관계없이 동일한 로직을 수행하는 불변 객체(예: 빈 집합 Collections.emptySet())를 단 하나만 생성해두고 재사용할 수 있다.
* 정적 팩토리 메서드 : 사용자는 `Collections.emptySet()` 같은 정적 팩토리 메서드를 호출하며 원하는 타입을 명시한다.

#### 재귀적 타입 한정(recursive type bound)

* 드문 경우이지만, 자기 자신이 들어간 표현식을 사용하여 타입 매개변수의 허용 범위를 한정할 수 있다.
* 주로 Comparable 인터페이스와 같이 사용된다.

```java
// 예시
public static <E extends Comparable<E>> E max(Collection<E> c);

public interface Comparable<T> {
    int compareTo(T o);
}
```
