package com.example.batch.dynamicdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.batch.TestcontainersConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
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
class DynamicDataJobTests {

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    Job dynamicDataJob;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(dynamicDataJob);
    }

    @Test
    void beforeJob이_만든_값을_세_Step이_순서대로_같은_값으로_읽는다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = groupParameters("south");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        String label = "dispatch-" + execution.getId();
        assertThat(output.getOut())
                .containsSubsequence(
                        "beforeJob group=south, regions=[BUSAN, DAEGU, ULSAN]",
                        "prepare label=" + label + ", regions=[BUSAN, DAEGU, ULSAN]",
                        "dispatch region=BUSAN",
                        "dispatch region=DAEGU",
                        "dispatch region=ULSAN",
                        "summary label=" + label + ", regionCount=3");
    }

    @Test
    void beforeJob이_조회한_목록이_청크_Step의_입력이_된다() throws Exception {
        // given
        JobParameters parameters = groupParameters("south");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        StepExecution dispatchStep = findStep(execution, "dispatchStep").orElseThrow();
        assertThat(dispatchStep.getReadCount()).isEqualTo(3);
        assertThat(dispatchStep.getWriteCount()).isEqualTo(3);
    }

    @Test
    void 같은_group으로_두_번_실행하면_label이_각_실행의_ID를_담아_서로_다르다() throws Exception {
        // given
        JobExecution first = jobOperatorTestUtils.startJob(groupParameters("south"));

        // when
        JobExecution second = jobOperatorTestUtils.startJob(groupParameters("south"));

        // then
        Object firstLabel = jobContext(first).get("label");
        Object secondLabel = jobContext(second).get("label");
        assertThat(firstLabel).isEqualTo("dispatch-" + first.getId());
        assertThat(secondLabel).isEqualTo("dispatch-" + second.getId());
        assertThat(firstLabel).isNotEqualTo(secondLabel);
    }

    @Test
    void beforeJob이_넣은_값이_Job_컨텍스트로_DB에_저장된다() throws Exception {
        // given
        JobParameters parameters = groupParameters("south");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        ExecutionContext context = jobContext(execution);
        assertThat(context.get("regions")).isEqualTo(List.of("BUSAN", "DAEGU", "ULSAN"));
        assertThat(context.getInt("regionCount")).isEqualTo(3);
        assertThat(context.getString("label")).isEqualTo("dispatch-" + execution.getId());
    }

    @Test
    void 목록에_없는_group이면_빈_목록으로_모든_Step이_완료된다(CapturedOutput output) throws Exception {
        // given
        JobParameters parameters = groupParameters("unknown");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(findStep(execution, "dispatchStep").orElseThrow().getReadCount()).isZero();
        assertThat(output.getOut()).contains("summary label=dispatch-" + execution.getId() + ", regionCount=0");
    }

    @Test
    void group_파라미터가_없으면_beforeJob이_실패해_Step이_하나도_실행되지_않는다() throws Exception {
        // given
        JobParameters parameters = jobOperatorTestUtils.getUniqueJobParameters();

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getStepExecutions()).isEmpty();
    }

    private JobParameters groupParameters(String group) {
        return jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addString("group", group)
                .toJobParameters();
    }

    private ExecutionContext jobContext(JobExecution execution) {
        return jobRepository.getJobExecution(execution.getId()).getExecutionContext();
    }

    private Optional<StepExecution> findStep(JobExecution execution, String stepName) {
        return execution.getStepExecutions().stream()
                .filter(step -> step.getStepName().equals(stepName))
                .findFirst();
    }
}