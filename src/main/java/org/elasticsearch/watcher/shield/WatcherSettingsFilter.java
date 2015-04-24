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

package org.elasticsearch.watcher.shield;

import org.elasticsearch.common.inject.Inject;

/**
 *
 */
public interface WatcherSettingsFilter {

    void filterOut(String... patterns);

    class Noop implements WatcherSettingsFilter {

        public static Noop INSTANCE = new Noop();

        private Noop() {
        }

        @Override
        public void filterOut(String... patterns) {
        }
    }

    class Shield implements WatcherSettingsFilter {

        private final ShieldIntegration shieldIntegration;

        @Inject
        public Shield(ShieldIntegration shieldIntegration) {
            this.shieldIntegration = shieldIntegration;
        }

        @Override
        public void filterOut(String... patterns) {
            shieldIntegration.filterOutSettings(patterns);
        }
    }
}
