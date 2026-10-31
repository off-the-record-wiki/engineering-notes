# Spring Batch - SYSTEM DOMINATE

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
* `beforeStep()`: Step의 시작 시점, 정확히는 StepScope가 활성화된 직후에 호출된다. Step 실행 전 필요한 초기화 작업을 수행할 수 있다.
* `afterStep()` : Step 종료 시점에 호출된다. JobExecutionListener의 `afterJob()`과 마찬가지로 Step의 성공/실패 여부와 관계없이 무조건 호출되기 때문에 실패 처리나 리소스 정리 등 안전한 후처리 작업을 수행할 수 있다.
