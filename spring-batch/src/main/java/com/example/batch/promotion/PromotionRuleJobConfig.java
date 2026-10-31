package com.example.batch.promotion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PromotionRuleJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PromotionRuleJobConfig.class);

    @Bean
    public Job promotionRuleJob(JobRepository jobRepository, Step statusStep, Step strictStep, Step ruleCheckStep) {
        return new JobBuilder("promotionRuleJob", jobRepository)
                .start(statusStep)
                .next(strictStep)
                .next(ruleCheckStep)
                .build();
    }

    @Bean
    public Step statusStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        ExecutionContextPromotionListener defaultStatuses = promotionListener("defaultKey");
        ExecutionContextPromotionListener emptyAllowed = promotionListener("emptyAllowedKey");
        emptyAllowed.setStatuses(new String[] {ExitStatus.COMPLETED.getExitCode(), "EMPTY"});

        return new StepBuilder("statusStep", jobRepository)
                .tasklet(
                        (contribution, chunkContext) -> {
                            ExecutionContext stepContext = contribution.getStepExecution().getExecutionContext();
                            stepContext.putString("defaultKey", "promoted");
                            stepContext.putString("emptyAllowedKey", "promoted");
                            contribution.setExitStatus(new ExitStatus("EMPTY"));
                            return RepeatStatus.FINISHED;
                        },
                        transactionManager)
                .listener(defaultStatuses)
                .listener(emptyAllowed)
                .build();
    }

    @Bean
    public Step strictStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        ExecutionContextPromotionListener strict = promotionListener("strictKey");
        strict.setStrict(true);

        return new StepBuilder("strictStep", jobRepository)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
                .listener(strict)
                .build();
    }

    @Bean
    public Step ruleCheckStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet ruleCheckTasklet) {
        return new StepBuilder("ruleCheckStep", jobRepository)
                .tasklet(ruleCheckTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet ruleCheckTasklet(
            @Value("#{jobExecutionContext['defaultKey']}") String defaultKey,
            @Value("#{jobExecutionContext['emptyAllowedKey']}") String emptyAllowedKey,
            @Value("#{jobExecutionContext['strictKey']}") String strictKey) {
        return (contribution, chunkContext) -> {
            log.info("defaultKey={}, emptyAllowedKey={}, strictKey={}", defaultKey, emptyAllowedKey, strictKey);
            return RepeatStatus.FINISHED;
        };
    }

    private ExecutionContextPromotionListener promotionListener(String key) {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {key});
        return listener;
    }
}
