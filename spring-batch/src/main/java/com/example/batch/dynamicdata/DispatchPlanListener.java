package com.example.batch.dynamicdata;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.infrastructure.item.ExecutionContext;

public class DispatchPlanListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(DispatchPlanListener.class);

    private final RegionDirectory regionDirectory;

    public DispatchPlanListener(RegionDirectory regionDirectory) {
        this.regionDirectory = regionDirectory;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String group = jobExecution.getJobParameters().getString("group");
        if (group == null) {
            throw new IllegalArgumentException("group parameter is required");
        }
        List<String> regions = new ArrayList<>(regionDirectory.findRegions(group));
        log.info("beforeJob group={}, regions={}", group, regions);

        ExecutionContext context = jobExecution.getExecutionContext();
        context.put("regions", regions);
        context.putInt("regionCount", regions.size());
        context.putString("label", "dispatch-" + jobExecution.getId());
    }
}