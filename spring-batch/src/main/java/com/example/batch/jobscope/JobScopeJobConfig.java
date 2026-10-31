package com.example.batch.jobscope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * TL;DR: {@code @JobScope} 빈은 JobExecution 하나에 하나씩 만들어진다.
 * - firstStep: Step 빈에 {@code @JobScope} → 실행마다 jobParameters 를 새로 받는다.
 * - {@link VisitedSteps}: 같은 실행의 Step 들이 한 인스턴스를 공유하고, 다음 실행은 새 인스턴스로 시작한다.
 * - Job 실행 밖에서 쓰면 {@code ScopeNotActiveException}.
 */
@Configuration
public class JobScopeJobConfig {

    private static final Logger log = LoggerFactory.getLogger(JobScopeJobConfig.class);

    @Bean
    public Job jobScopeJob(JobRepository jobRepository, Step jobScopeFirstStep, Step jobScopeSecondStep) {
        return new JobBuilder("jobScopeJob", jobRepository)
                .start(jobScopeFirstStep)
                .next(jobScopeSecondStep)
                .build();
    }

    @Bean
    @JobScope
    public Step jobScopeFirstStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            VisitedSteps visitedSteps,
            @Value("#{jobParameters['message']}") String message) {
        return new StepBuilder("firstStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            visitedSteps.add("firstStep");
                            log.info("firstStep message={}", message);
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }

    @Bean
    public Step jobScopeSecondStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager, VisitedSteps visitedSteps) {
        return new StepBuilder("secondStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            visitedSteps.add("secondStep");
                            log.info("secondStep visitedSteps={}", visitedSteps.getSteps());
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }
}