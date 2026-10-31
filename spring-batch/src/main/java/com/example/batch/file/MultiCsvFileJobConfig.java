package com.example.batch.file;

import java.io.IOException;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.MultiResourceItemReader;
import org.springframework.batch.infrastructure.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MultiCsvFileJobConfig {

    private static final int CHUNK_SIZE = 10;

    @Bean
    public Job multiCsvFileJob(JobRepository jobRepository, Step multiCsvFileStep) {
        return new JobBuilder("multiCsvFileJob", jobRepository).start(multiCsvFileStep).build();
    }

    @Bean
    public Step multiCsvFileStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            MultiResourceItemReader<Order> multiCsvOrderReader,
            OrderGradeProcessor orderGradeProcessor,
            FlatFileItemWriter<GradedOrder> gradedOrderWriter,
            OrderSummaryFooter orderSummaryFooter) {
        return new StepBuilder("multiCsvFileStep", jobRepository)
                .<Order, GradedOrder>chunk(CHUNK_SIZE)
                .reader(multiCsvOrderReader)
                .processor(orderGradeProcessor)
                .writer(gradedOrderWriter)
                .listener(orderSummaryFooter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader<Order> multiCsvOrderReader(@Value("#{jobParameters['inputDir']}") String inputDir)
            throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("file:" + inputDir + "/*.csv");
        return new MultiResourceItemReaderBuilder<Order>()
                .name("multiCsvOrderReader")
                .resources(resources)
                .delegate(OrderFileSupport.orderReaderBuilder("multiCsvOrderDelegate").build())
                .build();
    }
}
