package com.example.batch.promotion;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PromotionJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PromotionJobConfig.class);

    @Bean
    public Job promotionJob(JobRepository jobRepository, Step aggregateStep, Step reportStep) {
        return new JobBuilder("promotionJob", jobRepository)
                .start(aggregateStep)
                .next(reportStep)
                .build();
    }

    @Bean
    public Step aggregateStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ListItemReader<Long> amountReader,
            OrderTotalWriter orderTotalWriter) {
        return new StepBuilder("aggregateStep", jobRepository)
                .<Long, Long>chunk(2)
                .reader(amountReader)
                .writer(orderTotalWriter)
                .listener(orderTotalPromotionListener())
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<Long> amountReader(@Value("#{jobParameters['amounts']}") String amounts) {
        List<Long> items = Arrays.stream(amounts.split(";"))
                .map(String::trim)
                .map(Long::valueOf)
                .toList();
        return new ListItemReader<>(items);
    }

    @Bean
    @StepScope
    public OrderTotalWriter orderTotalWriter() {
        return new OrderTotalWriter();
    }

    private ExecutionContextPromotionListener orderTotalPromotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {OrderTotalWriter.ORDER_COUNT, OrderTotalWriter.TOTAL_AMOUNT});
        return listener;
    }

    @Bean
    public Step reportStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet reportTasklet) {
        return new StepBuilder("reportStep", jobRepository)
                .tasklet(reportTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet reportTasklet(
            @Value("#{jobExecutionContext['orderCount']}") Integer orderCount,
            @Value("#{jobExecutionContext['totalAmount']}") Long totalAmount) {
        return (contribution, chunkContext) -> {
            log.info("report orderCount={}, totalAmount={}", orderCount, totalAmount);
            return RepeatStatus.FINISHED;
        };
    }
}
