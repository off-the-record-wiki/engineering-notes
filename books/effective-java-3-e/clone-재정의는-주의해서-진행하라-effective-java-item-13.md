# (Effective Java Item 13) Java ‐ clone 재정의는 주의해서 진행하라

#### clone 재정의는 주의해서 진행하라

* `Cloneable`은 내부에 어떤 메서드도 정의하지 않는 마커 인터페이스이다.
* 일반적으로 인터페이스를 구현한다는 것은 해당 인터페이스 기능을 제공한다는 의미인데 `Cloneable` 인터페이스의 경우 상위 클래스인 Object의 protected 메서드인 `clone()` 동작 방식을 변경하라는 모순적인 의미를 담고 있다.
* Object의 `clone()` 메서드는 protected 접근 제어자로 선언되어 있어 `Cloneable` 인터페이스를 구현한다고 하더라도 외부에서 `clone()` 메서드를 호출할 수 없다. 결국 개발자가 직접 public으로 오버라이드해야 한다.

```java
/**
 * Cloneable 인터페이스를 구현한 클래스는 Object.clone() 메서드가 
 * 해당 객체의 필드-대-필드 복사를 수행해도 좋다는 것을 알리는 역할을 합니다.
 *
 * <p>Cloneable을 구현하지 않은 인스턴스에서 Object의 clone 메서드를 호출하면
 * CloneNotSupportedException이 발생합니다.
 *
 * <p>관례상 이 인터페이스를 구현하는 클래스는 public으로 Object.clone을 
 * 오버라이딩해야 합니다.
 *
 * <p>주의: 이 인터페이스는 정작 clone() 메서드를 포함하고 있지 않습니다.
 * 따라서 단지 인터페이스를 구현했다는 사실만으로는 객체를 복제할 수 없습니다.
 */
public interface Cloneable {

}
```

* `Object.clone()`은 기본적으로 얕은 복사를 수행한다. 객체 내부에 또 다른 객체를 참조하는 필드가 있을 경우, 해당 필드 참조만 복사될 뿐 실제 객체가 복사되지 않는다. 이로 인해 원본 객체와 복사본 객체가 같은 내부 객체를 공유하게 되면서 수정 시 사이드 이펙트가 발생할 수 있다.
* 완전히 독립적인 객체 복사를 원한다면 개발자가 직접 깊은 복사 로직을 구현해야 한다.
* Cloneable 인터페이스는 복잡하고 안전하지 않은 구조적 결함으로 인해 현대 자바 개발에서는 사실상 사장된 기술이다. 배열의 `clone()` 메서드처럼 특수한 경우를 제외하고는 복사 생성자와 같은 명시적이고 안전한 대안을 사용하는 것이 바람직하다.

> [📚Shallow copy & Deep copy](https://en.wikipedia.org/wiki/Object_copying)
>

| 구분 | 얕은 복사 (Shallow Copy) | 깊은 복사 (Deep Copy) |
| :--- | :--- | :--- |
| **핵심 개념** | 필드 값만 복사 (참조 주소값 유지) | 참조하는 실제 객체까지 모두 새로 생성 |
| **메모리** | 원본과 복사본이 동일한 내부 객체 공유 | 원본과 복사본이 완전히 독립된 메모리 점유 |
| **영향도** | 복사본 수정 시 원본의 내부 객체도 변경됨 | 원본과 복사본은 서로 전혀 영향을 주지 않음 |
| **성능** | 빠르고 메모리 사용량이 적음 | 느리고 재귀적 복사로 인한 비용 발생 |

>
> **1. 얕은 복사(Shallow Copy)**
> * 자바의 기본 `Object.clone()`이나 `ArrayList`의 생성자를 이용한 복사는 모두 얕은 복사이다.
>
> **2. 깊은 복사(Deep Copy)**
> * 데이터 무결성을 보장해야 하는 경우(원본 데이터 보존)에는 반드시 깊은 복사를 수행해야 한다.
> * 재귀적 복사 : 모든 가변 필드에 대해 새로운 객체를 생성하여 할당한다.
> * 복사 생성자/팩토리 : 클래스 내부에 직접 복사 로직을 구현한다.
> * 직렬화 : 객체를 직렬화했다가 다시 역직렬화하면 깊은 복사가 이루어지지만 성능이 매우 느리며 직렬화는 권장하지 않는다.
> * `Cloneable`을 구현하기보다 복사 생성자나 복사 팩토리를 직접 구현하는 것이 바람직하다.

```java
import java.util.ArrayList;
import java.util.List;

public class User {
    private String name;
    private List<String> roles; // 가변 객체(Mutable Object)

    public User(String name, List<String> roles) {
        this.name = name;
        this.roles = roles;
    }

    // [정석] 깊은 복사를 위한 복사 생성자
    public User(User other) {
        this.name = other.name; // String은 불변이므로 그대로 복사해도 안전
        
        // 핵심: 가변 필드인 리스트를 새로 생성하여 내용을 채움
        this.roles = new ArrayList<>(other.roles); 
    }

    // Getter, Setter 생략...
}
```
