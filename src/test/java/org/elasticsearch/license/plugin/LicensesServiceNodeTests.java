package org.elasticsearch.license.plugin;

import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.TEST;
import static org.hamcrest.Matchers.equalTo;

/**
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = TEST, numDataNodes = 10, numClientNodes = 0)
public class LicensesServiceNodeTests extends AbstractLicensesIntegrationTests {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("test_consumer_plugin.trial_license_duration_in_seconds", 10)
                .putArray("plugin.types", LicensePlugin.class.getName(), TestConsumerPlugin.class.getName())
                .put(InternalNode.HTTP_ENABLED, true)
                .build();
    }

    @Test
    @TestLogging("_root:DEBUG")
    public void testPluginStatus() throws Exception {
        final Iterable<TestPluginService> testPluginServices = internalCluster().getDataNodeInstances(TestPluginService.class);
        assertThat(awaitBusy(new Predicate<Object>() {
            @Override
            public boolean apply(Object o) {
                for (TestPluginService pluginService : testPluginServices) {
                    if (!pluginService.enabled()) {
                        return false;
                    }
                }
                return true;
            }
        }, 1, TimeUnit.MINUTES), equalTo(true));

    }

}
