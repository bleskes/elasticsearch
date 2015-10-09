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

package org.elasticsearch.shield.license;

import org.elasticsearch.license.plugin.core.LicenseState;

import java.util.HashSet;
import java.util.Set;

/**
 * Serves as a registry of license event listeners and enables notifying them about the
 * different events.
 *
 * This class is required to serves as a bridge between the license service and any other
 * service that needs to recieve license events. The reason for that is that some services
 * that require such notifications also serves as a dependency for the licensing service
 * which introdues a circular dependency in guice (e.g. TransportService). This class then
 * serves as a bridge between the different services to eliminate such circular dependencies.
 */
public class LicenseEventsNotifier {

    private final Set<Listener> listeners = new HashSet<>();

    public void register(Listener listener) {
        listeners.add(listener);
    }

    protected void notify(LicenseState state) {
        for (Listener listener : listeners) {
            listener.notify(state);
        }
    }

    public static interface Listener {

        void notify(LicenseState state);
    }
}
