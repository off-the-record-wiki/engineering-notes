package com.example.batch.pojo;

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
class PojoJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job pojoJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(pojoJob);
    }

    @Test
    void POJO로_주입받은_파라미터로_처리해_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = reportParameters("daily", LocalDate.of(2026, 10, 31), 50);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("reportName=daily, reportDate=2026-10-31, pageSize=50");
    }

    @Test
    void 연달아_실행하면_POJO에는_각_실행의_파라미터가_들어간다(CapturedOutput output) throws Exception {
        // given
        jobOperatorTestUtils.startJob(reportParameters("daily", LocalDate.of(2026, 10, 31), 50));

        // when
        jobOperatorTestUtils.startJob(reportParameters("weekly", LocalDate.of(2026, 11, 1), 10));

        // then
        assertThat(output.getOut()).contains("reportName=weekly, reportDate=2026-11-01, pageSize=10");
    }

    private JobParameters reportParameters(String reportName, LocalDate reportDate, Integer pageSize) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("reportName", reportName)
                .addJobParameter("reportDate", reportDate, LocalDate.class)
                .addJobParameter("pageSize", pageSize, Integer.class)
                .toJobParameters();
    }
}