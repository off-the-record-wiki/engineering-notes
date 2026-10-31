# spring-batch

Spring Batch 실습용 프로젝트 골격이다. Spring Boot 4.1.2, Java 25, MySQL 8.4 기준.

## 실행

Docker 가 켜져 있어야 한다. `compose.yaml` 의 MySQL 을 자동으로 띄운다.

```bash
./gradlew bootRun
```

Job 을 하나만 등록했을 때는 그 Job 이 실행된다. 여러 개를 등록했으면 `--args='--spring.batch.job.name=<Job 이름>'` 으로 고른다.

### parameterJob

문자열 파라미터 `name` 과 정수형 파라미터 `count` 를 받아 `Hello, <name>` 을 `count` 번 출력한다. 둘 다 필수다.
파라미터는 `이름=값,타입` 으로 넘기고, 타입을 생략하면 `String` 이다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=parameterJob name=batch count=3,java.lang.Integer'
```

같은 파라미터로 다시 실행하면 이미 완료된 JobInstance 라서 실행되지 않는다. 값을 바꿔서 실행한다.

필수 파라미터가 빠지면 Job 은 실행되지 않는다. 다만 Spring Batch 6 은 파라미터 검증 전에 JobInstance 를 만들기 때문에, 실행 기록이 없는 `BATCH_JOB_INSTANCE` 행이 남는다.

### dateTimeJob

날짜 파라미터 `targetDate`(`LocalDate`)와 날짜·시간 파라미터 `requestedAt`(`LocalDateTime`)을 받아 요일과 시각을 출력한다.
값은 `LocalDate` 는 `2026-10-31`, `LocalDateTime` 은 `2026-10-31T09:30:15` 형식(ISO)으로 넘긴다. 파라미터는 지정한 타입 그대로 `BATCH_JOB_EXECUTION_PARAMS` 에 저장된다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=dateTimeJob targetDate=2026-10-31,java.time.LocalDate requestedAt=2026-10-31T09:30:15,java.time.LocalDateTime'
```

Job 이 여러 개라서 `--spring.batch.job.name` 으로 실행할 Job 을 지정해야 한다.

### enumJob

열거형 파라미터 `deliveryType`(`DeliveryType`)을 받아 상수 이름과 배송 소요일(`leadDays()`)을 출력한다.
값은 상수 이름(`EXPRESS`), 타입은 enum 의 전체 클래스 이름으로 넘긴다. 중첩 enum 은 타입 이름에 `$` 가 들어가므로 최상위 enum 으로 둔다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=enumJob deliveryType=EXPRESS,com.example.batch.enumparam.DeliveryType'
```

### pojoJob

Job 파라미터를 POJO(`ReportJobParameters`) 하나로 모아 주입받는다. POJO 는 `@Component` + `@StepScope` 빈이고 필드에 `@Value("#{jobParameters['...']}")` 를 붙인다.
Tasklet 은 POJO 를 주입받아 getter 로 쓴다. 주입되는 것은 스코프 프록시라서 Tasklet 에는 `@StepScope` 가 없어도 실행마다 그 실행의 파라미터가 들어간다.
`@StepScope` 는 CGLIB 클래스 프록시를 만들기 때문에 final 인 record 는 쓸 수 없다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=pojoJob reportName=daily reportDate=2026-10-31,java.time.LocalDate pageSize=50,java.lang.Integer'
```

### directAccessJob

`@Value` 주입 없이 Tasklet 안에서 `JobParameters` 를 코드로 꺼내 쓴다. `@StepScope` 도 필요 없다.

```java
JobParameters parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
String userName = parameters.getString("userName");
LocalDate targetDate = parameters.getLocalDate("targetDate");
Long limit = parameters.getLong("limit", 10L);
```

- 스코프 없는 싱글턴 Tasklet 이어도 실행마다 그 실행의 `StepExecution` 에서 꺼내므로 값이 섞이지 않는다.
- 타입별 getter 는 넘긴 타입과 정확히 맞아야 한다. `getInteger` 는 없고, `limit=5,java.lang.Integer` 로 넘기면 `getLong` 이 `Key limit is not of type Long` 으로 실패해 Job 이 `FAILED` 가 된다. 그래서 `limit` 은 `java.lang.Long` 으로 넘긴다.
- `getLong(key, 기본값)` 처럼 기본값을 받는 오버로드는 키가 없을 때만 기본값을 쓴다. `limit` 을 생략하면 `10` 이다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=directAccessJob userName=kim targetDate=2026-10-31,java.time.LocalDate limit=5,java.lang.Long'
```

### validatorJob

`JobParametersValidator` 를 직접 구현해(`FileJobParametersValidator`) 파라미터 값을 검사한다. `CompositeJobParametersValidator` 로 필수 키 검사(`DefaultJobParametersValidator`)와 묶어 `JobBuilder.validator(...)` 에 등록한다. 검증기는 등록한 순서대로 돈다.

| 검사 | 검증기 | 실패 메시지 |
| --- | --- | --- |
| `fileName`, `chunkSize` 가 있어야 한다 | `DefaultJobParametersValidator` | `The JobParameters do not contain required keys: [...]` |
| `fileName` 은 `java.lang.String` 이어야 한다 | `FileJobParametersValidator` | `fileName must be java.lang.String` |
| `fileName` 은 `.csv` 로 끝나야 한다 | `FileJobParametersValidator` | `fileName must end with .csv: <값>` |
| `chunkSize` 는 `java.lang.Long` 이어야 한다 | `FileJobParametersValidator` | `chunkSize must be java.lang.Long` |
| `chunkSize` 는 1 이상 1000 이하여야 한다 | `FileJobParametersValidator` | `chunkSize must be between 1 and 1000: <값>` |

- 검증에 실패하면 `InvalidJobParametersException` 이 나고 Step 은 실행되지 않는다.
- 타입 검사는 `getString`·`getLong` 대신 `getParameter(...).type()` 으로 한다. 타입별 getter 는 타입이 다르면 `IllegalArgumentException` 을 던져서 검증 예외로 바뀌지 않는다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=validatorJob fileName=orders.csv chunkSize=100,java.lang.Long'
```

### JSON 표기법

`JsonJobParametersConverter` 를 감싼 별도 빈(`JsonJobParametersReader`, `JsonParameterConfig`)을 두고, 필요한 곳에서 명시적으로 주입받아 쓴다. Jackson 3(`tools.jackson.core:jackson-databind`)이 필요하다.
값은 `이름={"value":"...","type":"...","identifying":"true"}` 로 넘긴다. 기본 표기법(`이름=값,타입`)은 쉼표로 값과 타입을 나누기 때문에 쉼표가 들어간 값을 넘길 수 없지만, JSON 표기법은 넘길 수 있다.

```java
JobParameters parameters = jsonJobParametersReader.read(properties);
jobOperator.start(parameterJob, parameters);
```

- `JsonJobParametersConverter` 를 그대로 빈으로 등록하면 안 된다. `JobParametersConverter` 타입 빈은 Boot 의 명령행 실행기(`JobLauncherApplicationRunner`, `@Autowired` setter)와 `JobOperator` 에 자동 주입되어, 위의 `bootRun` 명령이 모두 `Unable to decode job parameter` 로 실패한다. 그래서 `JobParametersConverter` 가 아닌 클래스로 감싸서 등록한다.
- 이렇게 하면 `bootRun --args` 는 계속 기본 표기법을 읽고, JSON 은 코드에서 명시적으로 쓸 때만 쓰인다.
- `JobOperator.start(String, Properties)` 는 Spring Batch 6.0 에서 폐기 예정이라 쓰지 않는다.

## 검사

Testcontainers 로 MySQL 을 띄워 배치 메타 테이블이 생성되는지 확인한다.

```bash
./gradlew build
```