package com.example.batch.logcollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.SimpleSystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.MultiResourceItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.RegexLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class LogCollectionJobConfig {

    private static final int CHUNK_SIZE = 10;
    private static final long SKIP_LIMIT = 10;
    private static final long COMMAND_TIMEOUT_MILLIS = 10_000;
    private static final String LOG_LINE_REGEX = "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) \\[(INFO|WARN|ERROR)\\] (.*)$";
    private static final String COLLECT_SCRIPT = """
            [ -d "$1" ] || exit 2
            { [ -n "$2" ] && [ -d "$2" ]; } || exit 3
            rm -f -- "$2"/*.log || exit 1
            for dir in "$1"/*/; do
              [ -d "$dir" ] || continue
              name=$(basename "$dir")
              if [ -f "${dir}app-$3.log" ]; then
                cp "${dir}app-$3.log" "$2/$name.log" || exit 1
              fi
            done
            exit 0
            """;

    @Bean
    public Job logCollectionJob(
            JobRepository jobRepository,
            Step createWorkDirStep,
            Step collectLogsStep,
            Step analyzeLogsStep,
            Step writeReportStep) {
        return new JobBuilder("logCollectionJob", jobRepository)
                .validator(new DefaultJobParametersValidator(
                        new String[] {"targetDate", "serversDir", "workDir", "reportDir"}, new String[] {}))
                .start(createWorkDirStep)
                .next(collectLogsStep)
                .next(analyzeLogsStep)
                .next(writeReportStep)
                .build();
    }

    @Bean
    public Step createWorkDirStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SystemCommandTasklet createWorkDirTasklet) {
        return new StepBuilder("createWorkDirStep", jobRepository)
                .tasklet(createWorkDirTasklet, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step collectLogsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SystemCommandTasklet collectLogsTasklet) {
        return new StepBuilder("collectLogsStep", jobRepository)
                .tasklet(collectLogsTasklet, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step analyzeLogsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            MultiResourceItemReader<LogEntry> logEntryReader,
            LogStatisticsWriter logStatisticsWriter) {
        return new StepBuilder("analyzeLogsStep", jobRepository)
                .<LogEntry, LogEntry>chunk(CHUNK_SIZE)
                .reader(logEntryReader)
                .writer(logStatisticsWriter)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(SKIP_LIMIT)
                .transactionManager(transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    public Step writeReportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DailyReportTasklet dailyReportTasklet) {
        return new StepBuilder("writeReportStep", jobRepository)
                .tasklet(dailyReportTasklet, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }

    @Bean
    @StepScope
    public SystemCommandTasklet createWorkDirTasklet(
            JobRepository jobRepository,
            @Value("#{jobParameters['workDir']}") String workDir,
            @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
        return systemCommandTasklet(jobRepository, "mkdir", "-p", workDir + "/" + targetDate);
    }

    @Bean
    @StepScope
    public SystemCommandTasklet collectLogsTasklet(
            JobRepository jobRepository,
            @Value("#{jobParameters['serversDir']}") String serversDir,
            @Value("#{jobParameters['workDir']}") String workDir,
            @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
        String date = targetDate.toString();
        return systemCommandTasklet(
                jobRepository, "sh", "-c", COLLECT_SCRIPT, "sh", serversDir, workDir + "/" + date, date);
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<LogEntry> logEntryReader(
            @Value("#{jobParameters['workDir']}") String workDir,
            @Value("#{jobParameters['targetDate']}") LocalDate targetDate)
            throws IOException {
        Path collectDir = Path.of(workDir, targetDate.toString());
        Resource[] resources = new Resource[0];
        if (Files.isDirectory(collectDir)) {
            try (Stream<Path> files = Files.list(collectDir)) {
                resources = files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".log"))
                        .sorted()
                        .map(FileSystemResource::new)
                        .toArray(Resource[]::new);
            }
        }
        return new MultiResourceItemReaderBuilder<LogEntry>()
                .name("logEntryReader")
                .resources(resources)
                .delegate(logEntryDelegate())
                .saveState(false)
                .build();
    }

    @Bean
    @JobScope
    public LogStatistics logStatistics() {
        return new LogStatistics();
    }

    @Bean
    public LogStatisticsWriter logStatisticsWriter(LogStatistics logStatistics) {
        return new LogStatisticsWriter(logStatistics);
    }

    @Bean
    @StepScope
    public DailyReportTasklet dailyReportTasklet(
            LogStatistics logStatistics,
            @Value("#{jobParameters['targetDate']}") LocalDate targetDate,
            @Value("#{jobParameters['reportDir']}") String reportDir) {
        return new DailyReportTasklet(logStatistics, targetDate, reportDir);
    }

    private static FlatFileItemReader<LogEntry> logEntryDelegate() {
        RegexLineTokenizer tokenizer = new RegexLineTokenizer(LOG_LINE_REGEX);
        tokenizer.setNames("timestamp", "level", "message");
        return new FlatFileItemReaderBuilder<LogEntry>()
                .name("logEntryDelegate")
                .saveState(false)
                .encoding("UTF-8")
                .comments(new String[0])
                .lineTokenizer(tokenizer)
                .targetType(LogEntry.class)
                .build();
    }

    private static SystemCommandTasklet systemCommandTasklet(JobRepository jobRepository, String... command) {
        SystemCommandTasklet tasklet = new SystemCommandTasklet();
        tasklet.setCommand(command);
        tasklet.setTimeout(COMMAND_TIMEOUT_MILLIS);
        tasklet.setJobRepository(jobRepository);
        tasklet.setTaskExecutor(new SimpleAsyncTaskExecutor());
        tasklet.setSystemProcessExitCodeMapper(new SimpleSystemProcessExitCodeMapper());
        return tasklet;
    }
}