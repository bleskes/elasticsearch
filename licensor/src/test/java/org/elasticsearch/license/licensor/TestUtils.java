/*
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
package org.elasticsearch.license.licensor;

import org.elasticsearch.common.joda.DateMathParser;
import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.core.DateUtils;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.core.Licenses;
import org.hamcrest.MatcherAssert;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static com.carrotsearch.randomizedtesting.RandomizedTest.*;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.test.ElasticsearchTestCase.randomFrom;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TestUtils {

    public static final String PUBLIC_KEY_RESOURCE = "/public.key";
    public static final String PRIVATE_KEY_RESOURCE = "/private.key";

    private final static FormatDateTimeFormatter formatDateTimeFormatter = Joda.forPattern("yyyy-MM-dd");
    private final static DateMathParser dateMathParser = new DateMathParser(formatDateTimeFormatter);
    private final static DateTimeFormatter dateTimeFormatter = formatDateTimeFormatter.printer();

    public static void isSame(Set<License> firstLicenses, Set<License> secondLicenses) {

        // we do the verifyAndBuild to make sure we weed out any expired licenses
        final Map<String, License> licenses1 = Licenses.reduceAndMap(firstLicenses);
        final Map<String, License> licenses2 = Licenses.reduceAndMap(secondLicenses);

        // check if the effective licenses have the same feature set
        assertThat(licenses1.size(), equalTo(licenses2.size()));

        // for every feature license, check if all the attributes are the same
        for (String featureType : licenses1.keySet()) {
            License license1 = licenses1.get(featureType);
            License license2 = licenses2.get(featureType);
            isSame(license1, license2);
        }
    }

    public static void isSame(License license1, License license2) {
        assertThat(license1.uid(), equalTo(license2.uid()));
        assertThat(license1.feature(), equalTo(license2.feature()));
        assertThat(license1.subscriptionType(), equalTo(license2.subscriptionType()));
        assertThat(license1.type(), equalTo(license2.type()));
        assertThat(license1.issuedTo(), equalTo(license2.issuedTo()));
        assertThat(license1.signature(), equalTo(license2.signature()));
        assertThat(license1.expiryDate(), equalTo(license2.expiryDate()));
        assertThat(license1.issueDate(), equalTo(license2.issueDate()));
        assertThat(license1.maxNodes(), equalTo(license2.maxNodes()));
    }

    public static String dumpLicense(License license) throws Exception {
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        Licenses.toXContent(Collections.singletonList(license), builder, ToXContent.EMPTY_PARAMS);
        builder.flush();
        return builder.string();
    }

    public static String dateMathString(String time, final long now) {
        return dateTimeFormatter.print(dateMathParser.parse(time, new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return now;
            }
        }));
    }

    public static long dateMath(String time, final long now) {
        return dateMathParser.parse(time, new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return now;
            }
        });
    }

    public static LicenseSpec generateRandomLicenseSpec() {
        boolean datesInMillis = randomBoolean();
        long now = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString();
        String feature = "feature__" + randomInt();
        String issuer = "issuer__"  + randomInt();
        String issuedTo = "issuedTo__" + randomInt();
        String type = randomFrom("subscription", "internal", "development");
        String subscriptionType = randomFrom("none", "gold", "silver", "platinum");
        int maxNodes = randomIntBetween(5, 100);
        if (datesInMillis) {
            long issueDateInMillis = dateMath("now", now);
            long expiryDateInMillis = dateMath("now+10d/d", now);
            return new LicenseSpec(uid, feature, issueDateInMillis, expiryDateInMillis, type, subscriptionType, issuedTo, issuer, maxNodes);
        } else {
            String issueDate = dateMathString("now", now);
            String expiryDate = dateMathString("now+10d/d", now);
            return new LicenseSpec(uid, feature, issueDate, expiryDate, type, subscriptionType, issuedTo, issuer, maxNodes);
        }
    }

    public static String generateLicenseSpecString(List<LicenseSpec> licenseSpecs) throws IOException {
        XContentBuilder licenses = jsonBuilder();
        licenses.startObject();
        licenses.startArray("licenses");
        for (LicenseSpec licenseSpec : licenseSpecs) {
            licenses.startObject()
                    .field("uid", licenseSpec.uid)
                    .field("type", licenseSpec.type)
                    .field("subscription_type", licenseSpec.subscriptionType)
                    .field("issued_to", licenseSpec.issuedTo)
                    .field("issuer", licenseSpec.issuer)
                    .field("feature", licenseSpec.feature)
                    .field("max_nodes", licenseSpec.maxNodes);

            if (licenseSpec.issueDate != null) {
                licenses.field("issue_date", licenseSpec.issueDate);
            } else {
                licenses.field("issue_date_in_millis", licenseSpec.issueDateInMillis);
            }
            if (licenseSpec.expiryDate != null) {
                licenses.field("expiry_date", licenseSpec.expiryDate);
            } else {
                licenses.field("expiry_date_in_millis", licenseSpec.expiryDateInMillis);
            }
            licenses.endObject();
        }
        licenses.endArray();
        licenses.endObject();
        return licenses.string();
    }

    public static void assertLicenseSpec(LicenseSpec spec, License license) {
        MatcherAssert.assertThat(license.uid(), equalTo(spec.uid));
        MatcherAssert.assertThat(license.feature(), equalTo(spec.feature));
        MatcherAssert.assertThat(license.issuedTo(), equalTo(spec.issuedTo));
        MatcherAssert.assertThat(license.issuer(), equalTo(spec.issuer));
        MatcherAssert.assertThat(license.type(), equalTo(spec.type));
        MatcherAssert.assertThat(license.subscriptionType(), equalTo(spec.subscriptionType));
        MatcherAssert.assertThat(license.maxNodes(), equalTo(spec.maxNodes));
        if (spec.issueDate != null) {
            MatcherAssert.assertThat(license.issueDate(), equalTo(DateUtils.beginningOfTheDay(spec.issueDate)));
        } else {
            MatcherAssert.assertThat(license.issueDate(), equalTo(spec.issueDateInMillis));
        }
        if (spec.expiryDate != null) {
            MatcherAssert.assertThat(license.expiryDate(), equalTo(DateUtils.endOfTheDay(spec.expiryDate)));
        } else {
            MatcherAssert.assertThat(license.expiryDate(), equalTo(spec.expiryDateInMillis));
        }
    }

    public static class LicenseSpec {
        public final String feature;
        public final String issueDate;
        public final long issueDateInMillis;
        public final String expiryDate;
        public final long expiryDateInMillis;
        public final String uid;
        public final String type;
        public final String subscriptionType;
        public final String issuedTo;
        public final String issuer;
        public final int maxNodes;

        public LicenseSpec(String feature, String issueDate, String expiryDate) {
            this(UUID.randomUUID().toString(), feature, issueDate, expiryDate, "trial", "none", "customer", "elasticsearch", 5);
        }

        public LicenseSpec(String uid, String feature, long issueDateInMillis, long expiryDateInMillis, String type,
                           String subscriptionType, String issuedTo, String issuer, int maxNodes) {
            this.feature = feature;
            this.issueDateInMillis = issueDateInMillis;
            this.issueDate = null;
            this.expiryDateInMillis = expiryDateInMillis;
            this.expiryDate = null;
            this.uid = uid;
            this.type = type;
            this.subscriptionType = subscriptionType;
            this.issuedTo = issuedTo;
            this.issuer = issuer;
            this.maxNodes = maxNodes;
        }

        public LicenseSpec(String uid, String feature, String issueDate, String expiryDate, String type,
                           String subscriptionType, String issuedTo, String issuer, int maxNodes) {
            this.feature = feature;
            this.issueDate = issueDate;
            this.issueDateInMillis = -1;
            this.expiryDate = expiryDate;
            this.expiryDateInMillis = -1;
            this.uid = uid;
            this.type = type;
            this.subscriptionType = subscriptionType;
            this.issuedTo = issuedTo;
            this.issuer = issuer;
            this.maxNodes = maxNodes;
        }
    }
}
