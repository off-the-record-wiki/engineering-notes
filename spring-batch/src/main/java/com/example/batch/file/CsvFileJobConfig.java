package com.example.batch.file;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CsvFileJobConfig {

    private static final int CHUNK_SIZE = 10;
    private static final long SKIP_LIMIT = 3;

    @Bean
    public Job csvFileJob(JobRepository jobRepository, Step csvFileStep) {
        return new JobBuilder("csvFileJob", jobRepository).start(csvFileStep).build();
    }

    @Bean
    public Step csvFileStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Order> csvOrderReader,
            OrderGradeProcessor orderGradeProcessor,
            FlatFileItemWriter<GradedOrder> gradedOrderWriter,
            OrderSummaryFooter orderSummaryFooter,
            OrderSkipLogger orderSkipLogger) {
        return new StepBuilder("csvFileStep", jobRepository)
                .<Order, GradedOrder>chunk(CHUNK_SIZE)
                .reader(csvOrderReader)
                .processor(orderGradeProcessor)
                .writer(gradedOrderWriter)
                .listener(orderSummaryFooter)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(SKIP_LIMIT)
                .skipListener(orderSkipLogger)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Order> csvOrderReader(
            @Value("#{jobParameters['inputFile']}") String inputFile,
            @Value("#{jobParameters['allowMissing'] ?: false}") boolean allowMissing) {
        return OrderFileSupport.orderReaderBuilder("csvOrderReader")
                .resource(new FileSystemResource(inputFile))
                .strict(!allowMissing)
                .build();
    }
}
