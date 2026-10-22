# (Effective Java Item 27) Java ‐ 비검사 경고를 제거하라

#### 비검사 경고를 제거하라

* 할 수 있는 한 모든 비검사 경고를 제거하라.
* 모두 제거한다면 그 코드는 타입 안전성이 보장된다.
* 즉, 런타임에 `ClassCastException`이 발생할 일이 없고, 여러분이 의도한 대로 잘 동작하리라 확신할 수 있다.
* 경고를 제거할 수 없지만 타입 안전하다고 확신할 수 있다면 `@SuppressWarnings("unchecked")` 어노테이션을 달아 경고를 숨기자.
  * 단, 타입 안전함을 검증하지 않은 채 경고를 숨기면 스스로에게 잘못된 보안 인식을 심어주게 된다.
* 또한 `@SuppressWarnings("unchecked")` 어노테이션은 항상 가능한 한 좁은 범위에 적용하자.

```java
// Good
public <T> T[] toArray(T[] a) {
    if (a.length < size) {
        // [정석] @SuppressWarnings은 가능한 한 가장 좁은 범위(지역변수)에 적용한다.
        // 주석: elementData는 내부적으로 Object[]이지만, 실제로는 T 타입만 담고 있으며
        // Arrays.copyOf를 통해 전달받은 a.getClass() 타입으로 생성되므로 T[]임이 보장된다.
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Arrays.copyOf(elementData, size, a.getClass());
        return result;
    }

    System.arraycopy(elementData, 0, a, 0, size);
    
    if (a.length > size) {
        a[size] = null;
    }
    
    return a;
}
```
* 위와 같이 `@SuppressWarnings("unchecked")` 어노테이션을 사용할 때면 그 경고를 무시해도 안전한 이유를 항상 주석으로 남겨야 한다.
* 다른 사람이 그 코드를 이해하는 데 도움이 되며, 더 중요하게는 다름 사람이 코드를 잘못 수정해 타입 안전성이 잃는 상황을 줄여준다.
