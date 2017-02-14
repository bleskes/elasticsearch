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

package org.elasticsearch.xpack.ml;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.XPackFeatureSet;
import org.junit.Before;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MachineLearningFeatureSetTests extends ESTestCase {

    private XPackLicenseState licenseState;

    @Before
    public void init() throws Exception {
        licenseState = mock(XPackLicenseState.class);
    }

    public void testAvailable() throws Exception {
        MachineLearningFeatureSet featureSet = new MachineLearningFeatureSet(Settings.EMPTY, licenseState);
        boolean available = randomBoolean();
        when(licenseState.isMachineLearningAllowed()).thenReturn(available);
        assertThat(featureSet.available(), is(available));
        assertThat(featureSet.usage().available(), is(available));

        BytesStreamOutput out = new BytesStreamOutput();
        featureSet.usage().writeTo(out);
        XPackFeatureSet.Usage serializedUsage = new MachineLearningFeatureSet.Usage(out.bytes().streamInput());
        assertThat(serializedUsage.available(), is(available));
    }

    public void testEnabled() throws Exception {
        boolean enabled = randomBoolean();
        Settings.Builder settings = Settings.builder();
        if (enabled) {
            settings.put("xpack.ml.enabled", enabled);
        } else {
            if (randomBoolean()) {
                settings.put("xpack.ml.enabled", enabled);
            }
        }
        MachineLearningFeatureSet featureSet = new MachineLearningFeatureSet(settings.build(), licenseState);
        assertThat(featureSet.enabled(), is(enabled));
        assertThat(featureSet.usage().enabled(), is(enabled));

        BytesStreamOutput out = new BytesStreamOutput();
        featureSet.usage().writeTo(out);
        XPackFeatureSet.Usage serializedUsage = new MachineLearningFeatureSet.Usage(out.bytes().streamInput());
        assertThat(serializedUsage.enabled(), is(enabled));
    }

}
