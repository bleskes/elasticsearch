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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.support.AbstractShieldModule;

/**
 *
 */
public class LicenseModule extends AbstractShieldModule.Node {

    private final ShieldLicenseState shieldLicenseState;

    public LicenseModule(Settings settings, ShieldLicenseState shieldLicenseState) {
        super(settings);
        verifyLicensePlugin();
        this.shieldLicenseState = shieldLicenseState;
    }

    @Override
    protected void configureNode() {
        bind(ShieldLicensee.class).asEagerSingleton();
        bind(ShieldLicenseState.class).toInstance(shieldLicenseState);
    }

    private void verifyLicensePlugin() {
        try {
            getClass().getClassLoader().loadClass("org.elasticsearch.license.plugin.LicensePlugin");
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException("shield plugin requires the license plugin to be installed");
        }
    }

}
