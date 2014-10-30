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

package org.elasticsearch.license.plugin;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.plugin.core.LicensesClientService;
import org.elasticsearch.license.plugin.core.LicensesService;

import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class TestPluginService extends AbstractLifecycleComponent<TestPluginService> {

    private LicensesClientService licensesClientService;

    // should be the same string used by the license Manger to generate
    // signed license
    static final String FEATURE_NAME = "shield";

    // specify the trial license spec for the feature
    // example: 30 day trial on 1000 nodes
    final LicensesService.TrialLicenseOptions trialLicenseOptions;

    private AtomicBoolean enabled = new AtomicBoolean(false);

    @Inject
    public TestPluginService(Settings settings, LicensesClientService licensesClientService) {
        super(settings);
        this.licensesClientService = licensesClientService;
        int durationInSec = settings.getAsInt("test_consumer_plugin.trial_license_duration_in_seconds", -1);
        logger.info("Trial license Duration in seconds: " + durationInSec);
        if (durationInSec == -1) {
            this.trialLicenseOptions = null;
        } else {
            this.trialLicenseOptions = new LicensesService.TrialLicenseOptions(TimeValue.timeValueSeconds(durationInSec), 1000);
        }
    }

    // check if feature is enabled
    public boolean enabled() {
        return enabled.get();
    }

    protected void doStart() throws ElasticsearchException {
        licensesClientService.register(FEATURE_NAME,
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
