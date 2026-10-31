package com.example.batch.direct;

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