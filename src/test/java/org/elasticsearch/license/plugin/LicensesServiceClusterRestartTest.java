/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.license.plugin;

import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.plugin.action.get.GetLicenseRequestBuilder;
import org.elasticsearch.license.plugin.action.get.GetLicenseResponse;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequestBuilder;
import org.elasticsearch.license.plugin.action.put.PutLicenseResponse;
import org.elasticsearch.license.plugin.core.LicensesStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.TEST;
import static org.hamcrest.CoreMatchers.equalTo;

@ClusterScope(scope = TEST, numDataNodes = 0)
public class LicensesServiceClusterRestartTest extends AbstractLicensesIntegrationTests {

    @Override
    protected Settings transportClientSettings() {
        return super.transportClientSettings();
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("gateway.type", "local")
                .put("format", "json")
                .build();
    }

    @Before
    public void beforeTest() throws Exception {
        wipeAllLicenses();
    }

    @Test
    public void testClusterRestart() throws Exception {
        logger.info("--> starting 1 node");
        internalCluster().startNode();
        ensureGreen();
        wipeAllLicenses();

        final List<ESLicense> esLicenses = generateAndPutLicense();
        getAndCheckLicense(esLicenses);
        logger.info("--> restart all nodes");
        internalCluster().fullRestart();
        ensureYellow();

        getAndCheckLicense(esLicenses);
    }

    private List<ESLicense> generateAndPutLicense() throws Exception {
        ClusterAdminClient cluster = internalCluster().client().admin().cluster();
        ESLicense license = generateSignedLicense("shield", TimeValue.timeValueMinutes(1));
        PutLicenseRequestBuilder putLicenseRequestBuilder = new PutLicenseRequestBuilder(cluster);
        final List<ESLicense> putLicenses = Arrays.asList(license);
        putLicenseRequestBuilder.setLicense(putLicenses);
        ensureGreen();

        final PutLicenseResponse putLicenseResponse = putLicenseRequestBuilder.execute().get();

        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.VALID));

        return putLicenses;
    }

    private void getAndCheckLicense(List<ESLicense> license) {
        ClusterAdminClient cluster = internalCluster().client().admin().cluster();
        final GetLicenseResponse response = new GetLicenseRequestBuilder(cluster).get();
        assertThat(response.licenses().size(), equalTo(1));
        TestUtils.isSame(license, response.licenses());
    }
}
