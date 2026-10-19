# (Effective Java Item 22) Java ‐ 인터페이스는 타입을 정의하는 용도로만 사용하라

#### 인터페이스는 타입을 정의하는 용도로만 사용하라

* 인터페이스는 자신을 구현한 클래스의 인스턴스를 참조할 수 있는 타입 역할을 한다.
* 인터페이스는 오직 이 용도로만 사용해야하는데 이 지침에 맞지 않는 예로 소위 상수 인터페이스가 있다.
* 상수 인터페이스란 메서드는 없이 상수를 뜻하는 static final 필드로만 가득 찬 인터페이스를 말한다.

```java
// Bad
public interface PhysicalConstants { 
    
    static final double AVOGADROS_NUMBER = ...

    static final double BOLTZMANN_CONSTANT = ...

    static final double ELECTRON_MASS = ...
}
```
* 상수 인터페이스 안티패턴은 인터페이스를 잘못 사용한 전형적인 예시이다.
* 이런 것이 오히려 사용자에게 혼란을 주기도 하며, 더 심하게는 클라이언트 코드가 내부 구현에 해당하는 이 상수들에 종속되게 된다.
* 다음 릴리스에서 더는 쓰지 않게 되더라도 바이너리 호환성을 위해 여전히 상수 인터페이스를 구현하고 있어야 한다.
* 상수를 공개할 목적이라면 합당한 선택지들이 있다.
  * 열거 타입으로 나타내기 적합한 상수라면 열거 타입으로 만들어 공개한다.
  * 그것도 아니라면 인스턴스화할 수 없는 유틸리티 클래스로 만들어 공개한다.
* 유틸리티 클래스의 경우 상수를 빈번히 사용한다면 정적 임포트하여 클래스 이름을 생략해 깔끔하게 사용할 수 있다.

```java
// Good
public class Test {

    double atoms(double mols) {
        return AVOGADROS_NUMBER * mols;
    }
}
```
