package com.example.batch.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.batch.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
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
class ValidatorJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job validatorJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(validatorJob);
    }

    @Test
    void 검증을_통과한_파라미터로_처리해_출력하고_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = fileParameters("orders.csv", 100L);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(output.getOut()).contains("fileName=orders.csv, chunkSize=100");
    }

    @Test
    void chunkSize가_없으면_필수_키_검사에서_Job이_시작되지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("fileName", "orders.csv")
                .toJobParameters();

        // when, then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessage("The JobParameters do not contain required keys: [chunkSize]");
    }

    @Test
    void fileName이_csv로_끝나지_않으면_Job이_시작되지_않는다() {
        // given
        JobParameters parameters = fileParameters("orders.txt", 100L);

        // when, then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessage("fileName must end with .csv: orders.txt");
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 1000L})
    void chunkSize가_허용_범위의_경계값이면_완료된다(long chunkSize) throws Exception {
        // given
        JobParameters parameters = fileParameters("orders.csv", chunkSize);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1001L})
    void chunkSize가_허용_범위를_벗어나면_Job이_시작되지_않는다(long chunkSize) {
        // given
        JobParameters parameters = fileParameters("orders.csv", chunkSize);

        // when, then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessage("chunkSize must be between 1 and 1000: " + chunkSize);
    }

    @Test
    void chunkSize를_Integer_타입으로_넘기면_검증_예외로_Job이_시작되지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("fileName", "orders.csv")
                .addJobParameter("chunkSize", 100, Integer.class)
                .toJobParameters();

        // when, then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessage("chunkSize must be java.lang.Long");
    }

    @Test
    void fileName을_String이_아닌_타입으로_넘기면_검증_예외로_Job이_시작되지_않는다() {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addLong("fileName", 1L)
                .addLong("chunkSize", 100L)
                .toJobParameters();

        // when, then
        assertThatThrownBy(() -> jobOperatorTestUtils.startJob(parameters))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessage("fileName must be java.lang.String");
    }

    private JobParameters fileParameters(String fileName, Long chunkSize) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("fileName", fileName)
                .addLong("chunkSize", chunkSize)
                .toJobParameters();
    }
}