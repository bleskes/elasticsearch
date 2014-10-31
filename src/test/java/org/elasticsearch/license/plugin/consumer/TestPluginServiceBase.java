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

package org.elasticsearch.license.plugin.consumer;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.plugin.core.LicensesClientService;
import org.elasticsearch.license.plugin.core.LicensesService;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class TestPluginServiceBase extends AbstractLifecycleComponent<TestPluginServiceBase> {

    private LicensesClientService licensesClientService;

    // specify the trial license spec for the feature
    // example: 30 day trial on 1000 nodes
    final LicensesService.TrialLicenseOptions trialLicenseOptions;

    private AtomicBoolean enabled = new AtomicBoolean(false);

    public TestPluginServiceBase(Settings settings, LicensesClientService licensesClientService) {
        super(settings);
        this.licensesClientService = licensesClientService;
        int durationInSec = settings.getAsInt(settingPrefix() + ".trial_license_duration_in_seconds", -1);
        if (durationInSec == -1) {
            this.trialLicenseOptions = null;
        } else {
            this.trialLicenseOptions = new LicensesService.TrialLicenseOptions(TimeValue.timeValueSeconds(durationInSec), 1000);
        }
    }

    // should be the same string used by the license Manger to generate
    // signed license
    public abstract String featureName();

    public abstract String settingPrefix();

    // check if feature is enabled
    public boolean enabled() {
        return enabled.get();
    }

    protected void doStart() throws ElasticsearchException {
        licensesClientService.register(featureName(),
                trialLicenseOptions,
                new LicensingClientListener());
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    private class LicensingClientListener implements LicensesClientService.Listener {

        @Override
        public void onEnabled() {
            enabled.set(true);
        }

        @Override
        public void onDisabled() {
            enabled.set(false);
        }
    }
}
