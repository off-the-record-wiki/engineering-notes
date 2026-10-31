package com.example.batch.annotationlistener;

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
public class AnnotationListenerJobConfig {

    private static final Logger log = LoggerFactory.getLogger(AnnotationListenerJobConfig.class);

    @Bean
    public AnnotatedBatchListener annotatedBatchListener() {
        return new AnnotatedBatchListener();
    }

    @Bean
    public Job annotationListenerJob(
            JobRepository jobRepository, Step annotationStep, AnnotatedBatchListener annotatedBatchListener) {
        return new JobBuilder("annotationListenerJob", jobRepository)
                .listener(annotatedBatchListener)
                .start(annotationStep)
                .build();
    }

    @Bean
    public Step annotationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet annotationTasklet,
            AnnotatedBatchListener annotatedBatchListener) {
        return new StepBuilder("annotationStep", jobRepository)
                .tasklet(annotationTasklet, transactionManager)
                .listener(annotatedBatchListener)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet annotationTasklet(@Value("#{jobParameters['fail']}") Boolean fail) {
        return (contribution, chunkContext) -> {
            log.info("annotated: tasklet");
            if (Boolean.TRUE.equals(fail)) {
                throw new IllegalStateException("failed on purpose");
            }
            return RepeatStatus.FINISHED;
        };
    }
}