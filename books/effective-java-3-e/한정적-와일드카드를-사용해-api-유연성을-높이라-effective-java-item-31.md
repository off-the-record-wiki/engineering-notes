# (Effective Java Item 31) Java - 한정적 와일드카드를 사용해 API 유연성을 높이라

#### 한정적 와일드카드를 사용해 API 유연성을 높이라

* 공변성 covariant - '공변하다'라고 말할 수 있는 경우는 A가 B의 하위타입일 때 List<A>가 List<B>의 하위타입인 경우이다.
* 반공변성 contravariant - 즉, B가 A의 하위타입일 때 List<A>가 List<B>의 하위타입인 경우이다.
* 무변성 invariant - A가 B의 타입이지만 List<A>와 List<B>는 아무 관계가 없는 경우 무변성, 무공변이라고 한다.
* 자바의 제네릭은 기본적으로 무변성을 따른다. 즉, Java는 무변성을 따르기 때문에 List\<String>과 List\<Object>는 아무런 관계가 없다. String이 Object의 하위 타입임에도 불구하고 Java는 List\<String>을 List\<Object>의 하위타입으로 판단하지 않는다.

#### 한정적 와일드카드

```java
public void pushAll(Collection<? extends E> elements) {
    position += elements.size();
    stack.addAll(elements);
}
```
* 위와 같이 한정적 와일드카드를 사용해 `Collection<E>`에서 `Collection<? extends E>`로 변경하면 E 타입의 하위 타입을 Java 컴파일러가 인식할 수 있다. 따라서 위 코드가 정상적으로 동작할 수 있다. 이를 공변적이라고 말한다.

```java
public void popAll(Collection<? super E> dst) {
    dst.addAll(stack);
}
```
* 이렇게 상위 타입을 받아도 되는 경우에는 `super` 키워드를 제공한다.
* 인자를 `Collection<E>`에서 `Collection<? super E>`로 변경한다면 문제없이 동작한다. 왜냐면 위의 경우는 Object가 Integer의 상위 타입이기 때문이다. 이를 반공변적이라고 한다.
* `extends`를 사용하는 경우는 제네릭 타입을 생산하는데 사용하고 있다.
* 반면 `super`를 사용하는 경우는 제네릭 타입을 소비하는데 사용된다. 따라서 `producer-extends`, `consumer-super`를 기억하면 좀 더 편하게 사용할 수 있다.
* 이렇게 API의 유연성을 극대화하기 위해서는 한정적 와일드카드를 사용하는 것이 유리하다. 다만 제네릭 타입이 생산자, 소비자로써 둘 다 사용된다면 한정적 와일드카드를 사용할 필요가 없다.

```java
// 1. 생산자(src)로부터 데이터를 꺼내므로 extends 사용
public void pushAll(Iterable<? extends E> src) {
    for (E e : src) push(e);
}

// 2. 소비자(dst)에게 데이터를 넣어주므로 super 사용
public void popAll(Collection<? super E> dst) {
    while (!isEmpty()) dst.add(pop());
}

// 사용 시:
Stack<Number> numberStack = new Stack<>();
Iterable<Integer> integers = List.of(1, 2, 3);
numberStack.pushAll(integers); // [성공] Integer는 Number의 하위 타입이므로 수용 가능!
```

**1. Producer-Extends(생산자 - extends)**

* 의미 : 컬렉션으로부터 데이터를 꺼내서 사용할 때는 `<? extends T>`를 사용한다.
* T의 하위 타입은 모두 상한선이 T기 때문에 데이터를 안전하게 꺼낼 수 있다.
* 제약 : 데이터를 넣을 수 없다.

**2. Consumer-Super(소비자 - super)**

* 의미 : 컬렉션에 데이터를 집어넣어 저장해야 할 때는 `<? super T>`를 사용한다.
* T를 받아줄 수 있는 하한선이라면 어떤 T의 하위 객체를 넣어도 안전하게 수용할 수 있다.
* 제약 : 데이터를 꺼낼 때 어떤 상위 타입인지 알 수가 없으므로 항상 Object로만 받아야 한다.
