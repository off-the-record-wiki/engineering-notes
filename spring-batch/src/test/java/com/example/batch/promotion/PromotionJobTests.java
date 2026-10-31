package com.example.batch.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class PromotionJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    Job promotionJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(promotionJob);
    }

    @Test
    void 청크_여러_개에_걸쳐_누적한_집계값을_다음_Step이_읽는다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = amountsParameters("1000;2500;500");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("report orderCount=3, totalAmount=4000");
    }

    @Test
    void 완료된_Step의_집계값이_Job_컨텍스트로_승격되어_저장된다() throws Exception {
        // given
        JobExecution execution = jobOperatorTestUtils.startJob(amountsParameters("1000;2500;500"));

        // when
        ExecutionContext jobContext = savedJobContext(execution);

        // then
        assertThat(jobContext.get("orderCount")).isEqualTo(3);
        assertThat(jobContext.get("totalAmount")).isEqualTo(4000L);
    }

    @Test
    void 집계_Step이_실패하면_값이_승격되지_않고_다음_Step도_실행되지_않는다() throws Exception {
        // given
        JobParameters parameters = amountsParameters("1000;2500;-1");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .doesNotContain("reportStep");
        ExecutionContext jobContext = savedJobContext(execution);
        assertThat(jobContext.containsKey("orderCount")).isFalse();
        assertThat(jobContext.containsKey("totalAmount")).isFalse();
    }

    @Test
    void 실패한_Step의_컨텍스트에는_커밋된_청크까지의_집계값이_남는다() throws Exception {
        // given
        JobExecution execution = jobOperatorTestUtils.startJob(amountsParameters("1000;2500;-1"));

        // when
        StepExecution aggregateStep = savedStep(execution, "aggregateStep").orElseThrow();

        // then
        assertThat(aggregateStep.getExecutionContext().get("orderCount")).isEqualTo(2);
        assertThat(aggregateStep.getExecutionContext().get("totalAmount")).isEqualTo(3500L);
    }

    private JobParameters amountsParameters(String amounts) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("amounts", amounts)
                .toJobParameters();
    }

    private ExecutionContext savedJobContext(JobExecution execution) {
        return jobRepository.getJobExecution(execution.getId()).getExecutionContext();
    }

    private Optional<StepExecution> savedStep(JobExecution execution, String stepName) {
        return jobRepository.getJobExecution(execution.getId()).getStepExecutions().stream()
                .filter(step -> step.getStepName().equals(stepName))
                .findFirst();
    }
}