package com.example.batch.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
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
class CsvFileJobTests {

    private static final String HEADER = "orderId,customer,amount,grade";

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job csvFileJob;

    @TempDir
    Path tempDir;

    Path inputFile;

    Path outputFile;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(csvFileJob);
        inputFile = tempDir.resolve("orders.csv");
        outputFile = tempDir.resolve("graded.csv");
    }

    @Test
    void 헤더와_주석을_건너뛰고_등급을_붙여_헤더와_합계_줄이_있는_CSV로_쓴다() throws Exception {
        // given
        writeInput("orderId,customer,amount", "# 10월 주문", "1,kim,5000", "2,lee,12000", "3,park,8000");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(outputLines())
                .containsExactly(
                        HEADER, "1,kim,5000,NORMAL", "2,lee,12000,VIP", "3,park,8000,NORMAL", "count=3,total=25000");
    }

    @Test
    void 헤더와_주석_줄은_읽은_건수에_들어가지_않는다() throws Exception {
        // given
        writeInput("orderId,customer,amount", "# 10월 주문", "1,kim,5000", "2,lee,12000", "3,park,8000");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStepExecutions()).singleElement().extracting(StepExecution::getReadCount).isEqualTo(3L);
    }

    @Test
    void 형식이_틀린_줄은_줄_번호와_원문을_남기고_건너뛴다(CapturedOutput output) throws Exception {
        // given
        writeInput(
                "orderId,customer,amount",
                "# 10월 주문",
                "1,kim,5000",
                "4,choi,abc",
                "2,lee,12000",
                "5,jung",
                "3,park,8000");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement().extracting(StepExecution::getReadSkipCount).isEqualTo(2L);
        assertThat(outputLines())
                .containsExactly(
                        HEADER, "1,kim,5000,NORMAL", "2,lee,12000,VIP", "3,park,8000,NORMAL", "count=3,total=25000");
        assertThat(output.getOut())
                .contains("skip line=4 input=4,choi,abc")
                .contains("skip line=6 input=5,jung");
    }

    @Test
    void 형식이_틀린_줄이_skip_한도와_같은_3개면_건너뛰고_완료한다() throws Exception {
        // given
        writeInput("orderId,customer,amount", "1,kim,abc", "2,lee,abc", "3,park,abc", "4,choi,1000");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions())
                .singleElement()
                .extracting(StepExecution::getReadSkipCount)
                .isEqualTo(3L);
    }

    @Test
    void 형식이_틀린_줄이_skip_한도를_넘으면_실패한다() throws Exception {
        // given
        writeInput("orderId,customer,amount", "1,kim,abc", "2,lee,abc", "3,park,abc", "4,choi,abc", "5,jung,1000");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    }

    @Test
    void 입력_파일이_없으면_기본_설정에서는_실패한다() throws Exception {
        // given

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getAllFailureExceptions())
                .anySatisfy(exception -> assertThat(exception).hasStackTraceContaining("Input resource must exist"));
    }

    @Test
    void 입력_파일이_없어도_allowMissing이면_0건으로_완료하고_헤더와_합계_줄만_쓴다() throws Exception {
        // given
        JobParametersBuilder parameters = parameters().addJobParameter("allowMissing", true, Boolean.class);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters.toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement().extracting(StepExecution::getReadCount).isEqualTo(0L);
        assertThat(outputLines()).containsExactly(HEADER, "count=0,total=0");
    }

    private void writeInput(String... lines) throws IOException {
        Files.write(inputFile, List.of(lines));
    }

    private JobParametersBuilder parameters() {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("inputFile", inputFile.toString())
                .addString("outputFile", outputFile.toString());
    }

    private List<String> outputLines() throws IOException {
        assertThat(outputFile).exists();
        return Files.readAllLines(outputFile);
    }
}
