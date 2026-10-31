# Spring Batch - SYSTEM BUILD

#### 태스크릿(Tasklet) 지향 처리

* 태스크릿(Tasklet) 지향 처리 모델은 Spring Batch에서 가장 기본적인 Step 구현 방식으로 비교적 복잡하지 않은 단순한 작업을 실행할 때 사용된다.
* 일반적인 Spring Batch의 Step은 대부분 대량 데이터 처리(읽고-처리하고-쓰기)하는 ETL 작업에 초점을 맞춘다.
* 하지만 때로는 단순한 시스템 작업이나 유틸성 작업이 필요할 때가 있다.
  * 매일 새벽 불필요한 로그 파일 삭제
  * 특정 디렉토리에서 오래된 파일을 아카이브
  * 사용자에게 단순한 알림 메시지 또는 이메일 발송
  * 외부 API 호출 후 결과를 단순히 저장하거나 로깅
* 이처럼 단일 비즈니스 로직 실행에 초점을 맞춘 작업들이 있다. 태스크릿 지향 처리는 바로 이러한 목적을 위해 설계된 처리 방식이다.

#### 태스크릿 지향 처리의 구현 방식

```java
@FunctionalInterface
public interface Tasklet {
    @Nullable
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception;
}
```

#### RepeatStatus의 두 얼굴: FINISHED vs CONTINUABLE

* `RepeatStatus.FINISHED` : Step의 처리가 성공이든 실패든 상관없이 해당 Step이 완료되었음을 의미한다. 더 이상 반복할 필요 없이 다음 스텝으로 넘어가며 배치 잡은 차근차근 진행된다.
* `RepeatStatus.CONTINUABLE` : `Tasklet`의 `execute()`메서드가 추가로 더 실행되어야 함을 Spring Batch Step에 알리는 신호이다. Step의 종료는 보류되고 필요한 만큼 `execute()`메서드가 반복 호출된다.

#### RepeatStatus가 필요한 이유: 짧은 트랜잭션을 활용한 안전한 배치 처리

![](https://github.com/user-attachments/assets/9b527891-f695-45b4-8373-893e8a1e06b3)

* `execute()` 내부에서 while문을 사용한다면 N만 건째 처리 중 예외가 발생했을 때, 이미 처리한 이전 데이터도 모두 롤백되어 하나도 정리되지 않은 상태로 돌아간다. 반면 `RepeatStatus.CONTINUABLE`로 반복한다면 매 만 건 처리마다 트랜잭션이 커밋되므로 예외가 발생하더라도 처리된 데이터는 이미 안전하게 정리된 상태로 남는다.
* 결국 `RepeatStatus`를 반환해 `execute()`를 반복 실행하도록 하는 이유는 거대한 하나의 트랜잭션 대신 작은 트랜잭션들로 나누어 안전하게 처리하기 위해서다.

#### ResourcelessTransactionManager

* `ResourcelessTransactionManager`는 `no-op(아무것도 하지 않는)` 방식으로 동작하는 `PlatformTransactionManager` 구현체로 이를 사용하면 불필요한 DB 트랜잭션 처리를 생략할 수 있다.
* `PlatformTransactionManager` 빈을 직접 정의할 땐 주의가 필요하다. Spring Batch는 내부적으로 Job, Step의 상태와 같은 메타데이터를 DB를 통해 관리한다. 이 때도 트랜잭션이 사용되는데 별도의 구성 변경 없이 `PlatformTransactionManager`를 빈으로 정의할 경우 Step의 비즈니스 로직 처리를 위한 트랜잭션과 메타데이터 관리를 위한 트랜잭션이 서로 다른 성격임에도 불구하고 같은 `PlatformTransactionManager` 빈을 사용하게 되어 의도치 않은 문제가 발생할 수 있다.

#### Chunk – 데이터를 작은 덩어리로 나누어 처리하는 방식

![](https://github.com/user-attachments/assets/b644ccab-3678-40ad-9ff2-ff47b8467906)

* 청크(Chunk)는 데이터를 일정 단위로 쪼갠 덩어리를 말한다. 읽고, 처리하고, 쓰는 작업을 일정 크기로 나눈 데이터 덩어리(청크)를 대상으로 하기 때문이다.
* 100만 건의 데이터를 한 번에 메모리로 불러오고 처리하고 저장한다면 전체 시스템이 다운되기에 청크라는 것이 존재한다.

1. 메모리를 지켜라 – 데이터 폭탄 방지

* 대용량 데이터를 한 번에 메모리에 올리는 것은 문제가 될 수 있다. DB에서 데이터를 불러오는 순간 메모리 과부하로 시스템이 뻗는다. 하지만 청크 방식은 나눠서 불러와서 메모리 사용량은 안정적이고 시스템은 무리 없이 데이터 처리를 이어갈 수 있다.

2. 가벼운 트랜잭션 – 작은 실패

* 트랜잭션은 작업 성공 또는 실패를 하나의 단위로 묶는 것이라고 볼 수 있다. 대용량 데이터 자체를 하나의 트랜잭션으로 처리할 경우 작업 중간에 오류가 발생하면 전체 데이터 모두가 롤백된다.

![](https://github.com/user-attachments/assets/6396ae01-92fc-47ce-8b59-390472ad1887)

* 청크 처리 과정에서 작업 중간에 에러가 발생하면 이전 청크들은 모두 커밋 완료가 되었고 에러가 발생한 청크만 롤백되고 해당 청크부터 재시작하면 된다.

#### 청크 지향 처리 - 읽고 깎고 쓴다

1. `ItemReader` - 데이터를 읽는다.

```java
public interface ItemReader<T> {
    T read() throws Exception, 
        UnexpectedInputException, 
        ParseException, 
        NonTransientResourceException;
}
```

* `read()` 메서드는 아이템을 하나씩 반환한다. 여기서 아이템이란 파일의 한 줄 또는 데이터베이스의 한 행(Row)에 해당하는 데이터 하나를 의미한다.
* `null`을 반환하면 Step의 종료 시점이라는 것을 의미한다. Spring Batch가 Step의 완료를 판단하는 조건이다.

2. `ItemProcessor` - 데이터를 가공한다.

```java
public interface ItemProcessor<I, O> {
    O process(I item) throws Exception;
}
```
* `process()` 메서드에서는 입력 데이터(I)를 원하는 형태(O)로 반환한다.
* `process()` 메서드가 `null`을 반환하면 입력 데이터가 처리 흐름에서 제외된다. 다시 말해 `ItemWriter`로 전달되지 않는다. 유효하지 않은 데이터나 처리할 필요가 없는 데이터를 걸러낼 때 사용한다.
* 입력 데이터의 유효성을 검사한다. 필터링과 달리 조건에 맞지 않는 데이터의 경우 예외를 발생시킨다. 예를 들어, 필수 필드 누락이나 잘못된 데이터 형식을 발견했을 때 예외를 던져 배치 잡을 중단시킨다.
* `ItemProcessor`는 생략 가능하다.

3. `ItemWriter` - 데이터를 반영한다.

```java
public interface ItemWriter<T> {
    void write(Chunk<? extends T> chunk) throws Exception;
}
```

* `ItemWriter`는 청크 단위로 묶어서 한 번에 데이터를 쓴다.

![](https://github.com/user-attachments/assets/3871d5f1-db08-4ec9-86c4-911611813d14)

#### 청크 지향 처리의 흐름

* 데이터 읽기 : `ItemReader`는 데이터 소스에서 하나씩 데이터를 읽어온다. `read()` 메서드가 호출될 때마다 데이터를 순차적으로 반환하며, 청크 크기만큼 데이터를 읽어야 끝난다. 다시 말해, 청크 크기가 10이면 `read()`가 10번 호출되어 하나의 청크가 생성된다.
* 데이터 깎기 : `ItemProcessor`는 `ItemReader`가 읽어온 청크의 각 아이템 하나씩을 처리한다.
* 데이터 쓰기 : `ItemProcessor`의 처리까지 완료되었다면 이제 청크를 실제로 쓸 차례다. `ItemReader` / `ItemProcessor`가 각각의 아이템을 하나씩 처리하는 것과 달리 `ItemWriter`는 청크 전체를 한 번에 입력 받는다.

#### 청크 처리와 트랜잭션

* 청크 단위로 트랜잭션이 시작되고 커밋된다는 것은 대용량 데이터를 처리하는 도중 중간에 스텝이 실패하더라도 이전 청크 반복에서 처리된 데이터는 이미 안전하게 커밋된다. 실패한 청크의 데이터만 롤백되므로 전체 데이터를 다시 처리할 필요가 없다.
* 적절한 청크 사이즈의 답은 없다. 청크 사이즈가 크면 그만큼 메모리에 많은 데이터를 한 번에 로드하게 된다. 트랜잭션의 경계가 커지므로 문제 발생시 롤백되는 데이터의 양이 증가한다. 반면 청크 사이즈가 작으면 트랜잭션 경계가 작아져 문제 발생시 롤백되는 데이터가 최소화된다. 대신 읽기/쓰기 I/O가 자주 발생하게 된다.

#### JobParameters란?

* `JobParameters`는 배치 작업에 전달되는 입력 값이다. 이 값은 배치가 어떤 조건에서 어떤 데이터를 다룰지를 결정한다.

#### 프로퍼티와 JobParameters의 결정적 차이

* 입력값 동적 변경
* 메타데이터 : `JobParameters`의 모든 값을 메타데이터 저장소에 기록하며 이를 통해 Job 인스턴스 식별 및 재시작 처리, Job 실행 이력 추적 등 중요한 기능을 제공한다.

#### JobParameters 전달하기 - 커맨드라인에서 잡 파라미터 전달하기

```
./gradlew bootRun --args='--spring.batch.job.name=dataProcessingJob inputFilePath=/data/input/users.csv,java.lang.String'
```

* `parameterName` : 배치 Job에서 파라미터를 찾을 때 사용할 Key 값이다. 이 이름으로 Job 내에서 파라미터에 접근할 수 있다.
* `parameterValue` : 파라미터 실제 값
* `parameterType` : 파라미터의 타입(`java.lang.String`)과 같은 fully qualified name 사용. 이 파라미터 타입을 명시하지 않을 경우 Spring Batch는 해당 파라미터를 `String` 타입으로 가정한다.
* `identificationFlag` : Spring Batch에게 해당 파라미터가 `JobInstance` 식별(identification)에 사용될 파라미터인지 여부를 전달하는 값으로 `true`이면 식별에 사용된다는 의미다. 이 플래그는 생략 가능하며 생략할 경우 `true`로 설정된다.

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

* Spring Batch에서는 `JobExecution`이라는 곳에 Job의 실행 정보를 쥐고 있다. 결국 Step 안에서 `JobParameter`를 찾아내려면 이 `JobExecution`을 통해서 찾아야 한다.
* `JobExecution`은 Job의 실행과 관련된 모든 정보를 담고 있는 객체다. Job 실행 시점에 생성되어 잡의 실행 상태, `JobParameters`, 실행 결과 등을 포함한다.

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

* `JobParametersValidator`를 사용하면 잘못된 파라미터가 들어오는 순간 즉시 차단할 수 있다.

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

>❓Step에서 `JobParameters`를 어떻게 사용하면 좋은지?

* 여러 방법이 있지만 그 중 실무에서 사용했던 방법은 `@StepScope` + `@Value`이다.
* 값이 생성자 인자로 들어와서 타입이 분명하고 실행마다 새 인스턴스라 상태가 섞이지 않는다.
* Spring Batch 가 테스트 도구(`StepScopeTestUtils`)를 함께 주기도 한다.

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

#### Spring Batch Listener

* 리스너는 배치 처리의 주요 순간들을 관찰하고 각 시점에 필요한 동작을 정의할 수 있는 강력한 도구이다.
* 즉, 배치 처리 중 발생하는 특정 이벤트를 감지하고 원하는 로직을 실행할 수 있게 해준다.
* Job 시작 전후, Step 실행 전후는 물론, 청크 단위 또는 아이템 단위 처리 시점까지 모든 과정에 개입할 수 있다. 이를 통해 로깅, 모니터링, 에러 처리 등 원하는 로직을 자유롭게 추가할 수 있다.

```java
@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class ListenerJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job listenerJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(listenerJob);
    }

    @Test
    void beforeJob이_가공한_기간을_Step이_읽고_afterJob이_완료_상태를_남긴다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = targetDateParameters(LocalDate.of(2026, 10, 31));

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut())
                .contains("period=2026-10-01~2026-10-31")
                .contains("afterJob status=COMPLETED");
    }

    @Test
    void beforeJob_Step_afterJob_순서로_실행된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = targetDateParameters(LocalDate.of(2026, 10, 31));

        // when
        jobOperatorTestUtils.startJob(parameters);

        // then
        String out = output.getOut();
        int before = out.indexOf("beforeJob targetDate=2026-10-31");
        int step = out.indexOf("period=");
        int after = out.indexOf("afterJob status=");
        assertThat(before).isNotNegative();
        assertThat(step).isGreaterThan(before);
        assertThat(after).isGreaterThan(step);
    }

    @Test
    void Step이_실패해도_afterJob이_실행되어_실패_상태를_남긴다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("targetDate", LocalDate.of(2026, 10, 31), LocalDate.class)
                .addJobParameter("fail", true, Boolean.class)
                .toJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(output.getOut()).contains("afterJob status=FAILED");
    }

    @Test
    void targetDate가_월_첫날이면_기간은_그날_하루다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = targetDateParameters(LocalDate.of(2026, 11, 1));

        // when
        jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(output.getOut()).contains("period=2026-11-01~2026-11-01");
    }

    private JobParameters targetDateParameters(LocalDate targetDate) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("targetDate", targetDate, LocalDate.class)
                .toJobParameters();
    }
}
```

#### JobExecutionListener

* `JobExecutionListener`는 Job 실행의 시작과 종료 시점에 호출되는 리스너 인터페이스이다.

```java
public interface JobExecutionListener {
    default void beforeJob(JobExecution jobExecution) { }
    default void afterJob(JobExecution jobExecution) { }
}
```
* `beforeJob()` : Job 실행 직전에 호출되며, Job 시작 전 필요한 리소스를 준비하는 등의 초기화 작업을 수행할 수 있다.
* `afterJob()` : Job 실행 후에 호출되어 Job 실행 결과를 이메일로 전송하거나 리소스를 정리하는 등의 후처리 작업을 수행할 수 있다. 특히 Job의 성공/실패 여부와 관계없이 무조건 호출되기 때문에 실패 시 알림을 보내거나 더 나아가 실패한 Job의 상태를 변경하는 작업까지 안전하게 수행할 수 있다.

#### StepExecutionListener

* `StepExecutionListener`는 Step 실행의 시작과 종료 시점에 호출되는 리스너 인터페이스다. 
* Step의 시작 시간, 종료 시간, 처리된 데이터 수를 로그로 기록하는 등의 사용자 정의 작업을 추가할 수 있다.

```java
public interface StepExecutionListener extends StepListener {
    default void beforeStep(StepExecution stepExecution) {
    }

    @Nullable
    default ExitStatus afterStep(StepExecution stepExecution) {
	return null;
    }
}
```
* `beforeStep()`: Step의 시작 시점, 정확히는 `StepScope`가 활성화된 직후에 호출된다. Step 실행 전 필요한 초기화 작업을 수행할 수 있다.
* `afterStep()` : Step 종료 시점에 호출된다. `JobExecutionListener`의 `afterJob()`과 마찬가지로 Step의 성공/실패 여부와 관계없이 무조건 호출되기 때문에 실패 처리나 리소스 정리 등 안전한 후처리 작업을 수행할 수 있다.

#### ChunkListener

* `ChunkListener`는 이러한 하나의 청크 단위 처리가 시작되기 전, 완료된 후, 그리고 실행 도중 에러가 발생했을 때 호출되는 리스너 인터페이스다. 
* 각 청크의 처리 현황을 모니터링하거나 로깅하는데 사용할 수 있다.

```java
public interface ChunkListener extends StepListener {
    default void beforeChunk(ChunkContext context) {
    }

    default void afterChunk(ChunkContext context) {
    }
	
    default void afterChunkError(ChunkContext context) {
    }
}
```
* `afterChunk()` : 트랜잭션이 커밋된 후에 호출된다. 
* `afterChunkError()` : 반면 청크 처리 도중 예외가 발생하면 호출되는 `afterChunkError()`는 청크 트랜잭션이 롤백된 이후에 호출된다.

#### Item[Read|Process|Write]Listener

![](https://github.com/user-attachments/assets/e274a65b-302a-48a2-b6c3-b2a53d39d264)

* `ItemReadListener`와 `ItemProcessListener`, 그리고 `ItemWriteListener`는 아이템의 읽기, 처리, 쓰기 작업이 수행되는 시점에 호출되는 리스너 인터페이스들이다. 
* 각 리스너의 메서드 이름에서 알 수 있듯이 아이템 단위의 처리 전후와 에러 발생 시점에 호출된다.

>❗리스너 활용: 시스템 장악 매뉴얼
* 단계별 모니터링과 추적 : 배치 작업의 모든 단계를 빠르게 파악할 수 있다. 각 Job과 Step의 실행 전후에 로그를 남길 수 있지. Step이 언제 시작하고, 언제 끝났는지, 몇 개의 데이터를 처리했는지까지 모든 것을 기록하고 추적할 수 있다.
* 실행 결과에 따른 후속 처리 : Job과 Step의 실행 상태를 리스너에서 직접 확인하고 그에 따른 조치를 할 수 있다. 예를 들어 `JobExecutionListener`의 `afterJob()` 메서드에서 Job의 종료 상태를 확인하고 종료 상태에 따른 후속 조치를 취할 수 있다.
* 데이터 가공과 전달 : 실제 처리 로직 전후에 데이터를 추가로 정제하거나 변환할 수 있다. `StepExecutionListener`나 `ChunkListener`를 사용하면 `ExecutionContext`의 데이터를 수정하거나 필요한 정보를 추가할 수 있다. 이를 통해 Step 간에 데이터를 전달하거나 다음 처리에 필요한 정보를 미리 준비할 수 있다.
* 부가 기능 분리 : 주요 처리 로직과 부가 로직을 깔끔하게 분리할 수 있다. 가령 `ChunkListener`에서 오류가 발생한 경우 `afterChunkError()` 메서드에서 관리자에게 알림 메일을 보내는 등의 부가적인 일은 여기서 처리할 수 있다.

#### 배치 리스너 구현 방법

1. 인터페이스 구현 - 가장 일반적인 리스너 구현 방법은 전용 리스너 인터페이스를 직접 구현한 우리만의 커스텀 리스너 클래스를 만드는 것이다.

```java
// 인터페이스 설계 후 각 커스텀 구현체에서 이를 구현한다.
public interface BatchProgressListener {

    default void onJobStarted(String jobName) {
    }

    default void onStepCompleted(String stepName, long readCount, long writeCount) {
    }

    default void onJobFinished(String jobName, BatchStatus status) {
    }
}
```

2. 어노테이션 기반 구현

* Spring Batch가 제공하는 `@BeforeJob`, `@AfterJob`, `@BeforeStep`, `@AfterStep`과 같은 리스너 특화 어노테이션을 사용하면 리스너 기능을 훨씬 간단하게 구현할 수 있다.

```java
public class AnnotatedBatchListener {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedBatchListener.class);

    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        log.info("annotated: beforeJob job={}", jobExecution.getJobInstance().getJobName());
    }

    @AfterJob
    public void afterJob(JobExecution jobExecution) {
        log.info("annotated: afterJob job={}, status={}",
                jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("annotated: beforeStep step={}", stepExecution.getStepName());
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("annotated: afterStep step={}, status={}", stepExecution.getStepName(), stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}
```

#### JobExecutionListener와 ExecutionContext를 활용한 동적 데이터 전달

* `JobParameters`만으로는 전달할 수 없는 동적인 데이터가 필요한 경우 `JobExecutionListener`의 `beforeJob()` 메서드를 활용하면 추가적인 동적 데이터를 각 Step에 전달할 수 있다.

```java
@Configuration
public class DynamicDataJobConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataJobConfig.class);

    @Bean
    public RegionDirectory regionDirectory() {
        return new RegionDirectory();
    }

    @Bean
    public Job dynamicDataJob(
            JobRepository jobRepository,
            RegionDirectory regionDirectory,
            Step prepareStep,
            Step dispatchStep,
            Step summaryStep) {
        return new JobBuilder("dynamicDataJob", jobRepository)
                .listener(new DispatchPlanListener(regionDirectory))
                .start(prepareStep)
                .next(dispatchStep)
                .next(summaryStep)
                .build();
    }

    @Bean
    public Step prepareStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet prepareTasklet) {
        return new StepBuilder("prepareStep", jobRepository)
                .tasklet(prepareTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet prepareTasklet(
            @Value("#{jobExecutionContext['label']}") String label,
            @Value("#{jobExecutionContext['regions']}") List<String> regions) {
        return (contribution, chunkContext) -> {
            log.info("prepare label={}, regions={}", label, regions);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step dispatchStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ListItemReader<String> dispatchReader) {
        return new StepBuilder("dispatchStep", jobRepository)
                .<String, String>chunk(2)
                .reader(dispatchReader)
                .writer(dispatchWriter())
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<String> dispatchReader(@Value("#{jobExecutionContext['regions']}") List<String> regions) {
        return new ListItemReader<>(regions);
    }

    private ItemWriter<String> dispatchWriter() {
        return chunk -> chunk.getItems().forEach(region -> log.info("dispatch region={}", region));
    }

    @Bean
    public Step summaryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("summaryStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            ExecutionContext jobContext = chunkContext.getStepContext()
                                    .getStepExecution()
                                    .getJobExecution()
                                    .getExecutionContext();
                            log.info(
                                    "summary label={}, regionCount={}",
                                    jobContext.getString("label"),
                                    jobContext.getInt("regionCount"));
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }
}
```

#### 왜 JobParameters가 아닌 ExecutionContext를 사용할까?

* Spring Batch의 핵심 철학 중 하나는 배치 작업의 재현 가능성(Repeatability)과 일관성(Consistency)을 보장하는 것에 있다.
* 이를 위해 `JobParameters`는 불변(immutable)하게 설계되었다.
  * 재현 가능성: 동일한 `JobParameters`로 실행한 Job은 항상 동일한 결과를 생성해야 한다. 실행 중간에 `JobParameters`가 변경되면 이를 보장할 수 없다.
  * 추적 가능성: 배치 작업의 실행 기록(`JobInstance`, `JobExecution`)과 `JobParameters`는 메타데이터 저장소에 저장된다. `JobParameters`가 변경 가능하다면 기록과 실제 작업의 불일치가 발생할 수 있다.
* 따라서 Job 실행 중에 동적으로 생성되거나 변경되어야 하는 데이터는 `ExecutionContext`를 통해 관리하는 것이 좋다.
* 하지만 Step 수준 `ExecutionContext`에 저장된 데이터는 해당 Step에서만 접근 가능하므로 다른 Step과 공유는 불가능하다. 그래서 한 Step의 `ExecutionContext`에 존재하는 데이터를 다음 Step에게 전달하려면 Step의 `ExecutionContext`의 값을 가져와 이를 Job 수준 `ExecutionContext`에 직접 설정해주어야 한다.
* 이런 불편한 문제를 알아서 처리할 수 있도록 `ExecutionContextPromotionListener`는 `StepExecutionListener` 구현체를 제공한다.
* `ExecutionContextPromotionListener`를 사용하면 번거로운 데이터 설정 작업 없이도 손쉽게 스텝 간 데이터 공유가 가능하다.

#### ExecutionContextPromotionListener를 활용한 Step 간 데이터 공유

* `ExecutionContextPromotionListener`는 Step 수준 `ExecutionContext`의 데이터를 Job 수준 `ExecutionContext`로 등록시켜주는 `StepExecutionListener`의 구현체다.
* Spring Batch에서는 Step 수준의 `ExecutionContext` 데이터를 Job 수준의 `ExecutionContext`로 옮기는 과정을 승격(Promote)이라 부른다. 그래서 이 리스너의 이름도 `ExecutionContextPromotion` + `Listener`인 것이다. 이 리스너는 `StepExecutionListener`의 `afterStep()` 메서드를 오버라이드하여 승격 작업을 수행한다.

```java
@Configuration
public class PromotionRuleJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PromotionRuleJobConfig.class);

    @Bean
    public Job promotionRuleJob(JobRepository jobRepository, Step statusStep, Step strictStep, Step ruleCheckStep) {
        return new JobBuilder("promotionRuleJob", jobRepository)
                .start(statusStep)
                .next(strictStep)
                .next(ruleCheckStep)
                .build();
    }

    @Bean
    public Step statusStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        ExecutionContextPromotionListener defaultStatuses = promotionListener("defaultKey");
        ExecutionContextPromotionListener emptyAllowed = promotionListener("emptyAllowedKey");
        emptyAllowed.setStatuses(new String[] {ExitStatus.COMPLETED.getExitCode(), "EMPTY"});

        return new StepBuilder("statusStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            ExecutionContext stepContext = contribution.getStepExecution().getExecutionContext();
                            stepContext.putString("defaultKey", "promoted");
                            stepContext.putString("emptyAllowedKey", "promoted");
                            contribution.setExitStatus(new ExitStatus("EMPTY"));
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .listener(defaultStatuses)
                .listener(emptyAllowed)
                .build();
    }

    @Bean
    public Step strictStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        ExecutionContextPromotionListener strict = promotionListener("strictKey");
        strict.setStrict(true);

        return new StepBuilder("strictStep", jobRepository)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
                .listener(strict)
                .build();
    }

    @Bean
    public Step ruleCheckStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet ruleCheckTasklet) {
        return new StepBuilder("ruleCheckStep", jobRepository)
                .tasklet(ruleCheckTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet ruleCheckTasklet(
            @Value("#{jobExecutionContext['defaultKey']}") String defaultKey,
            @Value("#{jobExecutionContext['emptyAllowedKey']}") String emptyAllowedKey,
            @Value("#{jobExecutionContext['strictKey']}") String strictKey) {
        return (contribution, chunkContext) -> {
            log.info("defaultKey={}, emptyAllowedKey={}, strictKey={}", defaultKey, emptyAllowedKey, strictKey);
            return RepeatStatus.FINISHED;
        };
    }

    private ExecutionContextPromotionListener promotionListener(String key) {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {key});
        return listener;
    }
}
```
