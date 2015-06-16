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

package org.elasticsearch.watcher.test.rest;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.LicenseExpiredException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.PluginsService;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.test.rest.RestTestCandidate;
import org.elasticsearch.watcher.WatcherPlugin;
import org.elasticsearch.watcher.license.LicenseIntegrationTests;
import org.elasticsearch.watcher.license.LicenseIntegrationTests.MockLicenseService;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 */
public class WatcherDisabledLicenseRestTests extends WatcherRestTests {

    @Override
    protected Class<? extends Plugin> licensePluginClass() {
        return LicenseIntegrationTests.MockLicensePlugin.class;
    }

    public WatcherDisabledLicenseRestTests(@Name("yaml") RestTestCandidate testCandidate) {
        super(testCandidate);
    }

    @Test
    public void test() throws IOException {
        disableLicensing();
        try {
            super.test();
            fail();
        } catch(AssertionError ae) {
            if (ae.getMessage() == null || ae.getMessage().contains("not supported")){
                //This was a test testing the "hijacked" methods
                return;
            }
            if (shieldEnabled) {
                assertThat(ae.getMessage(), containsString("returned [403 Forbidden]"));
                assertThat(ae.getMessage(), containsString("is unauthorized for user [admin]"));
            } else {
                assertThat(ae.getMessage(), containsString("unauthorized"));
                assertThat(ae.getMessage(), containsString(LicenseExpiredException.class.getSimpleName()));
            }
        }
    }

    public static void disableLicensing() {
        for (MockLicenseService service : internalCluster().getInstances(MockLicenseService.class)) {
            service.disable();
        }
    }
}
