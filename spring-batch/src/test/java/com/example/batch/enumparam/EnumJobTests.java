package com.example.batch.enumparam;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;
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
class EnumJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    Job enumJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(enumJob);
    }

    @Test
    void 열거형_파라미터를_넘기면_열거형으로_처리해_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = expressParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("deliveryType=EXPRESS, leadDays=1");
    }

    @Test
    void 열거형_파라미터는_타입과_상수를_유지한_채_저장된다() throws Exception {
        // given
        JobExecution execution = jobOperatorTestUtils.startJob(expressParameters());

        // when
        JobParameter<?> stored = jobRepository.getJobExecution(execution.getId())
                .getJobParameters()
                .getParameter("deliveryType");

        // then
        assertThat(stored.type()).isEqualTo(DeliveryType.class);
        assertThat(stored.value()).isSameAs(DeliveryType.EXPRESS);
    }

    private JobParameters expressParameters() {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("deliveryType", DeliveryType.EXPRESS, DeliveryType.class)
                .toJobParameters();
    }
}