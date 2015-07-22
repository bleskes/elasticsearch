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

package org.elasticsearch.marvel.license;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.SysGlobals;
import com.google.common.collect.ImmutableSet;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.LicensesClientService;
import org.elasticsearch.license.plugin.core.LicensesService;
import org.elasticsearch.marvel.MarvelPlugin;
import org.elasticsearch.marvel.mode.Mode;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.SUITE;
import static org.hamcrest.Matchers.equalTo;

@ClusterScope(scope = SUITE, transportClientRatio = 0, numClientNodes = 0)
public class LicenseIntegrationTests extends ElasticsearchIntegrationTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("plugin.types", MarvelPlugin.class.getName() + "," + MockLicensePlugin.class.getName())
                .build();
    }

    @Test
    public void testEnableDisableLicense() {
        assertMarvelMode(Mode.STANDARD);
        disableLicensing();

        assertMarvelMode(Mode.LITE);
        enableLicensing();

        assertMarvelMode(Mode.STANDARD);
    }

    private void assertMarvelMode(Mode expected) {
        LicenseService licenseService = internalCluster().getInstance(LicenseService.class);
        assertNotNull(licenseService);
        assertThat(licenseService.mode(), equalTo(expected));
    }


    public static void disableLicensing() {
        for (MockLicenseService service : internalCluster().getInstances(MockLicenseService.class)) {
            service.disable();
        }
    }

    public static void enableLicensing() {
        for (MockLicenseService service : internalCluster().getInstances(MockLicenseService.class)) {
            service.enable();
        }
    }

    public static class MockLicensePlugin extends AbstractPlugin {

        public static final String NAME = "internal-test-licensing";

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public String description() {
            return name();
        }

        @Override
        public Collection<Class<? extends Module>> modules() {
            return ImmutableSet.<Class<? extends Module>>of(InternalLicenseModule.class);
        }
    }

    public static class InternalLicenseModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MockLicenseService.class).asEagerSingleton();
            bind(LicensesClientService.class).to(MockLicenseService.class);
        }
    }

    public static class MockLicenseService extends AbstractComponent implements LicensesClientService {

        static final License DUMMY_LICENSE = License.builder()
                .feature(LicenseService.FEATURE_NAME)
                .expiryDate(System.currentTimeMillis())
                .issueDate(System.currentTimeMillis())
                .issuedTo("LicensingTests")
                .issuer("test")
                .maxNodes(Integer.MAX_VALUE)
                .signature("_signature")
                .type("standard")
                .subscriptionType("all_is_good")
                .uid(String.valueOf(RandomizedTest.systemPropertyAsInt(SysGlobals.CHILDVM_SYSPROP_JVM_ID, 0)) + System.identityHashCode(LicenseIntegrationTests.class))
                .build();

        private final List<Listener> listeners = new ArrayList<>();

        @Inject
        public MockLicenseService(Settings settings) {
            super(settings);
            enable();
        }

        @Override
        public void register(String s, LicensesService.TrialLicenseOptions trialLicenseOptions, Collection<LicensesService.ExpirationCallback> collection, Listener listener) {
            listeners.add(listener);
            enable();
        }

        public void enable() {
            // enabled all listeners (incl. shield)
            for (Listener listener : listeners) {
                listener.onEnabled(DUMMY_LICENSE);
            }
        }

        public void disable() {
            // only disable watcher listener (we need shield to work)
            for (Listener listener : listeners) {
                listener.onDisabled(DUMMY_LICENSE);
            }
        }
    }
}
