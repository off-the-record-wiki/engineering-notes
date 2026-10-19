# (Effective Java Item 49) Java ‐ 매개변수가 유효한지 검사하라

* 메서드와 생성자 대부분은 입력 매개변수의 값이 특정 조건을 만족하길 바란다.
* 예컨대 인덱스 값은 음수이면 안되며, 객체 참조는 `null`이 아니어야 하는 것이 있다.
* 이런 제약은 반드시 문서화를 해야 하며 메서드 몸체가 시작되기 전에 검사해야 한다.
* 오류는 가능한 한 빨리 잡아야 한다는 일반 원칙의 한 사례이기도 하다.
* 오류를 발생한 즉시 잡지 못하면 해당 오류는 감지하기 어려워지고, 감지하더라도 오류 발생 지점을 찾기 어려워진다.
* 매개변수의 제약을 문서화한다면 그 제약을 어겼을 때 발생하는 예외도 함께 기술해야 한다.

```java
/**
 * (현재 값 mod m) 값을 반환한다. 이 메서드는
 * 항상 음이 아닌 BigInteger를 반환하다는 점에서 remainder 메서드와 다르다.
 * 
 * @param m 계수(양수여야 한다.)
 * @return 현재 값 mod m
 * @throws ArithmeticException m이 0보다 작거나 같으면 발생한다.
 */
public BigInteger mod(BigInteger m) {
    if (m.signum() <= 0) {
        throw new ArithmeticException("계수(m)는 양수여야 합니다." + m);
    // .. 계산 로직
}
```

* 자바 7에 추가된 `java.util.Objects.requireNonNull` 메서드는 유연하고 사용하기도 편하니, 더 이상 null 검사를 수동으로 하지 않아도 된다.

```java
@ForceInline
public static <T> T requireNonNull(T obj) {
    if (obj == null)
        throw new NullPointerException();
    return obj;
}
```
