package com.example.batch.logcollection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

public class DailyReportTasklet implements Tasklet {

    static final String ANALYZE_STEP_NAME = "analyzeLogsStep";

    private final LogStatistics logStatistics;
    private final LocalDate targetDate;
    private final Path reportDir;

    public DailyReportTasklet(LogStatistics logStatistics, LocalDate targetDate, String reportDir) {
        this.logStatistics = logStatistics;
        this.targetDate = targetDate;
        this.reportDir = Path.of(reportDir);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
        long skipped = analyzeSkipCount(chunkContext.getStepContext().getStepExecution());
        Files.createDirectories(reportDir);
        Files.write(reportDir.resolve("report-" + targetDate + ".txt"), reportLines(skipped), StandardCharsets.UTF_8);
        return RepeatStatus.FINISHED;
    }

    private long analyzeSkipCount(StepExecution current) {
        return current.getJobExecution().getStepExecutions().stream()
                .filter(step -> step.getStepName().equals(ANALYZE_STEP_NAME))
                .mapToLong(StepExecution::getReadSkipCount)
                .sum();
    }

    private List<String> reportLines(long skipped) {
        List<String> servers = logStatistics.servers();
        List<String> lines = new ArrayList<>();
        lines.add("date=" + targetDate);
        lines.add("servers=" + servers.size());
        lines.add("total=" + logStatistics.total());
        lines.add("skipped=" + skipped);
        lines.add("ALL " + levelCounts(logStatistics::total));
        for (String server : servers) {
            lines.add(server + " " + levelCounts(level -> logStatistics.count(server, level)));
        }
        return lines;
    }

    private static String levelCounts(ToLongFunction<String> counter) {
        return LogStatistics.LEVELS.stream()
                .map(level -> level + "=" + counter.applyAsLong(level))
                .collect(Collectors.joining(" "));
    }
}