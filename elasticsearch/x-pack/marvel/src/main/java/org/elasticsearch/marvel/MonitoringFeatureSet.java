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

package org.elasticsearch.marvel;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.XPackFeatureSet;

/**
 *
 */
public class MonitoringFeatureSet implements XPackFeatureSet {

    private final boolean enabled;
    private final MonitoringLicensee licensee;

    @Inject
    public MonitoringFeatureSet(Settings settings, @Nullable MonitoringLicensee licensee) {
        this.enabled = MonitoringSettings.ENABLED.get(settings);
        this.licensee = licensee;
    }

    @Override
    public String name() {
        return Monitoring.NAME;
    }

    @Override
    public String description() {
        return "Monitoring for the Elastic Stack";
    }

    @Override
    public boolean available() {
        return licensee != null && licensee.available();
    }

    @Override
    public boolean enabled() {
        return enabled;
    }
}
