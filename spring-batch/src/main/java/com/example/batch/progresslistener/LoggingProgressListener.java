package com.example.batch.progresslistener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;

public class LoggingProgressListener implements BatchProgressListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingProgressListener.class);

    @Override
    public void onJobStarted(String jobName) {
        log.info("progress: jobStarted job={}", jobName);
    }

    @Override
    public void onStepCompleted(String stepName, long readCount, long writeCount) {
        log.info("progress: stepCompleted step={}, read={}, write={}", stepName, readCount, writeCount);
    }

    @Override
    public void onJobFinished(String jobName, BatchStatus status) {
        log.info("progress: jobFinished job={}, status={}", jobName, status);
    }
}