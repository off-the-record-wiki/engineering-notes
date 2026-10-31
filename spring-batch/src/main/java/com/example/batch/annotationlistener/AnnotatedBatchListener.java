package com.example.batch.annotationlistener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

public class AnnotatedBatchListener {

    private static final Logger log = LoggerFactory.getLogger(AnnotatedBatchListener.class);

    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        log.info("annotated: beforeJob job={}", jobExecution.getJobInstance().getJobName());
    }

    @AfterJob
    public void afterJob(JobExecution jobExecution) {
        log.info("annotated: afterJob job={}, status={}",
                jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("annotated: beforeStep step={}", stepExecution.getStepName());
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("annotated: afterStep step={}, status={}", stepExecution.getStepName(), stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}