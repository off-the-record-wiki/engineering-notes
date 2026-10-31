package com.example.batch.validator;

import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersValidator;

public class FileJobParametersValidator implements JobParametersValidator {

    private static final long MIN_CHUNK_SIZE = 1L;
    private static final long MAX_CHUNK_SIZE = 1000L;

    @Override
    public void validate(JobParameters parameters) throws InvalidJobParametersException {
        JobParameter<?> fileNameParameter = parameters.getParameter("fileName");
        if (fileNameParameter == null || !fileNameParameter.type().equals(String.class)) {
            throw new InvalidJobParametersException("fileName must be java.lang.String");
        }
        String fileName = (String) fileNameParameter.value();
        if (!fileName.endsWith(".csv")) {
            throw new InvalidJobParametersException("fileName must end with .csv: " + fileName);
        }

        JobParameter<?> chunkSize = parameters.getParameter("chunkSize");
        if (chunkSize == null || !chunkSize.type().equals(Long.class)) {
            throw new InvalidJobParametersException("chunkSize must be java.lang.Long");
        }
        long value = (Long) chunkSize.value();
        if (value < MIN_CHUNK_SIZE || value > MAX_CHUNK_SIZE) {
            throw new InvalidJobParametersException(
                    "chunkSize must be between " + MIN_CHUNK_SIZE + " and " + MAX_CHUNK_SIZE + ": " + value);
        }
    }
}