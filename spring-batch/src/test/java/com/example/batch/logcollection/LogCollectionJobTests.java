package com.example.batch.logcollection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;

import com.example.batch.TestcontainersConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.SystemCommandException;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
@Import(TestcontainersConfiguration.class)
class LogCollectionJobTests {

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 11, 1);
    private static final String DATE = "2026-11-01";
    private static final String REPORT_FILE = "report-2026-11-01.txt";

    private static final List<String> SERVER_1_LOG = List.of(
            "2026-11-01 08:00:00 [INFO] 서버 기동",
            "2026-11-01 09:10:00 [WARN] 재시도 발생",
            "2026-11-01 23:10:05 [ERROR] 결제 승인 실패 orderId=1001",
            "2026-11-01 08:30:00 [INFO] 헬스체크 통과");

    private static final List<String> SERVER_2_LOG = List.of(
            "2026-11-01 10:00:00 [INFO] 로그인 성공",
            "2026-11-01 11:00:00 [ERROR] 타임아웃",
            "2026-11-01 12:00:00 [ERROR] DB 연결 실패");

    @Autowired
    JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    Job logCollectionJob;

    @TempDir
    Path tempDir;

    Path serversDir;

    Path workDir;

    Path reportDir;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob(logCollectionJob);
        serversDir = tempDir.resolve("servers");
        workDir = tempDir.resolve("work");
        reportDir = tempDir.resolve("report");
    }

    @Test
    void 여러_서버의_그날_로그를_모아_서버별_레벨별_건수를_보고서로_쓴다() throws Exception {
        // given
        writeLog("server-1", DATE, SERVER_1_LOG);
        writeLog("server-2", DATE, SERVER_2_LOG);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(0));
    }

    @Test
    void 다른_날짜의_로그_줄은_집계에_들어가지_않는다() throws Exception {
        // given
        writeLog("server-1", DATE, SERVER_1_LOG);
        writeLog("server-2", DATE, SERVER_2_LOG);
        writeLog("server-1", "2026-10-31", List.of("2026-10-31 23:59:59 [ERROR] 전날 오류", "2026-10-31 08:00:00 [INFO] 전날 정보"));

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(0));
    }

    @Test
    void 수집_단계는_서버별_그날_로그를_작업_폴더에_원본과_같은_내용으로_복사한다() throws Exception {
        // given
        writeLog("server-1", DATE, SERVER_1_LOG);
        writeLog("server-2", DATE, SERVER_2_LOG);

        // when
        jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(copied("server-1")).exists();
        assertThat(copied("server-2")).exists();
        assertThat(Files.readAllBytes(copied("server-1"))).isEqualTo(Files.readAllBytes(source("server-1")));
        assertThat(Files.readAllBytes(copied("server-2"))).isEqualTo(Files.readAllBytes(source("server-2")));
    }

    @Test
    void 그날_로그가_없는_서버는_보고서에서_빠지고_servers_수에도_세지_않는다() throws Exception {
        // given
        writeLog("server-1", DATE, SERVER_1_LOG);
        writeLog("server-2", DATE, SERVER_2_LOG);
        Files.createDirectories(serversDir.resolve("server-3"));

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(0));
    }

    @Test
    void 형식이_맞지_않는_줄은_건너뛰고_skipped에_센다() throws Exception {
        // given
        writeLog("server-1", DATE, plus(SERVER_1_LOG, "\tat com.example.Payment.approve(Payment.java:42)"));
        writeLog("server-2", DATE, SERVER_2_LOG);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(1));
    }

    @Test
    void 형식이_맞지_않는_줄이_한도_10개와_같으면_건너뛰고_완료한다() throws Exception {
        // given
        writeLog("server-1", DATE, plus(SERVER_1_LOG, brokenLines(10)));
        writeLog("server-2", DATE, SERVER_2_LOG);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(10));
    }

    @Test
    void 형식이_맞지_않는_줄이_한도_10개를_넘으면_실패하고_보고서를_남기지_않는다() throws Exception {
        // given
        writeLog("server-1", DATE, plus(SERVER_1_LOG, brokenLines(11)));
        writeLog("server-2", DATE, SERVER_2_LOG);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(reportDir.resolve(REPORT_FILE)).doesNotExist();
    }

    @Test
    void 어느_서버에도_그날_로그가_없으면_모든_값이_0인_보고서를_쓴다() throws Exception {
        // given
        writeLog("server-1", "2026-10-31", List.of("2026-10-31 08:00:00 [INFO] 전날 정보"));
        Files.createDirectories(serversDir.resolve("server-2"));

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines())
                .containsExactly(
                        "date=2026-11-01", "servers=0", "total=0", "skipped=0", "ALL INFO=0 WARN=0 ERROR=0");
    }

    @Test
    void 서버_디렉터리가_없으면_수집_단계가_실패하고_분석_단계는_실행되지_않는다() throws Exception {
        // given

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(execution.getStepExecutions())
                .extracting(StepExecution::getStepName, StepExecution::getStatus)
                .contains(tuple("collectLogsStep", BatchStatus.FAILED));
        StepExecution collect = execution.getStepExecutions().stream()
                .filter(step -> step.getStepName().equals("collectLogsStep"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("collectLogsStep 실행 기록이 없다"));
        assertThat(collect.getFailureExceptions())
                .anySatisfy(exception -> assertThat(exception).isInstanceOf(SystemCommandException.class));
        assertThat(execution.getStepExecutions())
                .extracting(StepExecution::getStepName)
                .doesNotContain("analyzeLogsStep");
    }

    @Test
    void 필수_파라미터_targetDate가_없으면_시작할_때_예외가_난다() {
        // given

        // when
        Throwable thrown = catchThrowable(() -> jobOperatorTestUtils.startJob(withPaths(jobOperatorTestUtils.getUniqueJobParametersBuilder()).toJobParameters()));

        // then
        assertThat(thrown).isInstanceOf(InvalidJobParametersException.class);
    }

    @Test
    void 같은_파라미터로_완료된_실행이_있으면_다시_시작할_때_예외가_난다() throws Exception {
        // given
        writeLog("server-1", DATE, SERVER_1_LOG);
        JobParameters parameters = fixedParameters().toJobParameters();
        JobExecution first = jobOperatorTestUtils.startJob(parameters);
        assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // when
        Throwable thrown = catchThrowable(() -> jobOperatorTestUtils.startJob(parameters));

        // then
        assertThat(thrown).isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    @Test
    void 보고서_쓰기가_실패한_뒤_파일을_지우고_재시작하면_완료되고_보고서가_남는다() throws Exception {
        // given
        writeLog("server-1", DATE, SERVER_1_LOG);
        writeLog("server-2", DATE, SERVER_2_LOG);
        Files.createFile(reportDir);
        JobParameters parameters = fixedParameters().toJobParameters();
        JobExecution first = jobOperatorTestUtils.startJob(parameters);
        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(first.getStepExecutions())
                .extracting(StepExecution::getStepName, StepExecution::getStatus)
                .contains(tuple("writeReportStep", BatchStatus.FAILED));
        Files.delete(reportDir);

        // when
        JobExecution second = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(0));
    }

    @Test
    void 분석_단계가_한도_초과로_실패한_뒤_깨진_줄을_지우고_재시작하면_완료되고_깨진_줄은_집계에_없다() throws Exception {
        // given
        List<String> normal = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> "2026-11-01 08:00:00 [INFO] 정상 처리 " + i)
                .toList();
        writeLog("server-1", DATE, plus(normal, brokenLines(11)));
        writeLog("server-2", DATE, SERVER_2_LOG);
        JobParameters parameters = fixedParameters().toJobParameters();
        JobExecution first = jobOperatorTestUtils.startJob(parameters);
        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(first.getStepExecutions())
                .extracting(StepExecution::getStepName, StepExecution::getStatus)
                .contains(tuple("analyzeLogsStep", BatchStatus.FAILED));
        writeLog("server-1", DATE, normal);

        // when
        JobExecution second = jobOperatorTestUtils.startJob(parameters);

        // then
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines())
                .containsExactly(
                        "date=2026-11-01",
                        "servers=2",
                        "total=28",
                        "skipped=0",
                        "ALL INFO=26 WARN=0 ERROR=2",
                        "server-1 INFO=25 WARN=0 ERROR=0",
                        "server-2 INFO=1 WARN=0 ERROR=2");
    }

    @Test
    void 작업_폴더에_지난_실행의_서버_로그가_남아_있어도_서버_디렉터리가_없으면_보고서에_나오지_않는다() throws Exception {
        // given
        writeLog("server-1", DATE, SERVER_1_LOG);
        writeLog("server-2", DATE, SERVER_2_LOG);
        Files.createDirectories(workDir.resolve(DATE));
        Files.write(workDir.resolve(DATE).resolve("server-9.log"),
                List.of("2026-11-01 07:00:00 [INFO] 지난 실행 기동", "2026-11-01 07:00:01 [ERROR] 지난 실행 오류"));

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(0));
    }

    @Test
    void 샵으로_시작하는_형식이_맞지_않는_줄도_건너뛰고_skipped에_센다() throws Exception {
        // given
        writeLog("server-1", DATE, plus(SERVER_1_LOG, "# A fatal error has been detected by the Java Runtime Environment:"));
        writeLog("server-2", DATE, SERVER_2_LOG);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(1));
    }

    @Test
    void 앞부분이_날짜와_시각_형식이_아닌_줄은_건너뛰고_skipped에_센다() throws Exception {
        // given
        writeLog("server-1", DATE, plus(SERVER_1_LOG, "upstream said: [ERROR] quota exceeded", "2026/11/01 10:00:00 [INFO] 다른 날짜 형식"));
        writeLog("server-2", DATE, SERVER_2_LOG);

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(2));
    }

    @Test
    void 작업_폴더_경로에_샵이_있어도_수집한_로그를_집계한다() throws Exception {
        // given
        writeLog("server-1", DATE, SERVER_1_LOG);
        writeLog("server-2", DATE, SERVER_2_LOG);
        workDir = tempDir.resolve("work#1");

        // when
        JobExecution execution = jobOperatorTestUtils.startJob(parameters().toJobParameters());

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(reportLines()).containsExactlyElementsOf(expectedReport(0));
    }

    private JobParametersBuilder parameters() {
        return withPaths(jobOperatorTestUtils.getUniqueJobParametersBuilder()
                .addJobParameter("targetDate", TARGET_DATE, LocalDate.class));
    }

    private JobParametersBuilder fixedParameters() {
        return withPaths(new JobParametersBuilder()
                .addJobParameter("targetDate", TARGET_DATE, LocalDate.class));
    }

    private JobParametersBuilder withPaths(JobParametersBuilder builder) {
        return builder.addString("serversDir", serversDir.toString())
                .addString("workDir", workDir.toString())
                .addString("reportDir", reportDir.toString());
    }

    private void writeLog(String server, String date, List<String> lines) throws IOException {
        Path dir = serversDir.resolve(server);
        Files.createDirectories(dir);
        Files.write(dir.resolve("app-" + date + ".log"), lines);
    }

    private Path source(String server) {
        return serversDir.resolve(server).resolve("app-" + DATE + ".log");
    }

    private Path copied(String server) {
        return workDir.resolve(DATE).resolve(server + ".log");
    }

    private List<String> reportLines() throws IOException {
        Path report = reportDir.resolve(REPORT_FILE);
        assertThat(report).exists();
        return Files.readAllLines(report);
    }

    private static List<String> expectedReport(int skipped) {
        return List.of(
                "date=2026-11-01",
                "servers=2",
                "total=7",
                "skipped=" + skipped,
                "ALL INFO=3 WARN=1 ERROR=3",
                "server-1 INFO=2 WARN=1 ERROR=1",
                "server-2 INFO=1 WARN=0 ERROR=2");
    }

    private static List<String> plus(List<String> lines, String... more) {
        List<String> result = new ArrayList<>(lines);
        result.addAll(List.of(more));
        return result;
    }

    private static List<String> plus(List<String> lines, List<String> more) {
        List<String> result = new ArrayList<>(lines);
        result.addAll(more);
        return result;
    }

    private static List<String> brokenLines(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> "\tat 깨진 스택 줄 " + i).toList();
    }
}