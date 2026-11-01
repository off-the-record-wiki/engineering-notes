package com.example.batch.orderrecovery;

import java.util.Map;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class OrderRecoveryJobConfig {

    private static final int CHUNK_SIZE = 10;
    private static final int PAGE_SIZE = 10;

    @Bean
    public Job orderRecoveryJob(JobRepository jobRepository, Step orderRecoveryStep) {
        return new JobBuilder("orderRecoveryJob", jobRepository).start(orderRecoveryStep).build();
    }

    @Bean
    public Step orderRecoveryStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcPagingItemReader<RecoveryOrder> recoveryOrderReader,
            OrderStatusRecoveryProcessor orderStatusRecoveryProcessor,
            JdbcBatchItemWriter<RecoveryOrder> recoveryOrderWriter) {
        return new StepBuilder("orderRecoveryStep", jobRepository)
                .<RecoveryOrder, RecoveryOrder>chunk(CHUNK_SIZE)
                .reader(recoveryOrderReader)
                .processor(orderStatusRecoveryProcessor)
                .writer(recoveryOrderWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<RecoveryOrder> recoveryOrderReader(DataSource dataSource) throws Exception {
        return new JdbcPagingItemReaderBuilder<RecoveryOrder>()
                .name("recoveryOrderReader")
                .dataSource(dataSource)
                .pageSize(PAGE_SIZE)
                .selectClause("id, customer_id, order_datetime, status, shipping_id")
                .fromClause("orders")
                .whereClause("status IN ('SHIPPED', 'CANCELLED')")
                .sortKeys(Map.of("id", Order.ASCENDING))
                .rowMapper(new DataClassRowMapper<>(RecoveryOrder.class))
                .build();
    }

    @Bean
    public OrderStatusRecoveryProcessor orderStatusRecoveryProcessor() {
        return new OrderStatusRecoveryProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<RecoveryOrder> recoveryOrderWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<RecoveryOrder>()
                .dataSource(dataSource)
                .sql("UPDATE orders SET status = :status WHERE id = :id")
                .itemSqlParameterSourceProvider(order -> new MapSqlParameterSource()
                        .addValue("status", order.status().name())
                        .addValue("id", order.id()))
                .build();
    }
}