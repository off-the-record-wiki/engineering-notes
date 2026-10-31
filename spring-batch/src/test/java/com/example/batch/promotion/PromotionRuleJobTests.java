package com.example.batch.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
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
class PromotionRuleJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job promotionRuleJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(promotionRuleJob);
    }

    @Test
    void 종료_코드가_EMPTY면_statuses에_EMPTY를_넣은_리스너만_승격한다(CapturedOutput output) throws Exception {
        // given

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(findStep(execution, "statusStep").orElseThrow().getExitStatus().getExitCode()).isEqualTo("EMPTY");
        assertThat(output.getOut()).contains("defaultKey=null, emptyAllowedKey=promoted");
    }

    @Test
    void strict_리스너가_키를_못_찾아도_예외는_로그로만_남고_Step은_완료된다(CapturedOutput output) throws Exception {
        // given

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // then
        StepExecution strictStep = findStep(execution, "strictStep").orElseThrow();
        assertThat(strictStep.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(strictStep.getExitStatus().getExitCode()).isEqualTo(ExitStatus.COMPLETED.getExitCode());
        assertThat(output.getOut())
                .contains("The key [strictKey] was not found")
                .contains("strictKey=null");
    }

    private Optional<StepExecution> findStep(JobExecution execution, String stepName) {
        return execution.getStepExecutions().stream()
                .filter(step -> step.getStepName().equals(stepName))
                .findFirst();
    }
}