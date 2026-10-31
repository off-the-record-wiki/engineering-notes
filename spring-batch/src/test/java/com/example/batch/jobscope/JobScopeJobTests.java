package com.example.batch.jobscope;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
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
class JobScopeJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job jobScopeJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(jobScopeJob);
    }

    @Test
    void 두_Step이_한_실행_안에서_같은_JobScope_빈을_함께_쓴다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = messageParameters("hello");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut())
                .contains("firstStep message=hello")
                .contains("secondStep visitedSteps=[firstStep, secondStep]");
    }

    @Test
    void 다시_실행하면_JobScope_Step은_새_파라미터로_만들어진다(CapturedOutput output) throws Exception {
        // given
        jobOperatorTestUtils.startJob(messageParameters("hello"));

        // when
        jobOperatorTestUtils.startJob(messageParameters("bye"));

        // then
        assertThat(output.getOut()).contains("firstStep message=bye");
    }

    @Test
    void 다시_실행하면_JobScope_빈은_이전_실행의_상태를_이어받지_않는다(CapturedOutput output) throws Exception {
        // given
        jobOperatorTestUtils.startJob(messageParameters("hello"));

        // when
        jobOperatorTestUtils.startJob(messageParameters("bye"));

        // then
        assertThat(output.getOut().lines().filter(line -> line.contains("visitedSteps=[firstStep, secondStep]")))
                .hasSize(2);
        assertThat(output.getOut()).doesNotContain("[firstStep, secondStep, firstStep");
    }

    private JobParameters messageParameters(String message) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("message", message)
                .toJobParameters();
    }
}