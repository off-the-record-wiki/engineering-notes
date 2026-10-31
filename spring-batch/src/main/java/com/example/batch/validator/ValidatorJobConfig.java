package com.example.batch.validator;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.CompositeJobParametersValidator;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
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
public class ValidatorJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ValidatorJobConfig.class);

    @Bean
    public Job validatorJob(JobRepository jobRepository, Step validatorStep) {
        return new JobBuilder("validatorJob", jobRepository)
                .validator(fileJobParametersValidator())
                .start(validatorStep)
                .build();
    }

    @Bean
    public Step validatorStep(
            JobRepository jobRepository, Tasklet validatorTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("validatorStep", jobRepository)
                .tasklet(validatorTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet validatorTasklet(
            @Value("#{jobParameters['fileName']}") String fileName,
            @Value("#{jobParameters['chunkSize']}") Long chunkSize) {
        return (contribution, chunkContext) -> {
            log.info("fileName={}, chunkSize={}", fileName, chunkSize);
            return RepeatStatus.FINISHED;
        };
    }

    private JobParametersValidator fileJobParametersValidator() {
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(List.of(
                new DefaultJobParametersValidator(new String[] {"fileName", "chunkSize"}, new String[] {}),
                new FileJobParametersValidator()));
        return validator;
    }
}