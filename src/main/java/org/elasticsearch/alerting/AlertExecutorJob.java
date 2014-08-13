package org.elasticsearch.alerting;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class AlertExecutorJob implements Job {

    public AlertExecutorJob () {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String alertName = jobExecutionContext.getJobDetail().getKey().getName();
        ((AlertScheduler)jobExecutionContext.getJobDetail().getJobDataMap().get("manager")).executeAlert(alertName,
                jobExecutionContext);
    }
}

