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

import org.elasticsearch.common.joda.time.DateTime;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A mock scheduler to help with unit testing. Provide {@link SchedulerMock#fire} method to manually trigger
 * jobs.
 */
public class SchedulerMock implements Scheduler {

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private final ConcurrentMap<String, Job> jobs = new ConcurrentHashMap<>();

    @Override
    public void start(Collection<? extends Job> jobs) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void add(Job job) {
        jobs.put(job.name(), job);
    }

    @Override
    public boolean remove(String jobName) {
        return jobs.remove(jobName) != null;
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void fire(String jobName) {
        DateTime now = new DateTime();
        for (Listener listener : listeners) {
            listener.fire(jobName, now ,now);
        }
    }
}
