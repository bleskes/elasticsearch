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

package org.elasticsearch.watcher.trigger.schedule.quartz;

import org.quartz.*;

public class WatcherQuartzJob implements Job {

    static final String ENGINE_KEY = "engine";

    public WatcherQuartzJob() {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String watchName = jobExecutionContext.getJobDetail().getKey().getName();
        QuartzScheduleTriggerEngine scheduler = (QuartzScheduleTriggerEngine) jobExecutionContext.getJobDetail().getJobDataMap().get(ENGINE_KEY);
        scheduler.notifyListeners(watchName, jobExecutionContext);
    }

    static JobKey jobKey(String watchName) {
        return new JobKey(watchName);
    }

    static JobDetail jobDetail(String watchName, QuartzScheduleTriggerEngine engine) {
        JobDetail job = JobBuilder.newJob(WatcherQuartzJob.class).withIdentity(watchName).build();
        job.getJobDataMap().put(ENGINE_KEY, engine);
        return job;
    }
}

