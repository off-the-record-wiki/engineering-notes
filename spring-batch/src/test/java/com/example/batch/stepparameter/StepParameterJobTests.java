package com.example.batch.stepparameter;

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
class StepParameterJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job stepParameterJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(stepParameterJob);
    }

    @Test
    void count만큼_읽어_chunkSize_단위로_나눠_쓰고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = stepParameters(7L, 3L);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut())
                .contains("chunk=[1, 2, 3]")
                .contains("chunk=[4, 5, 6]")
                .contains("chunk=[7]");
    }

    @Test
    void 다시_실행하면_Step과_Reader가_새_파라미터로_만들어진다(CapturedOutput output) throws Exception {
        // given
        jobOperatorTestUtils.startJob(stepParameters(7L, 3L));

        // when
        jobOperatorTestUtils.startJob(stepParameters(4L, 2L));

        // then
        assertThat(output.getOut())
                .contains("chunk=[1, 2]")
                .contains("chunk=[3, 4]");
    }

    @Test
    void count가_0이면_아무것도_쓰지_않고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = stepParameters(0L, 3L);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions().iterator().next().getWriteCount()).isZero();
        assertThat(output.getOut()).doesNotContain("chunk=");
    }

    private JobParameters stepParameters(Long count, Long chunkSize) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addLong("count", count)
                .addLong("chunkSize", chunkSize)
                .toJobParameters();
    }
}