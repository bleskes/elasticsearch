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

package org.elasticsearch.license.licensor;

import org.elasticsearch.common.joda.DateMathParser;
import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.DateUtils;
import org.elasticsearch.license.License;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.MatcherAssert;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomBoolean;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomInt;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomIntBetween;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.test.ESTestCase.randomFrom;
import static org.hamcrest.core.IsEqual.equalTo;

public class TestUtils {

    public static final String PUBLIC_KEY_RESOURCE = "/public.key";
    public static final String PRIVATE_KEY_RESOURCE = "/private.key";

    private static final FormatDateTimeFormatter formatDateTimeFormatter =
            Joda.forPattern("yyyy-MM-dd");
    private static final DateMathParser dateMathParser =
            new DateMathParser(formatDateTimeFormatter);
    private static final DateTimeFormatter dateTimeFormatter = formatDateTimeFormatter.printer();

    public static String dumpLicense(License license) throws Exception {
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        builder.startObject();
        builder.startObject("license");
        license.toInnerXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.endObject();
        builder.endObject();
        return builder.string();
    }

    public static String dateMathString(String time, final long now) {
        return dateTimeFormatter.print(dateMathParser.parse(time, () -> now));
    }

    public static long dateMath(String time, final long now) {
        return dateMathParser.parse(time, () -> now);
    }

    public static LicenseSpec generateRandomLicenseSpec(int version) {
        boolean datesInMillis = randomBoolean();
        long now = System.currentTimeMillis();
        String uid = UUID.randomUUID().toString();
        String issuer = "issuer__"  + randomInt();
        String issuedTo = "issuedTo__" + randomInt();
        String type = version < License.VERSION_NO_FEATURE_TYPE ?
                randomFrom("subscription", "internal", "development") :
                randomFrom("basic", "silver", "dev", "gold", "platinum");
        final String subscriptionType;
        final String feature;
        if (version < License.VERSION_NO_FEATURE_TYPE) {
            subscriptionType = randomFrom("gold", "silver", "platinum");
            feature = "feature__" + randomInt();
        } else {
            subscriptionType = null;
            feature = null;
        }
        int maxNodes = randomIntBetween(5, 100);
        if (datesInMillis) {
            long issueDateInMillis = dateMath("now", now);
            long expiryDateInMillis = dateMath("now+10d/d", now);
            return new LicenseSpec(
                    version,
                    uid,
                    feature,
                    issueDateInMillis,
                    expiryDateInMillis,
                    type,
                    subscriptionType,
                    issuedTo,
                    issuer,
                    maxNodes);
        } else {
            String issueDate = dateMathString("now", now);
            String expiryDate = dateMathString("now+10d/d", now);
            return new LicenseSpec(
                    version,
                    uid,
                    feature,
                    issueDate,
                    expiryDate, type,
                    subscriptionType,
                    issuedTo,
                    issuer,
                    maxNodes);
        }
    }

    public static String generateLicenseSpecString(LicenseSpec licenseSpec) throws IOException {
        XContentBuilder licenses = jsonBuilder();
        licenses.startObject();
        licenses.startObject("license")
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
        licenses.field("version", licenseSpec.version);
        licenses.endObject();
        licenses.endObject();
        return licenses.string();
    }

    public static void assertLicenseSpec(LicenseSpec spec, License license) {
        MatcherAssert.assertThat(license.uid(), equalTo(spec.uid));
        MatcherAssert.assertThat(license.issuedTo(), equalTo(spec.issuedTo));
        MatcherAssert.assertThat(license.issuer(), equalTo(spec.issuer));
        MatcherAssert.assertThat(license.type(), equalTo(spec.type));
        MatcherAssert.assertThat(license.maxNodes(), equalTo(spec.maxNodes));
        if (spec.issueDate != null) {
            MatcherAssert.assertThat(
                    license.issueDate(),
                    equalTo(DateUtils.beginningOfTheDay(spec.issueDate)));
        } else {
            MatcherAssert.assertThat(license.issueDate(), equalTo(spec.issueDateInMillis));
        }
        if (spec.expiryDate != null) {
            MatcherAssert.assertThat(
                    license.expiryDate(),
                    equalTo(DateUtils.endOfTheDay(spec.expiryDate)));
        } else {
            MatcherAssert.assertThat(license.expiryDate(), equalTo(spec.expiryDateInMillis));
        }
    }

    public static License generateSignedLicense(
            TimeValue expiryDuration, Path pubKeyPath, Path priKeyPath) throws Exception {
        long issue = System.currentTimeMillis();
        int version = ESTestCase.randomIntBetween(License.VERSION_START, License.VERSION_CURRENT);
        String type = version < License.VERSION_NO_FEATURE_TYPE ?
                randomFrom("subscription", "internal", "development") :
                randomFrom("trial", "basic", "silver", "dev", "gold", "platinum");
        final License.Builder builder = License.builder()
                .uid(UUID.randomUUID().toString())
                .expiryDate(issue + expiryDuration.getMillis())
                .issueDate(issue)
                .version(version)
                .type(type)
                .issuedTo("customer")
                .issuer("elasticsearch")
                .maxNodes(5);
        if (version == License.VERSION_START) {
            builder.subscriptionType(randomFrom("dev", "gold", "platinum", "silver"));
            builder.feature(ESTestCase.randomAsciiOfLength(10));
        }
        LicenseSigner signer = new LicenseSigner(priKeyPath, pubKeyPath);
        return signer.sign(builder.build());
    }

    public static class LicenseSpec {
        public final int version;
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

        public LicenseSpec(
                int version,
                String uid,
                String feature,
                long issueDateInMillis,
                long expiryDateInMillis,
                String type,
                String subscriptionType,
                String issuedTo,
                String issuer,
                int maxNodes) {
            this.version = version;
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

        public LicenseSpec(
                int version,
                String uid,
                String feature,
                String issueDate,
                String expiryDate,
                String type,
                String subscriptionType,
                String issuedTo,
                String issuer,
                int maxNodes) {
            this.version = version;
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
