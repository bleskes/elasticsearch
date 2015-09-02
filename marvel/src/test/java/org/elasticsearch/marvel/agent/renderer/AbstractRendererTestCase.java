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

package org.elasticsearch.marvel.agent.renderer;

import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.LicensePlugin;
import org.elasticsearch.marvel.MarvelPlugin;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;


@ClusterScope(scope = ESIntegTestCase.Scope.SUITE, randomDynamicTemplates = false, transportClientRatio = 0.0)
public abstract class AbstractRendererTestCase extends ESIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(Node.HTTP_ENABLED, true)
                .put(MarvelSettings.STARTUP_DELAY, "3s")
                .put(MarvelSettings.INTERVAL, "1s")
                .put(MarvelSettings.COLLECTORS, Strings.collectionToCommaDelimitedString(collectors()))
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(LicensePlugin.class, MarvelPlugin.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return nodePlugins();
    }

    protected abstract Collection<String> collectors ();

    protected void waitForMarvelDocs(final String type) throws Exception {
        waitForMarvelDocs(type, 0L);
    }

    protected void waitForMarvelDocs(final String type, final long minCount) throws Exception {
        logger.debug("--> waiting for at least [{}] marvel docs of type [{}] to be collected", minCount, type);
        assertBusy(new Runnable() {
            @Override
            public void run() {
                try {
                    refresh();
                    assertThat(client().prepareCount().setTypes(type).get().getCount(), greaterThan(minCount));
                } catch (Throwable t) {
                    fail("exception when waiting for marvel docs: " + t.getMessage());
                }
            }
        }, 30L, TimeUnit.SECONDS);
    }

    /**
     * Checks if a field exist in a map of values. If the field contains a dot like 'foo.bar'
     * it checks that 'foo' exists in the map of values and that it points to a sub-map. Then
     * it recurses to check if 'bar' exists in the sub-map.
     */
    protected void assertContains(String field, Map<String, Object> values) {
        assertNotNull(field);
        assertNotNull(values);

        int point = field.indexOf('.');
        if (point > -1) {
            assertThat(point, allOf(greaterThan(0), lessThan(field.length())));

            String segment = field.substring(0, point);
            assertTrue(Strings.hasText(segment));

            boolean fieldExists = values.containsKey(segment);
            assertTrue("expecting field [" + segment + "] to be present in marvel document", fieldExists);

            Object value = values.get(segment);
            String next = field.substring(point + 1);
            if (next.length() > 0) {
                assertTrue(value instanceof Map);
                assertContains(next, (Map<String, Object>) value);
            } else {
                assertFalse(value instanceof Map);
            }
        } else {
            assertNotNull(values.get(field));
        }
    }

    protected void assertMarvelTemplateExists() throws Exception {
        final String marvelTemplate = "marvel";

        assertBusy(new Runnable() {
            @Override
            public void run() {
                GetIndexTemplatesResponse response = client().admin().indices().prepareGetTemplates(marvelTemplate).get();
                assertNotNull(response);

                boolean found = false;
                for (IndexTemplateMetaData template : response.getIndexTemplates()) {
                    if (marvelTemplate.equals(template.getName())) {
                        found = true;
                        break;
                    }
                }
                assertTrue("Template [" + marvelTemplate + "] not found", found);
            }
        });
    }
}
