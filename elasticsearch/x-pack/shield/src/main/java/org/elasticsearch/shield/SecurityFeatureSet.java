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

package org.elasticsearch.shield;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.XPackFeatureSet;

/**
 *
 */
public class SecurityFeatureSet implements XPackFeatureSet {

    private final boolean enabled;
    private final SecurityLicenseState licenseState;

    @Inject
    public SecurityFeatureSet(Settings settings, @Nullable SecurityLicenseState licenseState) {
        this.enabled = Security.enabled(settings);
        this.licenseState = licenseState;
    }

    @Override
    public String name() {
        return Security.NAME;
    }

    @Override
    public String description() {
        return "Security for the Elastic Stack";
    }

    @Override
    public boolean available() {
        return licenseState != null && licenseState.securityEnabled();
    }

    @Override
    public boolean enabled() {
        return enabled;
    }
}
