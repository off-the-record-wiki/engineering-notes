package com.example.batch.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DateTimeJobConfig {

    private static final Logger log = LoggerFactory.getLogger(DateTimeJobConfig.class);

    @Bean
    public Job dateTimeJob(JobRepository jobRepository, Step dateTimeStep) {
        return new JobBuilder("dateTimeJob", jobRepository)
                .start(dateTimeStep)
                .build();
    }

    @Bean
    public Step dateTimeStep(
            JobRepository jobRepository, Tasklet dateTimeTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("dateTimeStep", jobRepository)
                .tasklet(dateTimeTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet dateTimeTasklet(
            @Value("#{jobParameters['targetDate']}") LocalDate targetDate,
            @Value("#{jobParameters['requestedAt']}") LocalDateTime requestedAt) {
        return (contribution, chunkContext) -> {
            log.info("targetDate={}, dayOfWeek={}",
                    targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE), targetDate.getDayOfWeek());
            log.info("requestedAt={}, time={}",
                    requestedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    requestedAt.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
            return RepeatStatus.FINISHED;
        };
    }
}