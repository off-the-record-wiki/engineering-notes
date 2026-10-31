package com.example.batch.json;

import java.util.Properties;
import org.springframework.batch.core.converter.JsonJobParametersConverter;
import org.springframework.batch.core.job.parameters.JobParameters;

public class JsonJobParametersReader {

    private final JsonJobParametersConverter converter = new JsonJobParametersConverter();

    public JobParameters read(Properties properties) {
        return converter.getJobParameters(properties);
    }
}