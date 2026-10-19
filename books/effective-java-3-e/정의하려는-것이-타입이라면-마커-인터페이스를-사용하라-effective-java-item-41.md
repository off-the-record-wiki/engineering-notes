# (Effective Java Item 41) Java ‐ 정의하려는 것이 타입이라면 마커 인터페이스를 사용하라

* 마커 인터페이스(Marker Interface)란, 어떠한 메서드 선언도 없으며 단지 클래스를 표시하기 위해 사용되는 인터페이스이다.
```java
/*
 * ...
 * @author  unascribed
 * @see java.io.ObjectOutputStream
 * @see java.io.ObjectInputStream
 * @see java.io.ObjectOutput
 * @see java.io.ObjectInput
 * @see java.io.Externalizable
 * @since   1.1
 */
public interface Serializable {
}
```

#### 마커 어노테이션과 마커 인터페이스의 차이점

1. 마커 인터페이스는 타입을 정의할 수 있다.
* 타입의 정의는 곧 컴파일 타입의 안전성을 말한다.
* 자바의 `Serializable` 마커 인터페이스는 해당 타입이 직렬화 가능함(Serializable)을 알려준다.

2. 타겟을 정확하게 지정할 수 있다.
* 특정 인터페이스의 구현체에만 적용가능한 마커를 가지고 싶다면, 마커 인터페이스로 구현하면 된다.
* `java.util.Set` 인터페이스는 마커 인터페이스의 대표적인 예시이다.
