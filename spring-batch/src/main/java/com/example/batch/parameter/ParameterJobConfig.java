package com.example.batch.parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
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
public class ParameterJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ParameterJobConfig.class);

    @Bean
    public Job parameterJob(JobRepository jobRepository, Step parameterStep) {
        return new JobBuilder("parameterJob", jobRepository)
                .validator(new DefaultJobParametersValidator(new String[] {"name", "count"}, new String[] {}))
                .start(parameterStep)
                .build();
    }

    @Bean
    public Step parameterStep(
            JobRepository jobRepository, Tasklet parameterTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("parameterStep", jobRepository)
                .tasklet(parameterTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet parameterTasklet(
            @Value("#{jobParameters['name']}") String name, @Value("#{jobParameters['count']}") Integer count) {
        return (contribution, chunkContext) -> {
            for (int i = 1; i <= count; i++) {
                log.info("Hello, {} ({}/{})", name, i, count);
            }
            return RepeatStatus.FINISHED;
        };
    }
}