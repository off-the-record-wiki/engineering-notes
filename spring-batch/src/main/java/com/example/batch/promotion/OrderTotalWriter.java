package com.example.batch.promotion;

import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class OrderTotalWriter implements ItemWriter<Long> {

    static final String ORDER_COUNT = "orderCount";
    static final String TOTAL_AMOUNT = "totalAmount";

    private StepExecution stepExecution;

    @BeforeStep
    public void saveStepExecution(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void write(Chunk<? extends Long> chunk) {
        for (Long amount : chunk) {
            if (amount < 0) {
                throw new IllegalArgumentException("amount must not be negative: " + amount);
            }
        }
        long chunkTotal = chunk.getItems().stream().mapToLong(Long::longValue).sum();

        ExecutionContext stepContext = stepExecution.getExecutionContext();
        stepContext.putInt(ORDER_COUNT, stepContext.getInt(ORDER_COUNT, 0) + chunk.size());
        stepContext.putLong(TOTAL_AMOUNT, stepContext.getLong(TOTAL_AMOUNT, 0L) + chunkTotal);
    }
}
