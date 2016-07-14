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

package org.elasticsearch.xpack.security;

import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.support.AbstractSecurityModule;
import org.elasticsearch.xpack.XPackPlugin;

/**
 *
 */
public class SecurityModule extends AbstractSecurityModule {

    private final SecurityLicenseState securityLicenseState;

    public SecurityModule(Settings settings, SecurityLicenseState securityLicenseState) {
        super(settings);
        this.securityLicenseState = securityLicenseState;
    }

    @Override
    protected void configure(boolean clientMode) {
        if (clientMode) {
            return;
        }

        if (securityLicenseState != null) {
            bind(SecurityLicenseState.class).toInstance(securityLicenseState);
        } else {
            bind(SecurityLicenseState.class).toProvider(Providers.<SecurityLicenseState>of(null));
        }

        XPackPlugin.bindFeatureSet(binder(), SecurityFeatureSet.class);

        if (securityEnabled) {
            bind(SecurityContext.Secure.class).asEagerSingleton();
            bind(SecurityContext.class).to(SecurityContext.Secure.class);
            bind(SecurityLifecycleService.class).asEagerSingleton();
        } else {
            bind(SecurityContext.class).toInstance(SecurityContext.Insecure.INSTANCE);
        }
    }

}
