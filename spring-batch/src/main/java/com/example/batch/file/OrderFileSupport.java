package com.example.batch.file;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class OrderFileSupport {

    static FlatFileItemReaderBuilder<Order> orderReaderBuilder(String name) {
        return new FlatFileItemReaderBuilder<Order>()
                .name(name)
                .linesToSkip(1)
                .delimited()
                .names("orderId", "customer", "amount")
                .targetType(Order.class);
    }

    @Bean
    public OrderGradeProcessor orderGradeProcessor() {
        return new OrderGradeProcessor();
    }

    @Bean
    public OrderSkipLogger orderSkipLogger() {
        return new OrderSkipLogger();
    }

    @Bean
    @StepScope
    public OrderSummaryFooter orderSummaryFooter() {
        return new OrderSummaryFooter();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<GradedOrder> gradedOrderWriter(
            @Value("#{jobParameters['outputFile']}") String outputFile, OrderSummaryFooter orderSummaryFooter) {
        return new FlatFileItemWriterBuilder<GradedOrder>()
                .name("gradedOrderWriter")
                .resource(new FileSystemResource(outputFile))
                .headerCallback(writer -> writer.write("orderId,customer,amount,grade"))
                .footerCallback(orderSummaryFooter)
                .delimited()
                .sourceType(GradedOrder.class)
                .names("orderId", "customer", "amount", "grade")
                .build();
    }
}
