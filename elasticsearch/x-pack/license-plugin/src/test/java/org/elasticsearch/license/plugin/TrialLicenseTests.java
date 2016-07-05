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
package org.elasticsearch.license.plugin;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.TrialLicense;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import static org.elasticsearch.license.core.CryptUtils.encrypt;
import static org.hamcrest.Matchers.equalTo;


public class TrialLicenseTests extends ESTestCase {
    public void testBasic() throws Exception {
        long issueDate = System.currentTimeMillis();
        License.Builder specBuilder = License.builder()
                .uid(UUID.randomUUID().toString())
                .issuedTo("customer")
                .maxNodes(5)
                .issueDate(issueDate)
                .expiryDate(issueDate + TimeValue.timeValueHours(2).getMillis());
        License trialLicense = TrialLicense.create(specBuilder);
        assertThat(TrialLicense.verify(trialLicense), equalTo(true));
    }

    public void testTampered() throws Exception {
        long issueDate = System.currentTimeMillis();
        License.Builder specBuilder = License.builder()
                .uid(UUID.randomUUID().toString())
                .issuedTo("customer")
                .maxNodes(5)
                .issueDate(issueDate)
                .expiryDate(issueDate + TimeValue.timeValueHours(2).getMillis());
        License trialLicense = TrialLicense.create(specBuilder);
        final String originalSignature = trialLicense.signature();
        License tamperedLicense = License.builder().fromLicenseSpec(trialLicense, originalSignature)
                .expiryDate(System.currentTimeMillis() + TimeValue.timeValueHours(5).getMillis())
                .build();
        assertThat(TrialLicense.verify(trialLicense), equalTo(true));
        assertThat(TrialLicense.verify(tamperedLicense), equalTo(false));
    }

    public void testFrom1x() throws Exception {
        long issueDate = System.currentTimeMillis();
        License.Builder specBuilder = License.builder()
                .uid(UUID.randomUUID().toString())
                .issuedTo("customer")
                .type("subscription")
                .subscriptionType("trial")
                .issuer("elasticsearch")
                .feature("")
                .version(License.VERSION_START)
                .maxNodes(5)
                .issueDate(issueDate)
                .expiryDate(issueDate + TimeValue.timeValueHours(2).getMillis());
        License pre20TrialLicense = specBuilder.build();
        License license = TrialLicense.create(License.builder().fromPre20LicenseSpec(pre20TrialLicense));
        assertThat(TrialLicense.verify(license), equalTo(true));
    }

    public void testTrialLicenseVerifyWithOlderVersion() throws Exception {
        long issueDate = System.currentTimeMillis();
        License.Builder specBuilder = License.builder()
                .issuedTo("customer")
                .maxNodes(5)
                .issueDate(issueDate)
                .expiryDate(issueDate + TimeValue.timeValueHours(2).getMillis())
                .feature("")
                .subscriptionType("trial")
                .version(1);
        License trialLicenseV1 = createTrialLicense(specBuilder);
        assertThat(TrialLicense.verify(trialLicenseV1), equalTo(true));
    }

    static License createTrialLicense(License.Builder specBuilder) {
        License spec = specBuilder
                .type("trial")
                .issuer("elasticsearch")
                .uid(UUID.randomUUID().toString())
                .build();
        final String signature;
        try {
            XContentBuilder contentBuilder = XContentFactory.contentBuilder(XContentType.JSON);
            spec.toXContent(contentBuilder, new ToXContent.MapParams(Collections.singletonMap(License.LICENSE_SPEC_VIEW_MODE, "true")));
            byte[] encrypt = encrypt(BytesReference.toBytes(contentBuilder.bytes()));
            byte[] bytes = new byte[4 + 4 + encrypt.length];
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            byteBuffer.putInt(-spec.version())
                    .putInt(encrypt.length)
                    .put(encrypt);
            signature = Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return License.builder().fromLicenseSpec(spec, signature).build();
    }
}
