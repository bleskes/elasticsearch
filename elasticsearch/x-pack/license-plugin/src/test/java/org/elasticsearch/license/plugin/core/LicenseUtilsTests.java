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

package org.elasticsearch.license.plugin.core;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.test.ESTestCase;

import java.util.Arrays;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class LicenseUtilsTests extends ESTestCase {

    public void testNewExpirationException() {
        for (String feature : Arrays.asList("feature", randomAsciiOfLength(5), null, "")) {
            ElasticsearchSecurityException exception = LicenseUtils.newComplianceException(feature);
            assertNotNull(exception);
            assertThat(exception.getHeaderKeys(), contains(LicenseUtils.EXPIRED_FEATURE_HEADER));
            assertThat(exception.getHeader(LicenseUtils.EXPIRED_FEATURE_HEADER), hasSize(1));
            assertThat(exception.getHeader(LicenseUtils.EXPIRED_FEATURE_HEADER).iterator().next(), equalTo(feature));
        }
    }

    public void testIsLicenseExpiredException() {
        ElasticsearchSecurityException exception = LicenseUtils.newComplianceException("feature");
        assertTrue(LicenseUtils.isLicenseExpiredException(exception));

        exception = new ElasticsearchSecurityException("msg");
        assertFalse(LicenseUtils.isLicenseExpiredException(exception));
    }
}
