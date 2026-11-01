package com.example.batch.logcollection;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class LogStatisticsWriter implements ItemWriter<LogEntry> {

    private final LogStatistics logStatistics;

    public LogStatisticsWriter(LogStatistics logStatistics) {
        this.logStatistics = logStatistics;
    }

    @Override
    public void write(Chunk<? extends LogEntry> chunk) {
        for (LogEntry entry : chunk) {
            logStatistics.add(entry.getServer(), entry.getLevel());
        }
    }
}