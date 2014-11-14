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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.plugin.core.LicensesClientService;
import org.elasticsearch.license.plugin.core.LicensesService;
import org.elasticsearch.shield.ShieldPlugin;

/**
 *
 */
public class LicenseService extends AbstractLifecycleComponent<LicenseService> {

    public static final String FEATURE_NAME = ShieldPlugin.NAME;

    private static final LicensesService.TrialLicenseOptions TRIAL_LICENSE_OPTIONS =
            new LicensesService.TrialLicenseOptions(TimeValue.timeValueHours(30 * 24), 1000);

    private final LicensesClientService licensesClientService;
    private final LicenseEventsNotifier notifier;

    private boolean enabled = false;

    @Inject
    public LicenseService(Settings settings, LicensesClientService licensesClientService, LicenseEventsNotifier notifier) {
        super(settings);
        this.licensesClientService = licensesClientService;
        this.notifier = notifier;
    }

    public synchronized boolean enabled() {
        return enabled;
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        licensesClientService.register(FEATURE_NAME, TRIAL_LICENSE_OPTIONS, new InternalListener());
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    class InternalListener implements LicensesClientService.Listener {

        @Override
        public void onEnabled() {
            synchronized (LicenseService.this) {
                enabled = true;
                notifier.notifyEnabled();
            }
        }

        @Override
        public void onDisabled() {
            synchronized (LicenseService.this) {
                enabled = false;
                notifier.notifyDisabled();
            }
        }
    }
}
