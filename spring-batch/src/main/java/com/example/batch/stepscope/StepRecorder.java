package com.example.batch.stepscope;

import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class StepRecorder {

    private final List<String> recorded = new ArrayList<>();

    public void record(String stepName) {
        recorded.add(stepName);
    }

    public List<String> getRecorded() {
        return List.copyOf(recorded);
    }
}