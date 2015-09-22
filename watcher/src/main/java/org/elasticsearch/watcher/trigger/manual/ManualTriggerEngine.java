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

package org.elasticsearch.watcher.trigger.manual;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.support.Exceptions;
import org.elasticsearch.watcher.trigger.TriggerEngine;
import org.elasticsearch.watcher.trigger.TriggerService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static org.elasticsearch.watcher.support.Exceptions.illegalArgument;

/**
 */
public class ManualTriggerEngine implements TriggerEngine<ManualTrigger,ManualTriggerEvent> {

    static final String TYPE = "manual";

    @Inject
    public ManualTriggerEngine(){
    }

    @Override
    public String type() {
        return TYPE;
    }

    /**
     * It's the responsibility of the trigger engine implementation to select the appropriate jobs
     * from the given list of jobs
     */
    @Override
    public void start(Collection<Job> jobs) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void register(Listener listener) {
    }

    @Override
    public void add(Job job) {
    }

    @Override
    public boolean remove(String jobId) {
        return false;
    }

    @Override
    public ManualTriggerEvent simulateEvent(String jobId, @Nullable Map<String, Object> data, TriggerService service) {
        if (data == null) {
            throw illegalArgument("could not simulate manual trigger event. missing required simulated trigger type");
        }
        if (data.size() == 1) {
            String type = data.keySet().iterator().next();
            return new ManualTriggerEvent(jobId, service.simulateEvent(type, jobId, data));
        }
        Object type = data.get("type");
        if (type instanceof String) {
            return new ManualTriggerEvent(jobId, service.simulateEvent((String) type, jobId, data));
        }
        throw illegalArgument("could not simulate manual trigger event. could not resolve simulated trigger type");
    }

    @Override
    public ManualTrigger parseTrigger(String context, XContentParser parser) throws IOException {
        return ManualTrigger.parse(parser);
    }

    @Override
    public ManualTriggerEvent parseTriggerEvent(TriggerService service, String watchId, String context, XContentParser parser) throws IOException {
        return ManualTriggerEvent.parse(service, watchId, context, parser);
    }
}
