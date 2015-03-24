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

package org.elasticsearch.watcher.scheduler;

import org.quartz.*;

public class WatcherQuartzJob implements Job {

    static final String SCHEDULER_KEY = "scheduler";

    public WatcherQuartzJob() {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String watchName = jobExecutionContext.getJobDetail().getKey().getName();
        InternalScheduler scheduler = (InternalScheduler) jobExecutionContext.getJobDetail().getJobDataMap().get(SCHEDULER_KEY);
        scheduler.notifyListeners(watchName, jobExecutionContext);
    }

    static JobKey jobKey(String watchName) {
        return new JobKey(watchName);
    }

    static JobDetail jobDetail(String watchName, InternalScheduler scheduler) {
        JobDetail job = JobBuilder.newJob(WatcherQuartzJob.class).withIdentity(watchName).build();
        job.getJobDataMap().put("scheduler", scheduler);
        return job;
    }
}

