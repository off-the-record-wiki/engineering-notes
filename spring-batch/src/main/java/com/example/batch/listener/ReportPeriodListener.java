package com.example.batch.listener;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.infrastructure.item.ExecutionContext;

public class ReportPeriodListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ReportPeriodListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        LocalDate targetDate = jobExecution.getJobParameters().getLocalDate("targetDate");
        log.info("beforeJob targetDate={}", targetDate);
        ExecutionContext context = jobExecution.getExecutionContext();
        context.putString("startDate", targetDate.withDayOfMonth(1).toString());
        context.putString("endDate", targetDate.toString());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("afterJob status={}", jobExecution.getStatus());
    }
}