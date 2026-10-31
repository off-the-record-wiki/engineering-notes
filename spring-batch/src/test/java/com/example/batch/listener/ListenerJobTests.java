package com.example.batch.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.time.LocalDate;
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