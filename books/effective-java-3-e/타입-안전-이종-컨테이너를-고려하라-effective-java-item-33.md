# (Effective Java Item 33) Java ‐ 타입 안전 이종 컨테이너를 고려하라

#### 타입 안전 이종 컨테이너

* `type-safe` : generic을 통해 타입 안전성이 보장된다.
* `hetrogeneous` : 서로 다른 타입이 아니라 하나의 컨테이너에 존재할 수 있다.
* `container` : 무언가를 담고 있는 객체를 뜻하며 쉽게 말해 Map이나 Set 등의 컬렉션이나 박싱 클래스를 예로 들 수 있다.

#### 왜 사용하는지?

* 제네릭에는 타입이 정해져 있다. 예를 들어, `Map<K, V>`에서는 설정된 V타입만 값으로 지정할 수 있다.
* 파라미터화된 제네릭 `Map<String, Integer>`가 있으면 Integer 이외의 값을 담을 수 있다. 이렇게 제네릭 타입의 수가 정해져 있는 것은 일반적이고 실제로 문제없이 작동한다.

#### 어떻게 사용하는지?

* 컨테이너를 매개변수화 하지 않고 컨테이너의 키를 매개변수화 하는 것이다.
```java
private Map<Class<?>, Object> favorites = new HashMap<>();

public <T> void putFavorites(Class<T> type, T instance) {
    favorites.put(Objects.requireNonNull(type), instance);
}

public <T> T getFavorite(Class<T> type) {
    return type.cast(favorites.get(type));
}
```

#### 왜 가능한지?

* `Class` 클래스가 바로 제네릭이기 때문이다.
```java
public final class Class<T> 
        implements java.io.Serializable, 
                   GenericDeclaration, 
                   Type, 
                   AnnotatedElement, 
                   TypeDescriptor.OfField<Class<?>>, 
                   Constable {
    // 클래스 내부 구현 ...
}
```
