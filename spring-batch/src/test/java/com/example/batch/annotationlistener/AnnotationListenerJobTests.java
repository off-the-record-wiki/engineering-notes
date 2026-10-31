package com.example.batch.annotationlistener;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.util.List;
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
class AnnotationListenerJobTests {

    private static final String PREFIX = "annotated: ";

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job annotationListenerJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(annotationListenerJob);
    }

    @Test
    void 어노테이션을_붙인_메서드가_Job과_Step_전후에_순서대로_불린다(CapturedOutput output) throws Exception {
        // given

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(annotatedEvents(output)).containsExactly(
                "beforeJob job=annotationListenerJob",
                "beforeStep step=annotationStep",
                "tasklet",
                "afterStep step=annotationStep, status=COMPLETED",
                "afterJob job=annotationListenerJob, status=COMPLETED");
    }

    @Test
    void Step이_실패해도_AfterStep과_AfterJob이_실패_상태로_불린다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("fail", true, Boolean.class)
                .toJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(annotatedEvents(output)).contains(
                "afterStep step=annotationStep, status=FAILED",
                "afterJob job=annotationListenerJob, status=FAILED");
    }

    private List<String> annotatedEvents(CapturedOutput output) {
        return output.getOut().lines()
                .filter(line -> line.contains(PREFIX))
                .map(line -> line.substring(line.indexOf(PREFIX) + PREFIX.length()))
                .toList();
    }
}