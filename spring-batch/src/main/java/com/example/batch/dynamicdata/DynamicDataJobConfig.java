package com.example.batch.dynamicdata;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DynamicDataJobConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataJobConfig.class);

    @Bean
    public RegionDirectory regionDirectory() {
        return new RegionDirectory();
    }

    @Bean
    public Job dynamicDataJob(
            JobRepository jobRepository,
            RegionDirectory regionDirectory,
            Step prepareStep,
            Step dispatchStep,
            Step summaryStep) {
        return new JobBuilder("dynamicDataJob", jobRepository)
                .listener(new DispatchPlanListener(regionDirectory))
                .start(prepareStep)
                .next(dispatchStep)
                .next(summaryStep)
                .build();
    }

    @Bean
    public Step prepareStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet prepareTasklet) {
        return new StepBuilder("prepareStep", jobRepository)
                .tasklet(prepareTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet prepareTasklet(
            @Value("#{jobExecutionContext['label']}") String label,
            @Value("#{jobExecutionContext['regions']}") List<String> regions) {
        return (contribution, chunkContext) -> {
            log.info("prepare label={}, regions={}", label, regions);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step dispatchStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ListItemReader<String> dispatchReader) {
        return new StepBuilder("dispatchStep", jobRepository)
                .<String, String>chunk(2)
                .reader(dispatchReader)
                .writer(dispatchWriter())
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<String> dispatchReader(@Value("#{jobExecutionContext['regions']}") List<String> regions) {
        return new ListItemReader<>(regions);
    }

    private ItemWriter<String> dispatchWriter() {
        return chunk -> chunk.getItems().forEach(region -> log.info("dispatch region={}", region));
    }

    @Bean
    public Step summaryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("summaryStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            ExecutionContext jobContext = chunkContext.getStepContext()
                                    .getStepExecution()
                                    .getJobExecution()
                                    .getExecutionContext();
                            log.info(
                                    "summary label={}, regionCount={}",
                                    jobContext.getString("label"),
                                    jobContext.getInt("regionCount"));
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .build();
    }
}