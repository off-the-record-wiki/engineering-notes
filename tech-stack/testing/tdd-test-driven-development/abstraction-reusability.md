# TDD - Abstraction & Reusability

#### 실패하지 않은 테스트

* 테스트로 전환하면 운영 코드를 변경하지 않아도 성공할 것으로 예상되는 테스트 시나리오는 Red-Green-Refactor 주기의 Red 단계에는 들어맞지 않는다.
* 이 때, 고려할 수 있는 선택지는 다음과 같이 2가지가 있다.
  * Red-Green-Refactor 주기에 적합하지 않기 때문에 해당 테스트 시나리오를 삭제한다.
  * Red-Green-Refactor 주기를 위배하고 성공하는 테스트를 추가한다.
* 해당 테스트 시나리오를 테스트로 전환하지 않더라도 불안하지 않다면 1번을 선택할 수 있다.
* 해당 테스트 시나리오를 테스트로 전환하지 않으면 미래의 코드 수정 과정에서 발생할 수 있는 버그가 우려되는 경우라면 2번을 선택할 수 있다.
* 예상하지 못했지만 작성한 테스트가 바로 성공하는 경우에도 적용 가능하다.

#### 테스트 데이터 재사용

* `@MethodSource` 어노테이션 : 정적 메서드를 사용해 `@ParameterizedTest` 어노테이션을 사용하는 테스트에 테스트 데이터 집합을 제공한다.

```java
@ParameterizedTest
@MethodSource("com.study.tdd.support.TestDataSource#invalidPasswords")
void password_속성이_올바른_형식을_따르지_않으면_400_Bad_Request_상태코드를_반환한다(
        String password,
        @Autowired TestRestTemplate client
) {
    // Arrange
    var command = new CreateShopperCommand(
            generateEmail(),
            generateUsername(),
            password
    );

    // Act
    ResponseEntity<Void> response = client.postForEntity(
            "/shopper/signUp",
            command,
            Void.class
    );

    // Assert
    assertThat(response.getStatusCode().value()).isEqualTo(400);
}
```
