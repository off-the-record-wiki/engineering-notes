package com.example.batch.listener;

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
public class ListenerJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ListenerJobConfig.class);

    @Bean
    public Job listenerJob(JobRepository jobRepository, Step listenerStep) {
        return new JobBuilder("listenerJob", jobRepository)
                .listener(new ReportPeriodListener())
                .start(listenerStep)
                .build();
    }

    @Bean
    public Step listenerStep(
            JobRepository jobRepository, Tasklet listenerTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("listenerStep", jobRepository)
                .tasklet(listenerTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet listenerTasklet(
            @Value("#{jobExecutionContext['startDate']}") String startDate,
            @Value("#{jobExecutionContext['endDate']}") String endDate,
            @Value("#{jobParameters['fail']}") Boolean fail) {
        return (contribution, chunkContext) -> {
            log.info("period={}~{}", startDate, endDate);
            if (Boolean.TRUE.equals(fail)) {
                throw new IllegalStateException("failed on purpose");
            }
            return RepeatStatus.FINISHED;
        };
    }
}