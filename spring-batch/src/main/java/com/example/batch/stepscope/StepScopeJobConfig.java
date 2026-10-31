package com.example.batch.stepscope;

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

/**
 * TL;DR: {@code @StepScope} 빈은 StepExecution 하나에 하나씩 만들어진다.
 * - stepScopeTasklet: 빈 정의 하나를 두 Step 이 쓰지만 Step 마다 새로 만들어져 {@code #{stepExecution.stepName}} 으로 자기 이름을 받는다.
 * - {@link StepRecorder}: Step 끼리 공유하지 않는다. secondStep 의 기록은 [secondStep] 뿐이다({@code @JobScope} 면 [firstStep, secondStep]).
 * - Step 실행 밖에서 쓰면 {@code ScopeNotActiveException}.
 */
@Configuration
public class StepScopeJobConfig {

    private static final Logger log = LoggerFactory.getLogger(StepScopeJobConfig.class);

    @Bean
    public Job stepScopeJob(JobRepository jobRepository, Step stepScopeFirstStep, Step stepScopeSecondStep) {
        return new JobBuilder("stepScopeJob", jobRepository)
                .start(stepScopeFirstStep)
                .next(stepScopeSecondStep)
                .build();
    }

    @Bean
    public Step stepScopeFirstStep(
            JobRepository jobRepository, Tasklet stepScopeTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("firstStep", jobRepository)
                .tasklet(stepScopeTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step stepScopeSecondStep(
            JobRepository jobRepository, Tasklet stepScopeTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("secondStep", jobRepository)
                .tasklet(stepScopeTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet stepScopeTasklet(
            StepRecorder stepRecorder, @Value("#{stepExecution.stepName}") String stepName) {
        return (contribution, chunkContext) -> {
            stepRecorder.record(stepName);
            log.info("stepName={}, recorded={}", stepName, stepRecorder.getRecorded());
            return RepeatStatus.FINISHED;
        };
    }
}