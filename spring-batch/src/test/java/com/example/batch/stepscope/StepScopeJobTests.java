package com.example.batch.stepscope;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
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
class StepScopeJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job stepScopeJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(stepScopeJob);
    }

    @Test
    void 같은_StepScope_빈_정의라도_Step마다_자기_Step_이름을_받은_새_인스턴스가_쓰인다(CapturedOutput output)
            throws Exception {
        // given

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut())
                .contains("stepName=firstStep, recorded=[firstStep]")
                .contains("stepName=secondStep, recorded=[secondStep]");
    }

    @Test
    void StepScope_빈은_다음_Step에_이전_Step의_상태를_넘기지_않는다(CapturedOutput output) throws Exception {
        // given

        // when
        jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // then
        assertThat(output.getOut()).doesNotContainPattern("recorded=\\[[^\\]]*,");
    }
}