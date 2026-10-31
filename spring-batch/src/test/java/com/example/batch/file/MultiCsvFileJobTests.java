package com.example.batch.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
class MultiCsvFileJobTests {

    private static final String INPUT_HEADER = "orderId,customer,amount";

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job multiCsvFileJob;

    @TempDir
    Path tempDir;

    Path inputDir;

    Path outputFile;

    @BeforeEach
    void setUp() throws Exception {
        jobOperatorTestUtils.setJob(multiCsvFileJob);
        inputDir = Files.createDirectory(tempDir.resolve("input"));
        outputFile = tempDir.resolve("graded.csv");
    }

    @Test
    void 디렉터리의_CSV를_파일_이름_순서로_읽고_파일마다_헤더를_건너뛴다() throws Exception {
        // given
        Files.write(inputDir.resolve("orders-2.csv"), List.of(INPUT_HEADER, "3,park,8000"));
        Files.write(inputDir.resolve("orders-1.csv"), List.of(INPUT_HEADER, "1,kim,5000", "2,lee,12000"));

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(outputLines())
                .containsExactly(
                        "orderId,customer,amount,grade",
                        "1,kim,5000,NORMAL",
                        "2,lee,12000,VIP",
                        "3,park,8000,NORMAL",
                        "count=3,total=25000");
    }

    @Test
    void CSV가_하나도_없으면_실패하지_않고_헤더와_합계_줄만_쓴다() throws Exception {
        // given
        Files.write(inputDir.resolve("readme.txt"), List.of("not a csv"));

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(outputLines()).containsExactly("orderId,customer,amount,grade", "count=0,total=0");
    }

    private JobParameters parameters() {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("inputDir", inputDir.toString())
                .addString("outputFile", outputFile.toString())
                .toJobParameters();
    }

    private List<String> outputLines() throws IOException {
        assertThat(outputFile).exists();
        return Files.readAllLines(outputFile);
    }
}
