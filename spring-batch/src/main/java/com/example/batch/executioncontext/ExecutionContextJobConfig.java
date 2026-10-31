package com.example.batch.executioncontext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ExecutionContextJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ExecutionContextJobConfig.class);

    @Bean
    public Job executionContextJob(
            JobRepository jobRepository, Step executionContextPutStep, Step executionContextReadStep) {
        return new JobBuilder("executionContextJob", jobRepository)
                .start(executionContextPutStep)
                .next(executionContextReadStep)
                .build();
    }

    @Bean
    public Step executionContextPutStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ExecutionContextPromotionListener executionContextPromotionListener) {
        return new StepBuilder("putStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
                            stepExecution.getJobExecution().getExecutionContext().putString("directValue", "direct");
                            stepExecution.getExecutionContext().putString("promotedValue", "promoted");
                            stepExecution.getExecutionContext().putString("stepOnlyValue", "stepOnly");
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .listener(executionContextPromotionListener)
                .build();
    }

    @Bean
    public ExecutionContextPromotionListener executionContextPromotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {"promotedValue"});
        return listener;
    }

    @Bean
    public Step executionContextReadStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet executionContextReadTasklet) {
        return new StepBuilder("readStep", jobRepository)
                .tasklet(executionContextReadTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet executionContextReadTasklet(
            @Value("#{jobExecutionContext['directValue']}") String directValue,
            @Value("#{jobExecutionContext['promotedValue']}") String promotedValue,
            @Value("#{jobExecutionContext['stepOnlyValue']}") String stepOnlyValue) {
        return (contribution, chunkContext) -> {
            log.info("directValue={}, promotedValue={}, stepOnlyValue={}", directValue, promotedValue, stepOnlyValue);
            return RepeatStatus.FINISHED;
        };
    }
}