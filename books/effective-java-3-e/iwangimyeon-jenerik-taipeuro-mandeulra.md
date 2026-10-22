# (Effective Java Item 29) Java ‐ 이왕이면 제네릭 타입으로 만들라

#### 이왕이면 제네릭 타입으로 만들라

```java
// Object 기반의 Stack
public class Stack {
   private Object[] elements;
   private int size = 0;
   private static final int DEFAULT_INITIAL_CAPACITY = 16;

   public Stack() {
       elements = new Object[DEFAULT_INITIAL_CAPACITY];
   }

   public void push(Object e) {
       ensureCapacity();
       elements[size++] = e;
   }

   public Object pop() {
       if (size == 0) {
           throw new EmptyStackException();
       }

       Object result = elements[--size];
       elements[size] = null;
       return result;
   }
}
```
* 제네릭 타입은 런타임 시점에 타입이 소거되기 때문에 제네릭 타입의 배열은 사용할 수 없다.
 
#### 배열을 사용한 코드를 제네릭으로 만드는 방법 1

```java
// push(E)로 넘어온 E 인스턴스만을 담는다.
// 타입 안전성을 보장하지만 실제로 런타임 타입은 Object[]이다.
@SuppressWarnings("unchecked")
public Stack() {
    elements = (E[]) new Object[DEFAULT_INITIAL_CAPACITY];
}
```

#### 배열을 사용한 코드를 제네릭으로 만드는 방법 2

```java
public E pop() {
    if (size == 0) {
        throw new EmptyStackException();

    @SuppressWarnings("unchecked")
    E result = (E) elements[--size];
    
    elements[size] = null; // 다 쓴 참조를 해제해 메모리 누수를 방지
    return result;
}
```

#### 권장되는 방안

```java
public static void main(String[] args) {
    GenericStack<String> stack = new GenericStack<>();

    for (String s : List.of("hi", "hello", "bye")) {
        stack.push(s);
    }

    while (!stack.isEmpty()) {
        System.out.println(stack.pop().toUpperCase());
    }
}
```
* `GenericStack<String>`을 사용하면 pop()의 반환 타입이 자동으로 String이 된다.
* Object 기반 스택과 달리, 반환된 값을 사용할 때 (String) stack.pop()과 같은 명시적 형 변환 코드를 작성할 필요가 없어 코드가 훨씬 깔끔하고 안전해진다.

#### 힙 오염(Heap Pollution)

* 힙 오염은 단어 그대로 JVM의 힙 영역에 저장되어있는 특정 변수가 불량 데이터를 참조함으로써 만일 힙에서 데이터를 가져오려고 할 때 예기치 못한 런타임 에러가 발생할 수 있는 오염 상태를 일컫는다.
* 제네릭은 컴파일하는 시점까지만 유효하고 컴파일이 되면 제네릭 타입 파라미터를 Object로 변환하거나 제거함으로써 하위 호환성을 맞출 수 있게 되었다.

> [📚 Erasure of Generic Types](https://docs.oracle.com/javase/tutorial/java/generics/genTypes.html)
>
> **1단계 : 타입 파라미터 치환(Replacing Type Parameters)**
> * 컴파일러는 제네릭 타입의 경계를 확인하고 해당 타입을 적절한 일반 타입으로 대체한다.
> * 제한 없는 타입 `<T>` : 경계가 없으므로 `Object`로 치환된다.
> * 제한 없는 타입 `<T extends Number>` : 첫 번째 경계인 `Number`로 치환된다.
>
> **2단계 : 타입 카스티칭 삽입(Inserting Type Casts)**
> * 타입 소거로 인해 런타임 시기에는 모든 데이터가 `Object`나 상위 클래스로 취급된다.
> * 따라서 컴파일러는 제네릭 타입을 사용하는 지점에서 명시적 형변환 코드를 자동으로 삽입해 올바른 타입으로 반환되도록 보장한다.
>
> **3단계 : 브리지 메서드 생성(Generating Bridge Methods)**
> * 다형성을 보존하기 위해 필요한 경우 컴파일러가 브리지 메서드를 생성한다.
> * 이는 제네릭 타입을 상속받거나 구현한 클래스에서 메서드 시그니처가 달라져 발생하는 문제를 해결하기 위함이다.
>
> **4단계 : 소거 결과**
> * 실체화 불가 : `List<String>`과 `List<Integer>`는 런타임에 똑같은 `List` 클래스 인스턴스일 뿐이다.
> * `instanceOf` 검사 불가 : 런타임에 타입 정보가 없기 때문에 `instanceOf`를 사용할 수 없다.
> * 배열 생성 불가 : 런타임에 타입 안정성을 보장할 수 없기 때문에 제네릭 배열 생성이 금지된다.
