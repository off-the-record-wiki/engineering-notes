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

### jobScopeJob

`@JobScope` 빈은 Job 실행(`JobExecution`) 하나에 하나씩 만들어진다. 그 실행의 모든 Step 이 같은 인스턴스를 쓰고, 다음 실행에서는 새로 만들어진다. `@StepScope` 는 Step 실행마다 새로 만든다.

- `firstStep` 은 Step 빈 자체에 `@JobScope` 를 붙여 `@Value("#{jobParameters['message']}")` 로 파라미터를 받는다. Step 빈이 실행마다 만들어지므로 다시 실행하면 새 값이 들어간다.
- `VisitedSteps` 는 `@Component @JobScope` 빈이다. 두 Step 이 자기 이름을 넣으면 `secondStep` 에서 `[firstStep, secondStep]` 이 보이고, 다음 실행은 빈 목록에서 다시 시작한다.
- Job 실행 밖에서 `@JobScope` 빈을 쓰면 `ScopeNotActiveException` 이 난다. `@SpringBatchTest` 는 테스트 메서드마다 Job 스코프를 열어 두므로(`JobScopeTestExecutionListener`), 이 동작은 `@SpringBatchTest` 없는 테스트(`JobScopeOutsideExecutionTests`)에서 확인한다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=jobScopeJob message=hello'
```

### stepScopeJob

`@StepScope` 빈은 Step 실행(`StepExecution`) 하나에 하나씩 만들어진다. `jobScopeJob` 과 같은 모양(두 Step + 상태를 담는 빈)이라 차이를 나란히 볼 수 있다.

| | `jobScopeJob`(`@JobScope`) | `stepScopeJob`(`@StepScope`) |
| --- | --- | --- |
| 인스턴스 단위 | Job 실행 하나 | Step 실행 하나 |
| `secondStep` 에서 본 기록 | `[firstStep, secondStep]` | `[secondStep]` |

- `stepScopeTasklet` 빈 정의 하나를 두 Step 이 함께 쓰지만, Step 마다 새로 만들어져 `@Value("#{stepExecution.stepName}")` 로 자기 Step 이름을 받는다.
- `StepRecorder` 는 `@Component @StepScope` 빈이라 Step 끼리 기록을 공유하지 않는다.
- Step 실행 밖에서 쓰면 `ScopeNotActiveException` 이 난다. `@SpringBatchTest` 는 테스트 메서드마다 Step 스코프도 열어 두므로(`StepScopeTestExecutionListener`), 이 동작은 `StepScopeOutsideExecutionTests` 에서 확인한다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=stepScopeJob run=1'
```

파라미터가 없으면 두 번째 실행부터 이미 완료된 JobInstance 라서 실행되지 않으므로, `run` 값을 바꿔 가며 실행한다.

### stepParameterJob

청크 지향 Step 에서 JobParameters 를 쓴다. 어디에 쓰느냐에 따라 붙이는 스코프가 다르다.

| 쓰는 곳 | 빈 | 스코프 | 파라미터 |
| --- | --- | --- | --- |
| Step 설정(청크 크기) | `stepParameterStep` | `@JobScope` | `chunkSize` → `.chunk(chunkSize)` |
| 읽을 데이터 범위 | `stepParameterReader`(`ListItemReader`) | `@StepScope` | `count` → 1..count |

- Step 빈은 Step 이 시작되기 전에 만들어지므로 `@StepScope` 가 아니라 `@JobScope` 를 붙인다. 스코프가 없으면 애플리케이션 시작 때 `jobParameters` 를 찾지 못해 실패한다.
- 두 빈 모두 실행마다 새로 만들어져서, 다음 실행에서 `chunkSize`·`count` 를 바꾸면 그대로 반영된다.
- Writer 는 청크마다 `chunk=[1, 2, 3]` 처럼 출력한다. `count=0` 이면 아무것도 쓰지 않고 완료된다.
- Spring Batch 6 에서는 `chunk(int, PlatformTransactionManager)` 가 폐기 예정이라 `chunk(int)` 뒤에 `.transactionManager(...)` 를 쓴다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=stepParameterJob count=7,java.lang.Long chunkSize=3,java.lang.Long'
```

### executionContextJob

ExecutionContext 는 실행 중에 만든 값을 담아 두는 저장소다. Job 실행마다 하나(`BATCH_JOB_EXECUTION_CONTEXT`), Step 실행마다 하나(`BATCH_STEP_EXECUTION_CONTEXT`)씩 있고 DB 에 저장된다. Job 파라미터는 실행 전에 정해지는 입력이고, ExecutionContext 는 실행 중에 쓰고 읽는다.

`putStep` 이 값 세 개를 서로 다른 방식으로 넣고, `readStep` 이 `@Value("#{jobExecutionContext['...']}")` 로 읽는다.

| 값 | 넣는 곳 | `readStep` 에서 | 저장되는 곳 |
| --- | --- | --- | --- |
| `directValue` | Job 컨텍스트에 직접(`stepExecution.getJobExecution().getExecutionContext()`) | `direct` | Job 컨텍스트 |
| `promotedValue` | Step 컨텍스트 + `ExecutionContextPromotionListener` | `promoted` | Step 컨텍스트와 Job 컨텍스트 |
| `stepOnlyValue` | Step 컨텍스트에만 | `null` | Step 컨텍스트 |

- 다음 Step 으로 값을 넘기려면 Job 컨텍스트에 있어야 한다. Step 컨텍스트에 넣은 값은 그 Step 에서만 보인다.
- `ExecutionContextPromotionListener` 는 Step 이 끝날 때(기본은 `COMPLETED` 일 때) 지정한 키를 Job 컨텍스트로 복사한다. Step 코드가 Job 컨텍스트를 몰라도 된다.
- `jobExecutionContext` 를 `@Value` 로 받는 빈에는 `@StepScope`(또는 `@JobScope`)가 필요하다.
- 테스트는 `JobRepository.getJobExecution(id)` 로 DB 에서 다시 읽어 저장된 내용을 확인한다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=executionContextJob run=1'
```

같은 `run` 값으로 다시 실행하면 이미 완료된 JobInstance 라서 실행되지 않으므로, `run` 값을 바꿔 가며 실행한다.

### listenerJob

`JobExecutionListener` 는 Job 이 시작되기 직전(`beforeJob`)과 끝난 직후(`afterJob`)에 불린다. `JobBuilder.listener(...)` 로 등록한다.

- `ReportPeriodListener.beforeJob` 은 `targetDate` 로 그 달 1일~`targetDate` 기간을 계산해 Job 컨텍스트(`startDate`, `endDate`)에 넣는다. Step 은 `#{jobExecutionContext['...']}` 로 읽기만 한다. 여러 Step 이 같은 가공 값을 써야 할 때 계산을 한 곳에 모으는 방법이다.
- `afterJob` 은 Job 이 실패해도 불린다(`AbstractJob` 의 `finally` 블록). `fail=true,java.lang.Boolean` 을 넘기면 Step 이 예외를 던지고 `afterJob status=FAILED` 가 출력된다.
- 인터페이스를 구현하는 대신 아무 클래스의 메서드에 `@BeforeJob`/`@AfterJob` 을 붙여 등록할 수도 있다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=listenerJob targetDate=2026-10-31,java.time.LocalDate'
```

### annotationListenerJob

리스너 인터페이스를 구현하지 않고, 일반 클래스(`AnnotatedBatchListener`)의 메서드에 `@BeforeJob`, `@AfterJob`, `@BeforeStep`, `@AfterStep` 을 붙여 리스너를 만든다.

| | 인터페이스 방식(`listenerJob`) | 어노테이션 방식(`annotationListenerJob`) |
| --- | --- | --- |
| 클래스 | `JobExecutionListener` 등을 `implements` | 아무것도 구현하지 않는 일반 클래스 |
| Job·Step 리스너를 한 클래스에 | 인터페이스를 여러 개 구현 | 어노테이션만 더 붙임 |
| 메서드 이름 | 인터페이스가 정함 | 자유 |
| 등록 | `listener(JobExecutionListener)` | `listener(Object)` — 빌더가 어노테이션 메서드를 찾아 리스너로 감싼다 |
| 시그니처 실수 | 컴파일 오류 | 실행 시점에야 드러남 |

- 같은 객체를 `JobBuilder.listener(...)` 와 `StepBuilder.listener(...)` 에 모두 등록해야 Job·Step 어노테이션이 각각 동작한다. Job 에만 등록하면 `@BeforeStep`/`@AfterStep` 은 불리지 않는다.
- `@AfterStep` 메서드의 시그니처는 `ExitStatus afterStep(StepExecution)` 이다. 반환한 값은 Step 의 종료 상태와 `ExitStatus.and(...)` 로 합쳐져 더 심각한 쪽이 남는다.
- `@AfterStep`, `@AfterJob` 은 실패해도 불린다. `fail=true,java.lang.Boolean` 을 넘기면 `status=FAILED` 로 출력된다.

```bash
./gradlew bootRun --args='--spring.batch.job.name=annotationListenerJob run=1'
```

같은 `run` 값으로 다시 실행하면 이미 완료된 JobInstance 라서 실행되지 않으므로, `run` 값을 바꿔 가며 실행한다.

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