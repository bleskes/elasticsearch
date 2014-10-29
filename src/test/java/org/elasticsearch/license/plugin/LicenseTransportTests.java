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

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.plugin.action.get.GetLicenseRequestBuilder;
import org.elasticsearch.license.plugin.action.get.GetLicenseResponse;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequestBuilder;
import org.elasticsearch.license.plugin.action.put.PutLicenseResponse;
import org.elasticsearch.license.plugin.core.LicensesStatus;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.TEST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@ClusterScope(scope = TEST, numDataNodes = 10)
public class LicenseTransportTests extends AbstractLicensesIntegrationTests {

    @After
    public void beforeTest() throws Exception {
        wipeAllLicenses();
    }

    @Test
    public void testEmptyGetLicense() throws Exception {
        final ActionFuture<GetLicenseResponse> getLicenseFuture = new GetLicenseRequestBuilder(client().admin().cluster()).execute();
        final GetLicenseResponse getLicenseResponse = getLicenseFuture.get();
        assertThat("expected 0 licenses; but got: " + getLicenseResponse.licenses().size(), getLicenseResponse.licenses().size(), equalTo(0));
    }

    @Test
    public void testPutLicense() throws Exception {
        ESLicense signedLicense = generateSignedLicense(TestUtils.SHIELD, TimeValue.timeValueMinutes(2));
        List<ESLicense> actualLicenses = Collections.singletonList(signedLicense);

        // put license
        PutLicenseRequestBuilder putLicenseRequestBuilder = new PutLicenseRequestBuilder(client().admin().cluster())
                .setLicense(actualLicenses);
        PutLicenseResponse putLicenseResponse = putLicenseRequestBuilder.execute().get();
        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.VALID));

        // get license
        GetLicenseResponse getLicenseResponse = new GetLicenseRequestBuilder(client().admin().cluster()).get();
        assertThat(getLicenseResponse.licenses(), notNullValue());

        // check license
        TestUtils.isSame(actualLicenses, getLicenseResponse.licenses());
    }

    @Test
    public void testPutInvalidLicense() throws Exception {
        ESLicense signedLicense = generateSignedLicense(TestUtils.SHIELD, TimeValue.timeValueMinutes(2));

        // modify content of signed license
        ESLicense tamperedLicense = ESLicense.builder()
                .fromLicenseSpec(signedLicense, signedLicense.signature())
                .expiryDate(signedLicense.expiryDate() + 10 * 24 * 60 * 60 * 1000l)
                .verify()
                .build();

        PutLicenseRequestBuilder builder = new PutLicenseRequestBuilder(client().admin().cluster());
        builder.setLicense(Collections.singletonList(tamperedLicense));

        // try to put license (should be invalid)
        final PutLicenseResponse putLicenseResponse = builder.execute().get();
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.INVALID));


        // try to get invalid license
        GetLicenseResponse getLicenseResponse = new GetLicenseRequestBuilder(client().admin().cluster()).get();
        assertThat(getLicenseResponse.licenses().size(), equalTo(0));
    }

    @Test
    public void testPutLicensesForSameFeature() throws Exception {
        ESLicense shortedSignedLicense = generateSignedLicense(TestUtils.SHIELD, TimeValue.timeValueMinutes(2));
        ESLicense longerSignedLicense = generateSignedLicense(TestUtils.SHIELD, TimeValue.timeValueMinutes(5));
        List<ESLicense> actualLicenses = Arrays.asList(longerSignedLicense, shortedSignedLicense);

        // put license
        PutLicenseRequestBuilder putLicenseRequestBuilder = new PutLicenseRequestBuilder(client().admin().cluster())
                .setLicense(actualLicenses);
        PutLicenseResponse putLicenseResponse = putLicenseRequestBuilder.execute().get();
        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.VALID));

        // get should return only one license (with longer expiry date)
        GetLicenseResponse getLicenseResponse = new GetLicenseRequestBuilder(client().admin().cluster()).get();
        assertThat(getLicenseResponse.licenses(), notNullValue());

        // check license
        TestUtils.isSame(Collections.singletonList(longerSignedLicense), getLicenseResponse.licenses());
    }

    @Test
    public void testPutLicensesForMultipleFeatures() throws Exception {
        ESLicense shieldLicense = generateSignedLicense(TestUtils.SHIELD, TimeValue.timeValueMinutes(2));
        ESLicense marvelLicense = generateSignedLicense(TestUtils.MARVEL, TimeValue.timeValueMinutes(5));
        List<ESLicense> actualLicenses = Arrays.asList(marvelLicense, shieldLicense);

        // put license
        PutLicenseRequestBuilder putLicenseRequestBuilder = new PutLicenseRequestBuilder(client().admin().cluster())
                .setLicense(actualLicenses);
        PutLicenseResponse putLicenseResponse = putLicenseRequestBuilder.execute().get();
        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.VALID));

        // get should return both the licenses
        GetLicenseResponse getLicenseResponse = new GetLicenseRequestBuilder(client().admin().cluster()).get();
        assertThat(getLicenseResponse.licenses(), notNullValue());

        // check license
        TestUtils.isSame(actualLicenses, getLicenseResponse.licenses());
    }

    @Test
    public void testPutMultipleLicensesForMultipleFeatures() throws Exception {
        ESLicense shortedSignedLicense = generateSignedLicense(TestUtils.SHIELD, TimeValue.timeValueMinutes(2));
        ESLicense longerSignedLicense = generateSignedLicense(TestUtils.SHIELD, TimeValue.timeValueMinutes(5));
        ESLicense marvelLicense = generateSignedLicense(TestUtils.MARVEL, TimeValue.timeValueMinutes(5));
        List<ESLicense> actualLicenses = Arrays.asList(marvelLicense, shortedSignedLicense, longerSignedLicense);

        // put license
        PutLicenseRequestBuilder putLicenseRequestBuilder = new PutLicenseRequestBuilder(client().admin().cluster())
                .setLicense(actualLicenses);
        PutLicenseResponse putLicenseResponse = putLicenseRequestBuilder.execute().get();
        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));
        assertThat(putLicenseResponse.status(), equalTo(LicensesStatus.VALID));

        // get should return both the licenses
        GetLicenseResponse getLicenseResponse = new GetLicenseRequestBuilder(client().admin().cluster()).get();
        assertThat(getLicenseResponse.licenses(), notNullValue());

        // check license (should get the longest expiry time for all unique features)
        TestUtils.isSame(Arrays.asList(marvelLicense, longerSignedLicense), getLicenseResponse.licenses());
    }

}
