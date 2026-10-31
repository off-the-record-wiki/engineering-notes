package com.example.batch.pojo;

import java.time.LocalDate;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class ReportJobParameters {

    @Value("#{jobParameters['reportName']}")
    private String reportName;

    @Value("#{jobParameters['reportDate']}")
    private LocalDate reportDate;

    @Value("#{jobParameters['pageSize']}")
    private Integer pageSize;

    public String getReportName() {
        return reportName;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public Integer getPageSize() {
        return pageSize;
    }
}