# (Effective Java Item 12) Java - toString을 항상 재정의하라

#### toString을 항상 재정의하라

* Object의 기본 `toString()` 메서드는 적합한 문자열을 반환하는 경우가 거의 없다.(기본적으로)
* 단순히 "클래스_이름@16진수"로 표현한 해시 코드를 반환할 뿐이다.
* `toString()`의 일반 규약에 따르면 '간결하면서 사람이 읽기 쉬운 형태의 유익한 정보'를 반환해야 한다.
* `equals()`, `hashCode()`만큼 중요한 것은 아니나 `toString()`을 잘 구현한 클래스는 사용하기 훨씬 편하고 그 클래스를 사용한 시스템은 디버깅하기가 쉽다.
* 실전에서 `toString()`은 그 객체가 가지는 주요 정보 모두를 반환하는 게 좋다.
* 또한 포맷을 명시하든 아니든 개발자 본인의 의도를 명확히 밝혀야 한다.

```java
// Good
// toString() 메서드 재정의
@Override
public String toString() {
    // 객체의 상태를 의미 있는 문자열로 반환
    return "Person{" +
           "name='" + name + '\'' +
           ", age=" + age +
           '}';
}
```
