package com.example.batch.jobscope;

import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

@Component
@JobScope
public class VisitedSteps {

    private final List<String> steps = new ArrayList<>();

    public void add(String stepName) {
        steps.add(stepName);
    }

    public List<String> getSteps() {
        return List.copyOf(steps);
    }
}