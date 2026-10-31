package com.example.batch.direct;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DirectAccessJobConfig {

    private static final Logger log = LoggerFactory.getLogger(DirectAccessJobConfig.class);

    @Bean
    public Job directAccessJob(JobRepository jobRepository, Step directAccessStep) {
        return new JobBuilder("directAccessJob", jobRepository)
                .start(directAccessStep)
                .build();
    }

    @Bean
    public Step directAccessStep(
            JobRepository jobRepository, Tasklet directAccessTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("directAccessStep", jobRepository)
                .tasklet(directAccessTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet directAccessTasklet() {
        return (contribution, chunkContext) -> {
            JobParameters parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
            String userName = parameters.getString("userName");
            LocalDate targetDate = parameters.getLocalDate("targetDate");
            Long limit = parameters.getLong("limit", 10L);
            log.info("userName={}, targetDate={}, limit={}", userName, targetDate, limit);
            return RepeatStatus.FINISHED;
        };
    }
}