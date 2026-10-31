package com.example.batch.progresslistener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;

public class FailureAlertListener implements BatchProgressListener {

    private static final Logger log = LoggerFactory.getLogger(FailureAlertListener.class);

    @Override
    public void onJobFinished(String jobName, BatchStatus status) {
        if (status == BatchStatus.FAILED) {
            log.warn("ALERT job={}, status={}", jobName, status);
        }
    }
}