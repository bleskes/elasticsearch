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

package org.elasticsearch.license.plugin.core;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A supporting base class for injectable Licensee components.
 */
public abstract class AbstractLicenseeComponent extends AbstractComponent implements Licensee {

    private final String id;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    // we initialize the licensee state to enabled with trial operation mode
    protected volatile Status status = Status.ENABLED;

    protected AbstractLicenseeComponent(Settings settings, String id) {
        super(settings);
        this.id = id;
    }

    @Override
    public final String id() {
        return id;
    }

    /**
     * @return the current status of this licensee (can never be null)
     */
    public Status getStatus() {
        return status;
    }

    public void add(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void onChange(Status status) {
        this.status = status;
        logger.trace("[{}] is running in [{}] mode", id(), status);
        for (Listener listener : listeners) {
            listener.onChange(status);
        }
    }

    public interface Listener {
        void onChange(Status status);
    }

}
