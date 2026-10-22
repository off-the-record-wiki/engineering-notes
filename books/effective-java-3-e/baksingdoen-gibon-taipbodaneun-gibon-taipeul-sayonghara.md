# (Effective Java Item 61) Java ‐ 박싱된 기본 타입보다는 기본 타입을 사용하라

* 자바의 데이터 타입은 크게 Primitive Type, Reference Type 두 가지로 나눌 수 있다.
* 각각의 기본 타입에는 대응하는 참조 타입이 하나씩 있으며, 이를 박싱된 기본 타입이라고 한다.
* 예컨대 `int`, `double`, `boolean`에 대응되는 박싱된 기본 타입은 `Integer`, `Double`, `Boolean` 등이 있다.
* 오토 박싱과 오토 언박싱 덕분에 두 타입을 크게 구분하지 않고 사용할 수 있지만 차이가 사라지는 것이 절대 아니다.

#### 기본 타입과 박싱된 기본 타입의 차이점
* 기본 타입은 값만 가지고 있으나 참조 타입은 값에 더해 식별성(Identity)이란 속성을 가진다.
  * 박싱된 기본 타입 즉, 참조 타입의 두 인스턴스는 값이 같아도 서로 다르다고 식별될 수 있다.
* 기본 타입의 값은 언제나 유효하나, 박싱된 기본 타입은 유효하지 않은 값 즉, `null`을 가질 수 있다.
* 기본 타입이 박싱된 기본 타입보다 시간과 메모리 사용 측면에서 더 효율적이다.

```java
// Bad
public static void main(String[] args) {
    Long sum = 0L;
    for (long i=0; i<=Integer.MAX_VALUE; i++) {
        sum += i;  // 오토 박싱과 오토 언박싱 발생! -> 성능 저하로 이어진다.
    }
    System.out.println(sum);
}
```
