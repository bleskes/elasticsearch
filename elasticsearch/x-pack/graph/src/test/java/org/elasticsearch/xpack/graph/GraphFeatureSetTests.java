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

package org.elasticsearch.xpack.graph;

import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.XPackLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class GraphFeatureSetTests extends ESTestCase {

    private XPackLicenseState licenseState;
    private NamedWriteableRegistry namedWriteableRegistry;

    @Before
    public void init() throws Exception {
        licenseState = mock(XPackLicenseState.class);
        namedWriteableRegistry = mock(NamedWriteableRegistry.class);
    }

    public void testWritableRegistration() throws Exception {
        new GraphFeatureSet(Settings.EMPTY, licenseState, namedWriteableRegistry);
        verify(namedWriteableRegistry).register(eq(GraphFeatureSet.Usage.class), eq("xpack.usage.graph"), anyObject());
    }

    public void testAvailable() throws Exception {
        GraphFeatureSet featureSet = new GraphFeatureSet(Settings.EMPTY, licenseState, namedWriteableRegistry);
        boolean available = randomBoolean();
        when(licenseState.isGraphAllowed()).thenReturn(available);
        assertThat(featureSet.available(), is(available));
    }

    public void testEnabled() throws Exception {
        boolean enabled = randomBoolean();
        Settings.Builder settings = Settings.builder();
        if (enabled) {
            if (randomBoolean()) {
                settings.put("xpack.graph.enabled", enabled);
            }
        } else {
            settings.put("xpack.graph.enabled", enabled);
        }
        GraphFeatureSet featureSet = new GraphFeatureSet(settings.build(), licenseState, namedWriteableRegistry);
        assertThat(featureSet.enabled(), is(enabled));
    }

}
