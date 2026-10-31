package com.example.batch.progresslistener;

import org.springframework.batch.core.BatchStatus;

public interface BatchProgressListener {

    default void onJobStarted(String jobName) {
    }

    default void onStepCompleted(String stepName, long readCount, long writeCount) {
    }

    default void onJobFinished(String jobName, BatchStatus status) {
    }
}