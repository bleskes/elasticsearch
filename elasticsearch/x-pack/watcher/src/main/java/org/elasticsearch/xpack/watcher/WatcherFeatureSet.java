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

package org.elasticsearch.xpack.watcher;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.XPackFeatureSet;

import java.io.IOException;

/**
 *
 */
public class WatcherFeatureSet implements XPackFeatureSet {

    private final boolean enabled;
    private final WatcherLicensee licensee;

    @Inject
    public WatcherFeatureSet(Settings settings, @Nullable WatcherLicensee licensee, NamedWriteableRegistry namedWriteableRegistry) {
        this.enabled = Watcher.enabled(settings);
        this.licensee = licensee;
        namedWriteableRegistry.register(Usage.class, Usage.writeableName(Watcher.NAME), Usage::new);
    }

    @Override
    public String name() {
        return Watcher.NAME;
    }

    @Override
    public String description() {
        return "Alerting, Notification and Automation for the Elastic Stack";
    }

    @Override
    public boolean available() {
        return licensee != null && licensee.isAvailable();
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public XPackFeatureSet.Usage usage() {
        return new Usage(available(), enabled());
    }

    static class Usage extends XPackFeatureSet.Usage {

        public Usage(StreamInput input) throws IOException {
            super(input);
        }

        public Usage(boolean available, boolean enabled) {
            super(Watcher.NAME, available, enabled);
        }

    }
}
