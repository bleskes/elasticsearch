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

package org.elasticsearch.watcher.trigger;

import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Collection;

/**
 *
 */
public interface TriggerEngine<T extends Trigger, E extends TriggerEvent> {

    String type();

    /**
     * It's the responsibility of the trigger engine implementation to select the appropriate jobs
     * from the given list of jobs
     */
    void start(Collection<Job> jobs);

    void stop();

    void register(Listener listener);

    void add(Job job);

    boolean remove(String jobName);

    T parseTrigger(String context, XContentParser parser) throws IOException;

    E parseTriggerEvent(String context, XContentParser parser) throws IOException;

    public static interface Listener {

        void triggered(String jobName, TriggerEvent event);
    }

    public static interface Job {

        String name();

        Trigger trigger();
    }


}
