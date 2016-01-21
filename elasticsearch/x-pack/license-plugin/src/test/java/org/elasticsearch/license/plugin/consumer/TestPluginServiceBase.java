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
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.gateway.GatewayService;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.LicenseState;
import org.elasticsearch.license.plugin.core.Licensee;
import org.elasticsearch.license.plugin.core.LicensesService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class TestPluginServiceBase extends AbstractLifecycleComponent<TestPluginServiceBase> implements ClusterStateListener, Licensee {

    private LicensesService licensesClientService;
    private final ClusterService clusterService;
    final boolean eagerLicenseRegistration;
    public final AtomicBoolean registered = new AtomicBoolean(false);
    private AtomicReference<LicenseState> state = new AtomicReference<>(LicenseState.DISABLED);

    public TestPluginServiceBase(boolean eagerLicenseRegistration, Settings settings, LicensesService licensesClientService, ClusterService clusterService) {
        super(settings);
        this.eagerLicenseRegistration = eagerLicenseRegistration;
        this.licensesClientService = licensesClientService;
        int trialDurationInSec = settings.getAsInt("_trial_license_duration_in_seconds", -1);
        if (trialDurationInSec != -1) {
            licensesClientService.setTrialLicenseDuration(TimeValue.timeValueSeconds(trialDurationInSec));
        }
        int graceDurationInSec = settings.getAsInt("_grace_duration_in_seconds", 5);
        licensesClientService.setGracePeriodDuration(TimeValue.timeValueSeconds(graceDurationInSec));
        if (!eagerLicenseRegistration) {
            this.clusterService = clusterService;
            clusterService.add(this);
        } else {
            this.clusterService = null;
        }
    }

    // should be the same string used by the license Manger to generate
    // signed license
    public abstract String id();

    // check if feature is enabled
    public LicenseState state() {
        return state.get();
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        if (!eagerLicenseRegistration && !event.state().blocks().hasGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK)) {
            if (registered.compareAndSet(false, true)) {
                logger.info("Registering to licensesService [lazy]");
                licensesClientService.register(this);
            }
        }
    }

    protected void doStart() throws ElasticsearchException {
        if (eagerLicenseRegistration) {
            if (registered.compareAndSet(false, true)) {
                logger.info("Registering to licensesService [eager]");
                licensesClientService.register(this);
            }
        }
    }

    @Override
    public String[] expirationMessages() {
        return new String[0];
    }

    @Override
    public String[] acknowledgmentMessages(License currentLicense, License newLicense) {
        return new String[0];
    }

    @Override
    public void onChange(Status status) {
        this.state.set(status.getLicenseState());
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        if (clusterService != null) {
            clusterService.remove(this);
        }
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }
}
