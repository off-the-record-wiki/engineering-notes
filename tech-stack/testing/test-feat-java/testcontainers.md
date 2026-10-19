# Test - TestContainers

#### [TestContainer](https://testcontainers.com/)

* 테스트에서 도커 컨테이너를 실행할 수 있는 라이브러리
* 테스트 실행시 DB를 설정하거나 별도의 프로그램 또는 스크립트를 실행할 필요가 없다.
* 보다 Production에 가까운 테스트를 만들 수 있으나 테스트 실행 속도가 느려진다는 단점이 있다.

#### 컨테이너 정보를 스프링 테스트에서 참조하는 방법

* `@ContextConfiguration` : 스프링에서 제공하는 어노테이션으로 스프링 테스트 컨텍스트가 사용할 설정 파일 또는 컨텍스트를 커스터마이징할 수 있는 방법을 제공한다.
* `ApplicationContextInitializer` : 스프링 `ApplicationContext`를 프로그래밍으로 초기화 할 때, 사용할 수 있는 콜백 인터페이스로 특정 프로필을 활성화하거나 프로퍼티 소스를 추가하는 등의 작업을 할 수 있다.
* `TestPropertyValues` : 테스트용 프로퍼티 소스를 정의할 때 사용된다.
* `Environment` : 스프링 핵심 API로 프로퍼티와 프로파일을 담당한다.

---

1. `TestConainer`를 사용해서 컨테이너 생성
2. `ApplicationContextInitializer`를 구현해 생성된 컨테이너에서 정보를 추출해 `Environment`에 바인딩한다.
3. `@ContextConfiguration`을 사용해서 `ApplicationContextInitializer` 구현체를 등록한다.
4. 테스트 코드에서 `Environment`, `@Value`, `@ConfigurationProperties` 등의 다양한 방법으로 해당 프로퍼티를 사용할 수 있다.

---

```java
@Container
static PostgreSQLContainer container = new PostgreSQLContainer<>();

@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", container::getJdbcUrl);
    registry.add("spring.datasource.username", container::getUsername);
    registry.add("spring.datasource.password", container::getPassword);
}
```

#### TestContainer 기능

```gradle
// https://mvnrepository.com/artifact/org.testcontainers/testcontainers
testImplementation group: 'org.testcontainers', name: 'testcontainers', version: '1.21.2'
```

* `@TestContainers` : JUnit5 확장팩으로 테스트 클래스에 `@Container`를 사용한 필드를 찾아서 컨테이너 라이프 사이클 관련 메서드를 실행해준다.
* `@Container` : 인스턴스 필드에 사용하면 모든 테스트마다 컨테이너를 재시작하고, static 필드에 사용하면 클래스 내부 모든 테스트에서 동일한 컨테이너를 재사용한다.

```java
@Testcontainers
@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

	@Mock
	protected StudyRepository studyRepository;

	@Mock
	protected MemberService memberService;

	@Container
	static GenericContainer postgreSQLContainer = new GenericContainer("postgres:latest")
			.withEnv("POSTGRES_DB", "studytest")
			.withEnv("POSTGRES_PASSWORD", "testpass")
			.withExposedPorts(5432);

        // ...
        }
}
```

#### [TestContainers & Docker Compose](https://java.testcontainers.org/modules/docker_compose/)

```java
public ComposeContainer environment = new ComposeContainer(
    new File("src/test/resources/composev2/compose-test.yml")
)
    .withExposedService("redis-1", REDIS_PORT)
    .withExposedService("db-1", 3306);
```

* YAML로 관리하는 도커 컴포즈 파일을 위와 같이 관리를 할 수 있다.

>**❗회사에서 테스트 코드를 작성하면서 느낀 점**
> 
> * 서비스 로직은 단위 테스트로 충분히 커버 가능하나, 쿼리 로직을 효율적으로 테스트하기는 어렵다.
> * 금융 도메인처럼 데이터 정밀도가 중요한 경우, TestContainers 같은 통합 테스트 도구 도입도 고려할 가치가 있다.
> * 다만 Production 환경 완벽 재현을 목표로 하기보다는, 팀의 검증 전략과 실질적 리스크를 기준으로 신중히 트레이드오프를 판단해야 한다.
