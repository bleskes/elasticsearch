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
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.licensor.tools.KeyPairGeneratorTool;
import org.elasticsearch.license.plugin.action.put.PutLicenseAction;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequest;
import org.elasticsearch.license.plugin.action.put.PutLicenseResponse;
import org.elasticsearch.license.plugin.cluster.LicensesMetaData;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class LicenseTransportTests extends ElasticsearchIntegrationTest {

    static {
        MetaData.registerFactory(LicensesMetaData.TYPE, LicensesMetaData.FACTORY);
    }

    private static String pubKeyPath = null;
    private static String priKeyPath = null;
    private static String keyPass = null;

    @BeforeClass
    public static void setup() throws IOException {

        // Generate temp KeyPair spec
        File privateKeyFile = File.createTempFile("privateKey", ".key");
        File publicKeyFile = File.createTempFile("publicKey", ".key");
        LicenseTransportTests.pubKeyPath = publicKeyFile.getAbsolutePath();
        LicenseTransportTests.priKeyPath = privateKeyFile.getAbsolutePath();
        assert privateKeyFile.delete();
        assert publicKeyFile.delete();
        String keyPass = "password";
        LicenseTransportTests.keyPass = keyPass;

        // Generate keyPair
        String[] args = new String[6];
        args[0] = "--publicKeyPath";
        args[1] = LicenseTransportTests.pubKeyPath;
        args[2] = "--privateKeyPath";
        args[3] = LicenseTransportTests.priKeyPath;
        args[4] = "--keyPass";
        args[5] = LicenseTransportTests.keyPass;
        KeyPairGeneratorTool.main(args);
    }

    @Test
    public void testPutLicense() throws ParseException, ExecutionException, InterruptedException {

        Map<ESLicenses.FeatureType, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes featureAttributes =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2015-12-13");
        map.put(ESLicenses.FeatureType.SHIELD, featureAttributes);

        String licenseString = TestUtils.generateESLicenses(map);

        PutLicenseRequest putLicenseRequest = new PutLicenseRequest();
        putLicenseRequest.license(licenseString);

        final ActionFuture<PutLicenseResponse> execute = client().admin().cluster().execute(PutLicenseAction.INSTANCE, putLicenseRequest);

        final PutLicenseResponse putLicenseResponse = execute.get();

        assertThat(putLicenseResponse.isAcknowledged(), equalTo(true));

    }
}
