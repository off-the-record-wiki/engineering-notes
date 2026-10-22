# (Effective Java Item 7) Java - 다 쓴 객체 참조를 해제하라

```java
public class Stack {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    private Object[] elements;
    private int size = 0;

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
        // [문제 발생 지점] 꺼내진 객체는 여전히 elements 배열이 참조하고 있음
        return elements[--size];
    }

    /**
     * 원소를 위한 공간을 적어도 하나 이상 확보한다.
     * 배열 크기를 늘려야 할 때마다 대략 두 배씩 늘린다.
     */
    private void ensureCapacity() {
        if (elements.length == size) {
            elements = Arrays.copyOf(elements, 2 * size + 1);
        }
    }

}
```
* `pop()` 메서드를 호출하면 실제 값은 삭제되지 않고 인덱스만 한칸씩 이동하는 것으로 메모리 누수가 발생하게 된다.

```java
// Good
public Object pop() {
    if (size == 0) {
        // [수정] 예외는 생성만 하는 것이 아니라 던져야(throw) 합니다.
        throw new EmptyStackException();
    }
    
    Object result = elements[--size];
    
    // [정석] 다 쓴 객체의 참조를 null로 해제하여 GC가 수거할 수 있게 함
    elements[size] = null; 
    
    return result;
}
```
* 다 쓴 참조를 `null` 처리하면 다른 이점도 따라온다.
* 만약 `null` 처리한 참조를 실수로 사용하게 되면 프로그램은 즉시 NPE를 던지며 종료된다.
* 지역 변수로 선언된 컬렉션 타입은 메서드가 끝나면(=스코프에서 벗어나면) 더 이상 참조가 없기에 GC의 대상이 된다. 따라서 일반적으로 메서드가 종료되면 메모리가 해제된다.
* 반대로 그 컬렉션 타입이 static으로 선언되어 참조가 유지되면 GC가 회수되지 못해 메모리가 유지되어 잠재적 메모리 누수가 발생하게 된다.

>❗주의사항
>
> * 모든 객체를 다 쓰자마자 일일이 `null` 처리하는 데 혈안이 되라는 말이 아니다.
> * 그럴 필요도 없고 바람직하지도 않기 때문이다. 오히려 프로그램을 필요 이상으로 지저분하게 만들 뿐이다.
> * 객체 참조를 `null` 처리하는 일은 예외적인 경우여야 한다.
>
> **1. 자기 메모리를 직접 관리하는 클래스**
> * 자기 메모리를 직접 관리하는 클래스라면 개발자가 항시 메모리 누수에 주의해야만 한다.
> * 원소를 다 사용한 즉시 그 원소가 참조한 객체들을 다 `null` 처리해줘야 한다.
>
> **2. 캐시(Cache)를 구현**
> * 객체 참조를 캐시에 넣고 나서 객체를 다 쓴 뒤로도 한참을 그냥 놔두는 경우가 많다.
> * 캐시를 만들 때 보통 캐시 엔트리의 유효 기간을 정확히 정의하기 어렵기 때문에 시간이 지날수록 엔트리의 가치를 떨어뜨리는 방식을 흔히 사용한다. 백그라운드 쓰레드를 활용하거나 캐시에 새 엔트리를 추가할 때 부수 작업으로 수행하는 방법들이 있다.
>
> **3. 리스너 및 콜백(Listener & Callback)**
> * 클라이언트가 콜백을 등록만 하고 명확히 해지하지 않는다면 콜백은 계속 쌓여갈 것이다.
> * 이를 방지하기 위해 등록 해제 시점에 참조를 끊어주어야 한다.
