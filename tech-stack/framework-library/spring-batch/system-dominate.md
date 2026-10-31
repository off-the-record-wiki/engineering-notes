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

#### ChunkListener

* ChunkListener는 이러한 하나의 청크 단위 처리가 시작되기 전, 완료된 후, 그리고 실행 도중 에러가 발생했을 때 호출되는 리스너 인터페이스다. 
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
