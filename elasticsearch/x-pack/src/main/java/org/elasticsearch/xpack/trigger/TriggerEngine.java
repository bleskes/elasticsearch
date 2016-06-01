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

package org.elasticsearch.xpack.trigger;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

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

    /**
     * Removes the job associated with the given name from this trigger engine.
     *
     * @param jobId   The name of the job to remove
     * @return          {@code true} if the job existed and removed, {@code false} otherwise.
     */
    boolean remove(String jobId);

    E simulateEvent(String jobId, @Nullable Map<String, Object> data, TriggerService service);

    T parseTrigger(String context, XContentParser parser) throws IOException;

    E parseTriggerEvent(TriggerService service, String id, String context, XContentParser parser) throws IOException;

    interface Listener {

        void triggered(Iterable<TriggerEvent> events);

    }

    interface Job {

        String id();

        Trigger trigger();
    }


}
