# (Effective Java Item 11) Java ‐ equals를 재정의하려거든 hashCode도 재정의하라

#### equals를 재정의하려거든 hashCode도 재정의하라

* `equals`를 재정의한 클래스 모두에서 `hashCode()` 메서드도 재정의해야 한다.
* Object 명세에서 `equals`는 기본적으로 물리적 동치성을 기준으로 비교한다.
* `equals(Object)`가 두 객체를 같다고 판단했다면 두 객체의 `hashCode`는 똑같은 값을 반환해야 한다.
* `hashCode` 재정의를 잘못하게 되면 문제가 발생한다. 논리적 동치성의 결과가 true인 객체는 같은 해시 코드를 반환해야 한다.
* `equals`, `hashCode`를 직접 재정의하기보다는 IDE에게 맡기는 것이 더 정확하다.

```java
// equals() 메서드 재정의
@Override
public boolean equals(Object o) {
    // 1. 자기 자신과의 비교
    // 두 객체의 참조값이 같으면(메모리 주소가 같으면) 당연히 true
    if (this == o) {
        return true;
    }

    // 2. null 체크 및 타입 비교
    // 비교 대상이 null이거나, 현재 클래스와 다른 타입이면 false
    if (o == null || getClass() != o.getClass()) {
        return false;
    }

    // 3. 필드 값 비교
    // Object 타입으로 전달된 객체를 Person 타입으로 캐스팅합니다.
    Person person = (Person) o;
    return age == person.age && Objects.equals(name, person.name);
}

// hashCode() 메서드 재정의
@Override
public int hashCode() {
    // Objects.hash()를 사용하여 필드들의 해시 코드를 조합하여 반환합니다.
    // equals()에서 사용한 필드들과 동일한 필드를 사용해야 합니다.
    return Objects.hash(name, age);
}
```
