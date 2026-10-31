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
