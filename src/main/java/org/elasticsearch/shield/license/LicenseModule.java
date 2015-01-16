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

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.LicenseVersion;
import org.elasticsearch.shield.ShieldVersion;
import org.elasticsearch.shield.support.AbstractShieldModule;

/**
 *
 */
public class LicenseModule extends AbstractShieldModule.Node {

    public LicenseModule(Settings settings) {
        super(settings);
        verifyLicensePlugin();
    }

    @Override
    protected void configureNode() {
        bind(LicenseService.class).asEagerSingleton();
        bind(LicenseEventsNotifier.class).asEagerSingleton();
    }

    private void verifyLicensePlugin() {
        try {
            getClass().getClassLoader().loadClass("org.elasticsearch.license.plugin.LicensePlugin");
        } catch (ClassNotFoundException cnfe) {
            throw new ElasticsearchIllegalStateException("shield plugin requires the elasticsearch-license plugin to be installed");
        }

        if (LicenseVersion.CURRENT.before(ShieldVersion.CURRENT.minLicenseCompatibilityVersion)) {
            throw new ElasticsearchIllegalStateException("shield [" + ShieldVersion.CURRENT +
                    "] requires minumum elasticsearch-license plugin version [" + ShieldVersion.CURRENT.minLicenseCompatibilityVersion +
                    "], but installed elasticsearch-license plugin version is [" + LicenseVersion.CURRENT + "]");
        }
    }

}