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
package org.elasticsearch.license.core;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.ESTestCase;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class LicenseSerializationTests extends ESTestCase {
    public void testSimpleIssueExpiryDate() throws Exception {
        long now = System.currentTimeMillis();
        String issueDate = TestUtils.dateMathString("now", now);
        String expiryDate = TestUtils.dateMathString("now+10d/d", now);
        String licenseSpecs = TestUtils.generateLicenseSpecString(new TestUtils.LicenseSpec(issueDate, expiryDate));
        License generatedLicense = License.fromSource(licenseSpecs.getBytes(StandardCharsets.UTF_8));
        assertThat(generatedLicense.issueDate(), equalTo(DateUtils.beginningOfTheDay(issueDate)));
        assertThat(generatedLicense.expiryDate(), equalTo(DateUtils.endOfTheDay(expiryDate)));
    }

    public void testLicensesFields() throws Exception {
        TestUtils.LicenseSpec randomLicenseSpec = TestUtils.generateRandomLicenseSpec(License.VERSION_START);
        String licenseSpecsSource = TestUtils.generateLicenseSpecString(randomLicenseSpec);
        final License fromSource = License.fromSource(licenseSpecsSource.getBytes(StandardCharsets.UTF_8));
        TestUtils.assertLicenseSpec(randomLicenseSpec, fromSource);
    }

    public void testLicenseRestView() throws Exception {
        long now = System.currentTimeMillis();
        String expiredLicenseExpiryDate = TestUtils.dateMathString("now-1d/d", now);
        String validLicenseIssueDate = TestUtils.dateMathString("now-10d/d", now);
        String invalidLicenseIssueDate = TestUtils.dateMathString("now+1d/d", now);
        String validLicenseExpiryDate = TestUtils.dateMathString("now+2d/d", now);

        License license = TestUtils.generateLicenses(new TestUtils.LicenseSpec(validLicenseIssueDate, expiredLicenseExpiryDate));
        XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
        license.toXContent(builder, new ToXContent.MapParams(Collections.singletonMap(License.REST_VIEW_MODE, "true")));
        builder.flush();
        Map<String, Object> map = XContentHelper.convertToMap(builder.bytes(), false).v2();

        // should have an extra status field, human readable issue_data and expiry_date
        assertThat(map.get("status"), notNullValue());
        assertThat(map.get("issue_date"), notNullValue());
        assertThat(map.get("expiry_date"), notNullValue());
        assertThat(map.get("status"), equalTo("expired"));
        builder = XContentFactory.contentBuilder(XContentType.JSON);
        license.toXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.flush();
        map = XContentHelper.convertToMap(builder.bytes(), false).v2();
        assertThat(map.get("status"), nullValue());

        license = TestUtils.generateLicenses(new TestUtils.LicenseSpec(validLicenseIssueDate, validLicenseExpiryDate));
        builder = XContentFactory.contentBuilder(XContentType.JSON);
        license.toXContent(builder, new ToXContent.MapParams(Collections.singletonMap(License.REST_VIEW_MODE, "true")));
        builder.flush();
        map = XContentHelper.convertToMap(builder.bytes(), false).v2();

        // should have an extra status field, human readable issue_data and expiry_date
        assertThat(map.get("status"), notNullValue());
        assertThat(map.get("issue_date"), notNullValue());
        assertThat(map.get("expiry_date"), notNullValue());
        assertThat(map.get("status"), equalTo("active"));
        builder = XContentFactory.contentBuilder(XContentType.JSON);
        license.toXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.flush();
        map = XContentHelper.convertToMap(builder.bytes(), false).v2();
        assertThat(map.get("status"), nullValue());

        license = TestUtils.generateLicenses(new TestUtils.LicenseSpec(invalidLicenseIssueDate, validLicenseExpiryDate));
        builder = XContentFactory.contentBuilder(XContentType.JSON);
        license.toXContent(builder, new ToXContent.MapParams(Collections.singletonMap(License.REST_VIEW_MODE, "true")));
        builder.flush();
        map = XContentHelper.convertToMap(builder.bytes(), false).v2();

        // should have an extra status field, human readable issue_data and expiry_date
        assertThat(map.get("status"), notNullValue());
        assertThat(map.get("issue_date"), notNullValue());
        assertThat(map.get("expiry_date"), notNullValue());
        assertThat(map.get("status"), equalTo("invalid"));
        builder = XContentFactory.contentBuilder(XContentType.JSON);
        license.toXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.flush();
        map = XContentHelper.convertToMap(builder.bytes(), false).v2();
        assertThat(map.get("status"), nullValue());
    }
}
