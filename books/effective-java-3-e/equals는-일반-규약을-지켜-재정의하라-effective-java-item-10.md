# (Effective Java Item 10) Java ‐ equals는 일반 규약을 지켜 재정의하라

#### equals는 일반 규약을 지켜 재정의하라

* 재정의할 필요가 없는 경우
  * 각 인스턴스가 본질적으로 고유하다.
    * 값을 표현하는게 아니라 동작하는 객체 자체를 표현하는 클래스가 여기에 해당되는데 대표적으로 Thread가 있다.
  * 인스턴스의 논리적 동치성을 검사할 일이 없다. 
  * 상위 클래스에서 재정의한 equals가 하위 클래스에도 들어맞는다.
  * 클래스가 `private`이거나 `package-private`이면 `equals()` 메서드를 호출할 일이 없다.

#### equals 메서드를 재정의할 때 반드시 일반 규약을 따라야 한다.

* 반사성 : `null`이 아닌 모든 참조 값 x에 대해, `x.equals(x)`는 true이다.
* 대칭성 : `null`이 아닌 모든 참조 값 x, y에 대해 `x.equals(y)`가 true이면 y.equals(x) 역시 true이다.
* 추이성 : `null`이 아닌 모든 참조 값 x, y, z에 대해 `x.equals(y)`가 true이고, `y.equals(z)`가 true라면, `x.equals(z)` 역시 true이다.
* 일관성 : `null`이 아닌 모든 참조 값 x, y에 대해 `x.equals(y)`를 반복해서 호출하면 항상 true 혹은 항상 false를 반환한다.
* `null`이 아님 : `null`이 아닌 모든 참조 값 x에 대해 `x.equals(null)`은 false이다.

#### equals 메서드 구현 방법은?

* `==` 연산자를 사용해 입력이 자기 자신의 참조인지 확인한다.
  * 자기 자신이라면 true를 반환한다.
  * 단순한 성능 최적화 용도이며, 비교 작업이 복잡한 상황에서 요긴하게 사용된다.
* `instanceof` 연산자로 입력이 올바른 타입인지 확인한다.
  * 입력과 호환되는 타입이 아니라면 false를 반환한다.
* 입력을 올바른 타입으로 형변환한다.
* 입력 객체와 자기 자신의 대응되는 핵심 필드들이 모두 일치하는지 하나씩 검사한다.
  * 모든 필드가 일치하면 true를, 하나라도 다르면 false를 반환한다.

```java
// Good
public final class PhoneNumber {
    private final short areaCode;
    private final short prefix;
    private final short lineNumber;

    public PhoneNumber(int areaCode, int prefix, int lineNumber) {
        this.areaCode = (short) areaCode;
        this.prefix = (short) prefix;
        this.lineNumber = (short) lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
    
        if (!(o instanceof PhoneNumber)) {
            return false;
        }
        PhoneNumber pn = (PhoneNumber) o;
        return pn.areaCode == areaCode && pn.prefix == prefix && pn.lineNumber == lineNumber;
    }

    @Override
    public int hashCode() {
        int result = Short.hashCode(areaCode);
        result = 31 * result + Short.hashCode(prefix);
        result = 31 * result + Short.hashCode(lineNumber);
        return result;
    }
}
```
* `equals`를 재정의할 땐 반드시 `hashCode`도 재정의해야한다.
* 재정의된 `equals()` 내부에서 필드들의 동치성만 검사해도 equals 규약을 지킬 수 있다.
* 꼭 필요한 경우가 아니라면 equals를 재정의하지 말자.
* **논리적 동치성 = 객체가 가지고 있는 값이 같은지!**
* **물리적 동치성 = 객체의 메모리 주소가 같은지!**
