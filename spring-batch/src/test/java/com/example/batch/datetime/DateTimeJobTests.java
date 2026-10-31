package com.example.batch.datetime;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
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
class DateTimeJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    Job dateTimeJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(dateTimeJob);
    }

    @Test
    void 날짜와_날짜시간_파라미터를_넘기면_그_타입으로_처리해_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = dateTimeParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut())
                .contains("targetDate=2026-10-31, dayOfWeek=SATURDAY")
                .contains("requestedAt=2026-10-31T09:30:15, time=09:30:15");
    }

    @Test
    void 날짜와_날짜시간_파라미터는_타입을_유지한_채_저장된다() throws Exception {
        // given
        JobExecution execution = jobOperatorTestUtils.startJob(dateTimeParameters());

        // when
        JobParameters stored = jobRepository.getJobExecution(execution.getId()).getJobParameters();

        // then
        assertThat(stored.getParameter("targetDate").type()).isEqualTo(LocalDate.class);
        assertThat(stored.getParameter("requestedAt").type()).isEqualTo(LocalDateTime.class);
    }

    private JobParameters dateTimeParameters() {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("targetDate", LocalDate.of(2026, 10, 31), LocalDate.class)
                .addJobParameter("requestedAt", LocalDateTime.of(2026, 10, 31, 9, 30, 15), LocalDateTime.class)
                .toJobParameters();
    }
}