# (Effective Java Item 60) Java ‐ 정확한 답이 필요하다면 float와 double은 피하라

* `float`과 `double` 타입은 과학과 공학 계산용으로 설계되었다.
* 이진 부동소수점 연산에 쓰이며, 넓은 범위의 수를 빠르게 정밀한 "근사치"로 계산하도록 세심하게 설계되었다.
* 따라서, 정확한 결과가 필요한 경우라면 사용해선 안 된다.
* `float`와 `double` 타입은 금융 관련 계산과는 맞지 않는다.
* 금융 계산에서는 `BigDecimal`, `int` 혹은 `long`을 사용해야 한다.

#### 정확한 연산을 위한 방법 1 : BigDecimal

```java
@Test
void BigDecimal을_활용한_연산() {
    BigDecimal b1 = new BigDecimal("1.03");
    BigDecimal b2 = new BigDecimal("0.42");
    String result = b1.subtract(b2).toString();
    System.out.println(result);	//0.61
    assertThat(result).isEqualTo("0.61");
}
```
* `double`형 대신 `BigDecimal` 객체를 사용하면 실수 연산을 오차없이 수행할 수 있으나 사용하기 번거롭고 느리다는 점이 단점으로 꼽힌다.
* 코딩 시의 불편함이나 성능 저하를 신경 쓰지 않겠다면 `BigDecimal`을 사용하라.
* 반면 성능이 중요하고 소수점을 직접 추적할 수 있으며 숫자가 너무 크지 않다면 `int`나 `long`을 사용하자.
* 숫자를 아홉 자리 십진수로 표현할 수 있다면 `int`를 사용하고 열여덟 자리 십진수로 표현할 수 있다면 `long`을 사용하자.
* 열여덟 자리를 넘어가면 `BigDecimal`을 사용해야 한다.

#### 정확한 연산을 위한 방법 2 : 정수 타입을 이용

* 실수 타입을 정수 타입으로 사용하는 방법이다.
* 예를 들어, 0.1달러는 10센트로 표현할 수 있다. 이 방법은 정수 기본형을 사용하기 때문에 연산도 정확하고 사용하기도 편리하고 빠르다.
* 다만 모든 상황에서 사용할 수 있는 것은 아니다.
