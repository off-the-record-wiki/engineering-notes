package com.example.batch.stepparameter;

import java.util.List;
import java.util.stream.LongStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class StepParameterJobConfig {

    private static final Logger log = LoggerFactory.getLogger(StepParameterJobConfig.class);

    @Bean
    public Job stepParameterJob(JobRepository jobRepository, Step stepParameterStep) {
        return new JobBuilder("stepParameterJob", jobRepository)
                .start(stepParameterStep)
                .build();
    }

    @Bean
    @JobScope
    public Step stepParameterStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ListItemReader<Long> stepParameterReader,
            ItemWriter<Long> stepParameterWriter,
            @Value("#{jobParameters['chunkSize']}") Long chunkSize) {
        return new StepBuilder("stepParameterStep", jobRepository)
                .<Long, Long>chunk(chunkSize.intValue())
                .reader(stepParameterReader)
                .writer(stepParameterWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<Long> stepParameterReader(@Value("#{jobParameters['count']}") Long count) {
        List<Long> items = LongStream.rangeClosed(1, count).boxed().toList();
        return new ListItemReader<>(items);
    }

    @Bean
    public ItemWriter<Long> stepParameterWriter() {
        return chunk -> log.info("chunk={}", chunk.getItems());
    }
}