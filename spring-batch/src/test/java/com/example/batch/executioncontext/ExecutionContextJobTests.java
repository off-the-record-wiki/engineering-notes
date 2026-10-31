package com.example.batch.executioncontext;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
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