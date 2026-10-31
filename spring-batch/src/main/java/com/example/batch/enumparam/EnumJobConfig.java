package com.example.batch.enumparam;

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
public class EnumJobConfig {

    private static final Logger log = LoggerFactory.getLogger(EnumJobConfig.class);

    @Bean
    public Job enumJob(JobRepository jobRepository, Step enumStep) {
        return new JobBuilder("enumJob", jobRepository)
                .start(enumStep)
                .build();
    }

    @Bean
    public Step enumStep(JobRepository jobRepository, Tasklet enumTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("enumStep", jobRepository)
                .tasklet(enumTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet enumTasklet(@Value("#{jobParameters['deliveryType']}") DeliveryType deliveryType) {
        return (contribution, chunkContext) -> {
            log.info("deliveryType={}, leadDays={}", deliveryType.name(), deliveryType.leadDays());
            return RepeatStatus.FINISHED;
        };
    }
}