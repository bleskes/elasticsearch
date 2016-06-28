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

package org.elasticsearch.xpack.watcher.trigger;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public abstract class AbstractTriggerEngine<T extends Trigger, E extends TriggerEvent> extends AbstractComponent implements
        TriggerEngine<T, E> {

    protected final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public AbstractTriggerEngine(Settings settings) {
        super(settings);
    }

    @Override
    public void register(Listener listener) {
        listeners.add(listener);
    }
}
