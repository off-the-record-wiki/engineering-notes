# Spring Batch - SYSTEM EXECUTE

#### JobParameters란?

* JobParameters는 배치 작업에 전달되는 입력 값이다. 이 값은 배치가 어떤 조건에서 어떤 데이터를 다룰지를 결정한다.

#### 프로퍼티와 JobParameters의 결정적 차이

* 입력값 동적 변경
* 메타데이터 : JobParameters의 모든 값을 메타데이터 저장소에 기록하며 이를 통해 Job 인스턴스 식별 및 재시작 처리, Job 실행 이력 추적 등 중요한 기능을 제공한다.

#### JobParameters 전달하기 - 커맨드라인에서 잡 파라미터 전달하기

```
./gradlew bootRun --args='--spring.batch.job.name=dataProcessingJob inputFilePath=/data/input/users.csv,java.lang.String'
```

* `parameterName` : 배치 Job에서 파라미터를 찾을 때 사용할 Key 값이다. 이 이름으로 Job 내에서 파라미터에 접근할 수 있다.
* `parameterValue` : 파라미터 실제 값
* `parameterType` : 파라미터의 타입(`java.lang.String`)과 같은 fully qualified name 사용. 이 파라미터 타입을 명시하지 않을 경우 Spring Batch는 해당 파라미터를 `String` 타입으로 가정한다.
* `identificationFlag` : Spring Batch에게 해당 파라미터가 JobInstance 식별(identification)에 사용될 파라미터인지 여부를 전달하는 값으로 `true`이면 식별에 사용된다는 의미다. 이 플래그는 생략 가능하며 생략할 경우 `true`로 설정된다.

#### 다양한 타입의 Job 파라미터 - 기본 데이터 타입 파라미터 전달

```java
@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class ParameterJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Test
    void 문자열과_정수형_파라미터를_넘기면_이름을_횟수만큼_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("name", "batch")
                .addJobParameter("count", 3, Integer.class)
                .toJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut().lines().filter(line -> line.contains("Hello, batch ("))).hasSize(3);
    }

    @Test
    void 정수형_파라미터가_0이면_아무것도_출력하지_않고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("name", "batch")
                .addJobParameter("count", 0, Integer.class)
                .toJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).doesNotContain("Hello, ");
    }

    @Test
    void 문자열_파라미터가_없으면_Job이_시작되지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("count", 3, Integer.class)
                .toJobParameters();

        // when / then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class);
    }

    @Test
    void 정수형_파라미터가_없으면_Job이_시작되지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("name", "batch")
                .toJobParameters();

        // when / then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class);
    }

    @Test
    void 문자열_파라미터가_없으면_JobExecution이_만들어지지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("count", 3, Integer.class)
                .toJobParameters();

        // when
        catchThrowable(() -> jobOperatorTestUtils.startJob(parameters));

        // then
        assertThat(jobRepository.getLastJobExecution("parameterJob", parameters)).isNull();
    }

    @Test
    void 정수형_파라미터가_없으면_JobExecution이_만들어지지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("name", "batch")
                .toJobParameters();

        // when
        catchThrowable(() -> jobOperatorTestUtils.startJob(parameters));

        // then
        assertThat(jobRepository.getLastJobExecution("parameterJob", parameters)).isNull();
    }
}
```

#### 날짜와 시간 파라미터 전달(LocalDate와 LocalDateTime 파라미터 전달하기)

```java
@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class DateTimeJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    Job dateTimeJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(dateTimeJob);
    }

    @Test
    void 날짜와_날짜시간_파라미터를_넘기면_그_타입으로_처리해_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = dateTimeParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut())
                .contains("targetDate=2026-10-31, dayOfWeek=SATURDAY")
                .contains("requestedAt=2026-10-31T09:30:15, time=09:30:15");
    }

    @Test
    void 날짜와_날짜시간_파라미터는_타입을_유지한_채_저장된다() throws Exception {
        // given
        JobExecution execution = jobOperatorTestUtils.startJob(dateTimeParameters());

        // when
        JobParameters stored = jobRepository.getJobExecution(execution.getId()).getJobParameters();

        // then
        assertThat(stored.getParameter("targetDate").type()).isEqualTo(LocalDate.class);
        assertThat(stored.getParameter("requestedAt").type()).isEqualTo(LocalDateTime.class);
    }

    private JobParameters dateTimeParameters() {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("targetDate", LocalDate.of(2026, 10, 31), LocalDate.class)
                .addJobParameter("requestedAt", LocalDateTime.of(2026, 10, 31, 9, 30, 15), LocalDateTime.class)
                .toJobParameters();
    }
}
```

#### 열거형(Enum) 파라미터 전달

```java
@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class EnumJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    Job enumJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(enumJob);
    }

    @Test
    void 열거형_파라미터를_넘기면_열거형으로_처리해_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = expressParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("deliveryType=EXPRESS, leadDays=1");
    }

    @Test
    void 열거형_파라미터는_타입과_상수를_유지한_채_저장된다() throws Exception {
        // given
        JobExecution execution = jobOperatorTestUtils.startJob(expressParameters());

        // when
        JobParameter<?> stored = jobRepository.getJobExecution(execution.getId())
                .getJobParameters()
                .getParameter("deliveryType");

        // then
        assertThat(stored.type()).isEqualTo(DeliveryType.class);
        assertThat(stored.value()).isSameAs(DeliveryType.EXPRESS);
    }

    private JobParameters expressParameters() {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("deliveryType", DeliveryType.EXPRESS, DeliveryType.class)
                .toJobParameters();
    }
}
```

#### POJO를 활용한 Job 파라미터 주입

* POJO를 활용한 파라미터 관리는 코드를 더 체계적으로 구성할 수 있게 해준다. 특히 여러 개의 파라미터를 다루는 복잡한 배치 잡에서 이런 구조화의 효과가 나오게 된다.

```java
@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class PojoJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job pojoJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(pojoJob);
    }

    @Test
    void POJO로_주입받은_파라미터로_처리해_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = reportParameters("daily", LocalDate.of(2026, 10, 31), 50);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("reportName=daily, reportDate=2026-10-31, pageSize=50");
    }

    @Test
    void 연달아_실행하면_POJO에는_각_실행의_파라미터가_들어간다(CapturedOutput output) throws Exception {
        // given
        jobOperatorTestUtils.startJob(reportParameters("daily", LocalDate.of(2026, 10, 31), 50));

        // when
        jobOperatorTestUtils.startJob(reportParameters("weekly", LocalDate.of(2026, 11, 1), 10));

        // then
        assertThat(output.getOut()).contains("reportName=weekly, reportDate=2026-11-01, pageSize=10");
    }

    private JobParameters reportParameters(String reportName, LocalDate reportDate, Integer pageSize) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("reportName", reportName)
                .addJobParameter("reportDate", reportDate, LocalDate.class)
                .addJobParameter("pageSize", pageSize, Integer.class)
                .toJobParameters();
    }
}
```

>❗기본 파라미터 표기법의 한계 : 파라미터 값에 쉼표(,)가 포함되면 `Caused by: java.lang.ClassNotFoundException`가 발생한다. 이런 경우 JSON 기반 파라미터 표기법을 사용할 수 있다.

#### JobParameter의 JSON 기반 표기법

```java
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class JsonParameterTests {

    @Autowired
    JobOperator jobOperator;

    @Autowired
    JsonJobParametersReader jsonJobParametersReader;

    @Autowired
    Job parameterJob;

    @Test
    void JSON_표기법으로_넘기면_쉼표가_들어간_값으로_Job이_실행된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jsonJobParametersReader.read(jsonProperties());

        // when
        jobOperator.start(parameterJob, parameters);

        // then
        assertThat(output.getOut())
                .contains("Hello, batch, json (1/2)")
                .contains("Hello, batch, json (2/2)");
    }

    @Test
    void JSON_표기법은_쉼표가_들어간_값과_지정한_타입을_그대로_유지한다() {
        // given
        Properties properties = jsonProperties();

        // when
        JobParameters parameters = jsonJobParametersReader.read(properties);

        // then
        assertThat(parameters.getParameter("name").value()).isEqualTo("batch, json");
        assertThat(parameters.getParameter("count").type()).isEqualTo(Integer.class);
    }

    private Properties jsonProperties() {
        Properties properties = new Properties();
        properties.setProperty("name", "{\"value\":\"batch, json\",\"type\":\"java.lang.String\"}");
        properties.setProperty("count", "{\"value\":\"2\",\"type\":\"java.lang.Integer\"}");
        return properties;
    }
}
```

#### JobParameters, 코드로 직접 접근하기

![](https://github.com/user-attachments/assets/86acf670-5918-4633-acd9-08c55cde01f5)

* Spring Batch에서는 JobExecution이라는 곳에 Job의 실행 정보를 쥐고 있다. 결국 Step 안에서 JobParameter를 찾아내려면 이 JobExecution을 통해서 찾아야 한다.
* JobExecution은 Job의 실행과 관련된 모든 정보를 담고 있는 객체다. Job 실행 시점에 생성되어 잡의 실행 상태, JobParameters, 실행 결과 등을 포함한다.

```java
@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class DirectAccessJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job directAccessJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(directAccessJob);
    }

    @Test
    void 코드로_꺼낸_파라미터로_처리해_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = directParameters("kim", LocalDate.of(2026, 10, 31), 5L);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("userName=kim, targetDate=2026-10-31, limit=5");
    }

    @Test
    void limit을_넘기지_않으면_기본값_10으로_처리하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("userName", "kim")
                .addJobParameter("targetDate", LocalDate.of(2026, 10, 31), LocalDate.class)
                .toJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("userName=kim, targetDate=2026-10-31, limit=10");
    }

    @Test
    void 연달아_실행하면_스코프_없는_Tasklet도_각_실행의_파라미터를_읽는다(CapturedOutput output) throws Exception {
        // given
        jobOperatorTestUtils.startJob(directParameters("kim", LocalDate.of(2026, 10, 31), 5L));

        // when
        jobOperatorTestUtils.startJob(directParameters("lee", LocalDate.of(2026, 11, 1), 7L));

        // then
        assertThat(output.getOut()).contains("userName=lee, targetDate=2026-11-01, limit=7");
    }

    @Test
    void limit을_Integer_타입으로_넘기면_getLong이_타입_불일치로_Job이_실패한다() throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("userName", "kim")
                .addJobParameter("targetDate", LocalDate.of(2026, 10, 31), LocalDate.class)
                .addJobParameter("limit", 5, Integer.class)
                .toJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getExitStatus().getExitDescription()).contains("Key limit is not of type Long");
    }

    private JobParameters directParameters(String userName, LocalDate targetDate, Long limit) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("userName", userName)
                .addJobParameter("targetDate", targetDate, LocalDate.class)
                .addLong("limit", limit)
                .toJobParameters();
    }
}
```

#### JobParametersValidator

* JobParametersValidator를 사용하면 잘못된 파라미터가 들어오는 순간 즉시 차단할 수 있다.

```java
@Configuration
public class ValidatorJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ValidatorJobConfig.class);

    @Bean
    public Job validatorJob(JobRepository jobRepository, Step validatorStep) {
        return new JobBuilder("validatorJob", jobRepository)
                .validator(fileJobParametersValidator())
                .start(validatorStep)
                .build();
    }

    @Bean
    public Step validatorStep(
            JobRepository jobRepository, Tasklet validatorTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("validatorStep", jobRepository)
                .tasklet(validatorTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet validatorTasklet(
            @Value("#{jobParameters['fileName']}") String fileName,
            @Value("#{jobParameters['chunkSize']}") Long chunkSize) {
        return (contribution, chunkContext) -> {
            log.info("fileName={}, chunkSize={}", fileName, chunkSize);
            return RepeatStatus.FINISHED;
        };
    }

    private JobParametersValidator fileJobParametersValidator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(List.of(
                new DefaultJobParametersValidator(new String[] {"fileName", "chunkSize"}, new String[] {}),
                new FileJobParametersValidator()));
        return validator;
    }
}
```
```java
public class FileJobParametersValidator implements JobParametersValidator {

    private static final long MIN_CHUNK_SIZE = 1L;
    private static final long MAX_CHUNK_SIZE = 1000L;

    @Override
    public void validate(JobParameters parameters) throws InvalidJobParametersException {
        String fileName = parameters.getString("fileName");
        if (fileName == null || !fileName.endsWith(".csv")) {
            throw new InvalidJobParametersException("fileName must end with .csv: " + fileName);
        }

        JobParameter<?> chunkSize = parameters.getParameter("chunkSize");
        if (chunkSize == null || !chunkSize.type().equals(Long.class)) {
            throw new InvalidJobParametersException("chunkSize must be java.lang.Long");
        }
        long value = (Long) chunkSize.value();
        if (value < MIN_CHUNK_SIZE || value > MAX_CHUNK_SIZE) {
            throw new InvalidJobParametersException(
                    "chunkSize must be between " + MIN_CHUNK_SIZE + " and " + MAX_CHUNK_SIZE + ": " + value);
        }
    }
}
```

#### Job과 Step의 Scope

![](https://github.com/user-attachments/assets/c5e6339d-708c-42d8-9e30-46a558cf5a97)

* Spring Batch는 일반적인 Spring 애플리케이션의 기본 스코프인 싱글톤과는 다른 특별한 스코프를 제공한다. `JobScope`와 `StepScope`이다.
* `JobScope`와 `StepScope`가 선언된 빈은 애플리케이션 구동 시점에는 우선 프록시 객체로만 존재한다. `Job`이나 `Step`이 실행된 후에 프록시 객체에 접근을 시도하면 그 때 실제 빈이 생성된다.
* 각각의 스코프 빈은 `Job`과 `Step`의 실행(`Execution`) 시점에 생성되어 종료할 때 함께 소멸된다.
* 런타임에 결정되는 `JobParameters`를 실행 시점에 정확하게 주입받을 수 있고, 동시에 여러 `Job`이 실행되더라도 각각 독립적인 빈을 사용하게 되어 동시성 문제도 해결할 수 있다.
* 또한 `Job`이나 `Step`의 실행이 끝나면 해당 빈도 함께 제거되므로 불필요하게 메모리를 점유하지 않아 리소스 관리 측면에서도 효율적이다.

#### @JobScope

* `@JobScope`는 `Job`이 실행될 때 실제 빈이 생성되고 `Job`이 종료될 때 함께 제거되는 스코프이다. 즉, `JobExecution`과 생명주기를 같이 한다.
* 지연된 빈 생성(Lazy Bean Creation) : `@JobScope`가 적용된 빈은 애플리케이션 구동 시점에는 프록시 객체만 생성된다.
* `Job` 파라미터와의 연동 : `@JobScope`는 이런 동적 파라미터를 `Step` 빈 생성 시에 주입받을 수 있게 해준다. 만약 `@JobScope`가 없다면 `Step` 빈은 애플리케이션 구동 시점에 생성되므로 실행 시점에 전달되는 파라미터를 받을 수 없을 것이다.
* 병렬 처리 지원(Parallel Processing Support) : `@JobScope`를 사용하면 `JobExecution`마다 별도의 빈이 생성되어 동시성 문제를 피할 수 있다.

```java
/**
 * TL;DR: {@code @JobScope} 빈은 JobExecution 하나에 하나씩 만들어진다.
 * - firstStep: Step 빈에 {@code @JobScope} → 실행마다 jobParameters 를 새로 받는다.
 * - {@link VisitedSteps}: 같은 실행의 Step 들이 한 인스턴스를 공유하고, 다음 실행은 새 인스턴스로 시작한다.
 * - Job 실행 밖에서 쓰면 {@code ScopeNotActiveException}.
 */
@Configuration
public class JobScopeJobConfig {

    private static final Logger log = LoggerFactory.getLogger(JobScopeJobConfig.class);

    @Bean
    public Job jobScopeJob(JobRepository jobRepository, Step jobScopeFirstStep, Step jobScopeSecondStep) {
        return new JobBuilder("jobScopeJob", jobRepository)
                .start(jobScopeFirstStep)
                .next(jobScopeSecondStep)
                .build();
    }

    @Bean
    @JobScope
    public Step jobScopeFirstStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            VisitedSteps visitedSteps,
            @Value("#{jobParameters['message']}") String message) {
        return new StepBuilder("firstStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            visitedSteps.add("firstStep");
                            log.info("firstStep message={}", message);
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }

    @Bean
    public Step jobScopeSecondStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager, VisitedSteps visitedSteps) {
        return new StepBuilder("secondStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            visitedSteps.add("secondStep");
                            log.info("secondStep visitedSteps={}", visitedSteps.getSteps());
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }
}
```

#### @StepScope

* `@StepScope`는 `@JobScope`와 동일한 방식으로 동작하지만 적용 범위에서 차이가 있다. `@JobScope`가 `Job`의 실행 범위에서 빈을 관리한다면, `@StepScope`는 `Step`의 실행 범위에서 빈을 관리한다.

```java
/**
 * TL;DR: {@code @StepScope} 빈은 StepExecution 하나에 하나씩 만들어진다.
 * - stepScopeTasklet: 빈 정의 하나를 두 Step 이 쓰지만 Step 마다 새로 만들어져 {@code #{stepExecution.stepName}} 으로 자기 이름을 받는다.
 * - {@link StepRecorder}: Step 끼리 공유하지 않는다. secondStep 의 기록은 [secondStep] 뿐이다({@code @JobScope} 면 [firstStep, secondStep]).
 * - Step 실행 밖에서 쓰면 {@code ScopeNotActiveException}.
 */
@Configuration
public class StepScopeJobConfig {

    private static final Logger log = LoggerFactory.getLogger(StepScopeJobConfig.class);

    @Bean
    public Job stepScopeJob(JobRepository jobRepository, Step stepScopeFirstStep, Step stepScopeSecondStep) {
        return new JobBuilder("stepScopeJob", jobRepository)
                .start(stepScopeFirstStep)
                .next(stepScopeSecondStep)
                .build();
    }

    @Bean
    public Step stepScopeFirstStep(
            JobRepository jobRepository, Tasklet stepScopeTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("firstStep", jobRepository)
                .tasklet(stepScopeTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step stepScopeSecondStep(
            JobRepository jobRepository, Tasklet stepScopeTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("secondStep", jobRepository)
                .tasklet(stepScopeTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet stepScopeTasklet(
            StepRecorder stepRecorder, @Value("#{stepExecution.stepName}") String stepName) {
        return (contribution, chunkContext) -> {
            stepRecorder.record(stepName);
            log.info("stepName={}, recorded={}", stepName, stepRecorder.getRecorded());
            return RepeatStatus.FINISHED;
        };
    }
}
```

#### JobScope와 StepScope 사용 시 주의사항

1. 프록시 대상의 타입이 클래스인 경우 반드시 상속 가능한 클래스여야 한다.

```java
@Scope(value = "job", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobScope {}
```
* CGLIB를 사용해 클래스 기반의 프록시를 생성한다. 프록시를 생성하려면 대상 클래스가 상속 가능해야 한다.

2. Step 빈에는 `@StepScope`와 `@JobScope`를 사용하지 말라.

![](https://github.com/user-attachments/assets/52edbedc-2fd3-430a-8877-b6db4670c71e)

* Spring Batch Step에 `@StepScope`를 달면 Step 빈 생성과 스코프 활성화 시점이 맞지 않아 오류가 발생한다.
* Spring Batch는 Step 실행 전 메타데이터 관리를 위해 Step 빈에 접근한다. 문제는 이 시점에는 Step이 실행되지 않았기 때문에 `@StepScope`가 활성화되지 않았다는 것이다. 결국 스코프가 활성화되지 않은 상태에서 프록시에 접근하려 하니 문제가 발생하는 것이다. 마찬가지로 `@JobScope` 또한 Step 빈에 선언해서는 안 된다.

>❓Step에서 JobParameters를 어떻게 사용하면 좋은지?

* 여러 방법이 있지만 그 중 실무에서 사용했던 방법은 `@StepScope` + `@Value`이다.
* 값이 생성자 인자로 들어와서 타입이 분명하고 실행마다 새 인스턴스라 상태가 섞이지 않는다.
* Spring Batch 가 테스트 도구(StepScopeTestUtils)를 함께 주기도 한다.

#### ExecutionContext

* Spring Batch는 `JobExecution`과 `StepExecution`을 사용해 시작 시간, 종료 시간, 실행 상태 같은 메타데이터를 관리한다. 
* 하지만 이런 기본적인 실행 정보만으로는 시스템을 완벽하게 제어하기 부족할 때가 있다. 비즈니스 로직 처리 중에 발생하는 커스텀 데이터를 관리할 방법이 필요한데 이 때 사용하는 것이 바로 `ExecutionContext`라는 데이터 컨테이너다.
* `ExecutionContext`를 활용하면 커스텀 컬렉션의 마지막 처리 인덱스나 집계 중간 결과물 같은 데이터를 저장할 수 있다. 이는 Job이 중단된 후 재시작할 때 특히 유용하다. Spring Batch가 재시작 시 `ExecutionContext`의 데이터를 자동으로 복원하므로 중단된 지점부터 처리를 이어갈 수 있다.

```java
@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class ExecutionContextJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    Job executionContextJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(executionContextJob);
    }

    @Test
    void 다음_Step은_Job_컨텍스트에_직접_넣거나_승격한_값만_읽는다(CapturedOutput output) throws Exception {
        // given

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("directValue=direct, promotedValue=promoted, stepOnlyValue=null");
    }

    @Test
    void 저장된_Job_컨텍스트에는_직접_넣은_값과_승격한_값만_있다() throws Exception {
        // given
        JobExecution execution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // when
        ExecutionContext jobContext = jobRepository.getJobExecution(execution.getId()).getExecutionContext();

        // then
        assertThat(jobContext.get("directValue")).isEqualTo("direct");
        assertThat(jobContext.get("promotedValue")).isEqualTo("promoted");
        assertThat(jobContext.containsKey("stepOnlyValue")).isFalse();
    }

    @Test
    void 저장된_Step_컨텍스트에는_승격하지_않은_값이_남아_있다() throws Exception {
        // given
        JobExecution execution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // when
        StepExecution putStep = jobRepository.getJobExecution(execution.getId()).getStepExecutions().stream()
                .filter(stepExecution -> stepExecution.getStepName().equals("putStep"))
                .findFirst()
                .orElseThrow();

        // then
        assertThat(putStep.getExecutionContext().get("stepOnlyValue")).isEqualTo("stepOnly");
    }
}
```
