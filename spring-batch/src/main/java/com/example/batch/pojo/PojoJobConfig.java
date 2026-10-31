package com.example.batch.pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PojoJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PojoJobConfig.class);

    @Bean
    public Job pojoJob(JobRepository jobRepository, Step pojoStep) {
        return new JobBuilder("pojoJob", jobRepository)
                .start(pojoStep)
                .build();
    }

    @Bean
    public Step pojoStep(JobRepository jobRepository, Tasklet pojoTasklet, PlatformTransactionManager transactionManager) {
        return new StepBuilder("pojoStep", jobRepository)
                .tasklet(pojoTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet pojoTasklet(ReportJobParameters parameters) {
        return (contribution, chunkContext) -> {
            log.info("reportName={}, reportDate={}, pageSize={}",
                    parameters.getReportName(), parameters.getReportDate(), parameters.getPageSize());
            return RepeatStatus.FINISHED;
        };
    }
}