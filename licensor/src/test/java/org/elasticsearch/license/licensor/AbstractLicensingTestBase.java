package org.elasticsearch.license.licensor;/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated
 *  All Rights Reserved.
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

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.core.DateUtils;
import org.elasticsearch.license.core.License;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.After;
import org.junit.Before;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractLicensingTestBase extends ElasticsearchTestCase {

    protected String pubKeyPath = null;
    protected String priKeyPath = null;

    @Before
    public void setup() throws Exception {
        pubKeyPath = getResourcePath("/public.key");
        priKeyPath = getResourcePath("/private.key");
    }

    @After
    public void cleanUp() {
        pubKeyPath = null;
        priKeyPath = null;
    }

    public static Set<License> generateSignedLicenses(List<TestUtils.LicenseSpec> licenseSpecs, String pubKeyPath, String priKeyPath) throws Exception {
        LicenseSigner signer = new LicenseSigner(priKeyPath, pubKeyPath);
        Set<License> unSignedLicenses = new HashSet<>();
        for (TestUtils.LicenseSpec spec : licenseSpecs) {
            License.Builder builder = License.builder()
                    .uid(spec.uid)
                    .feature(spec.feature)
                    .type(spec.type)
                    .subscriptionType(spec.subscriptionType)
                    .issuedTo(spec.issuedTo)
                    .issuer(spec.issuer)
                    .maxNodes(spec.maxNodes);

            if (spec.expiryDate != null) {
                builder.expiryDate(DateUtils.endOfTheDay(spec.expiryDate));
            } else {
                builder.expiryDate(spec.expiryDateInMillis);
            }
            if (spec.issueDate != null) {
                builder.issueDate(DateUtils.beginningOfTheDay(spec.issueDate));
            } else {
                builder.issueDate(spec.issueDateInMillis);
            }
            unSignedLicenses.add(builder.build());
        }
        return signer.sign(unSignedLicenses);
    }

    public static License generateSignedLicense(String feature, TimeValue expiryDuration, String pubKeyPath, String priKeyPath) throws Exception {
        return generateSignedLicense(feature, -1, expiryDuration, pubKeyPath, priKeyPath);
    }

    public static License generateSignedLicense(String feature, long issueDate, TimeValue expiryDuration, String pubKeyPath, String priKeyPath) throws Exception {
        long issue = (issueDate != -1l) ? issueDate : System.currentTimeMillis();
        final License licenseSpec = License.builder()
                .uid(UUID.randomUUID().toString())
                .feature(feature)
                .expiryDate(issue + expiryDuration.getMillis())
                .issueDate(issue)
                .type("subscription")
                .subscriptionType("gold")
                .issuedTo("customer")
                .issuer("elasticsearch")
                .maxNodes(5)
                .build();

        LicenseSigner signer = new LicenseSigner(priKeyPath, pubKeyPath);
        return signer.sign(licenseSpec);
    }

    public String getTestPriKeyPath() throws Exception {
        return getResourcePath("/private.key");
    }

    public String getTestPubKeyPath() throws Exception {
        return getResourcePath("/public.key");
    }

    private String getResourcePath(String resource) throws Exception {
        return getDataPath(resource).toString();
    }
}
