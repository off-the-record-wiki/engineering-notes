package com.example.batch.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.batch.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
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
class ParameterJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Test
    void 문자열과_정수형_파라미터를_넘기면_이름을_횟수만큼_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("name", "batch")
                .addJobParameter("count", 3, Integer.class)
                .toJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut().lines().filter(line -> line.contains("Hello, batch ("))).hasSize(3);
    }

    @Test
    void 정수형_파라미터가_0이면_아무것도_출력하지_않고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("name", "batch")
                .addJobParameter("count", 0, Integer.class)
                .toJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).doesNotContain("Hello, ");
    }

    @Test
    void 문자열_파라미터가_없으면_Job이_시작되지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("count", 3, Integer.class)
                .toJobParameters();

        // when / then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class);
    }

    @Test
    void 정수형_파라미터가_없으면_Job이_시작되지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("name", "batch")
                .toJobParameters();

        // when / then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class);
    }

    @Test
    void 문자열_파라미터가_없으면_JobExecution이_만들어지지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("count", 3, Integer.class)
                .toJobParameters();

        // when
        catchThrowable(() -> jobOperatorTestUtils.startJob(parameters));

        // then
        assertThat(jobRepository.getLastJobExecution("parameterJob", parameters)).isNull();
    }

    @Test
    void 정수형_파라미터가_없으면_JobExecution이_만들어지지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("name", "batch")
                .toJobParameters();

        // when
        catchThrowable(() -> jobOperatorTestUtils.startJob(parameters));

        // then
        assertThat(jobRepository.getLastJobExecution("parameterJob", parameters)).isNull();
    }
}