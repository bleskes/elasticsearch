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

package org.elasticsearch.license;

import org.elasticsearch.license.manager.ESLicenseManager;
import org.junit.BeforeClass;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

public class AbstractLicensingTestBase {

    protected static String pubKeyPath = null;
    protected static String priKeyPath = null;

    @BeforeClass
    public static void setup() throws Exception {
        pubKeyPath = getResourcePath("/public.key");
        priKeyPath = getResourcePath("/private.key");

    }

    public static String getTestPriKeyPath() throws Exception {
        return getResourcePath("/private.key");
    }

    public static String getTestPubKeyPath() throws Exception {
        return getResourcePath("/public.key");
    }

    private static String getResourcePath(String resource) throws Exception {
        URL url = ESLicenseManager.class.getResource(resource);
        return url.toURI().getPath();
    }

    public String generateSignedLicenses(Map<String, TestUtils.FeatureAttributes> map) throws IOException, ParseException {
        String licenseString = TestUtils.generateESLicenses(map);
        return TestUtils.runLicenseGenerationTool(licenseString, pubKeyPath, priKeyPath);
    }
}
