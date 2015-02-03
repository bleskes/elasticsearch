/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.alerts.scheduler;

import org.quartz.*;

public class FireAlertJob implements Job {

    static final String SCHEDULER_KEY = "scheduler";

    public FireAlertJob() {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String alertName = jobExecutionContext.getJobDetail().getKey().getName();
        Scheduler scheduler = (Scheduler) jobExecutionContext.getJobDetail().getJobDataMap().get(SCHEDULER_KEY);
        scheduler.notifyListeners(alertName, jobExecutionContext);
    }

    static JobKey jobKey(String alertName) {
        return new JobKey(alertName);
    }

    static JobDetail jobDetail(String alertName, Scheduler scheduler) {
        JobDetail job = JobBuilder.newJob(FireAlertJob.class).withIdentity(alertName).build();
        job.getJobDataMap().put("scheduler", scheduler);
        return job;
    }
}

