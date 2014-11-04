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

import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;
import java.util.*;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TestUtils {

    public static void isSame(Collection<ESLicense> firstLicenses, Collection<ESLicense> secondLicenses) {
        isSame(new HashSet<>(firstLicenses), new HashSet<>(secondLicenses));
    }

    public static void isSame(Set<ESLicense> firstLicenses, Set<ESLicense> secondLicenses) {

        // we do the verifyAndBuild to make sure we weed out any expired licenses
        final Map<String, ESLicense> licenses1 = ESLicenses.reduceAndMap(firstLicenses);
        final Map<String, ESLicense> licenses2 = ESLicenses.reduceAndMap(secondLicenses);

        // check if the effective licenses have the same feature set
        assertThat(licenses1.size(), equalTo(licenses2.size()));

        // for every feature license, check if all the attributes are the same
        for (String featureType : licenses1.keySet()) {
            ESLicense license1 = licenses1.get(featureType);
            ESLicense license2 = licenses2.get(featureType);
            isSame(license1, license2);
        }
    }

    public static void isSame(ESLicense license1, ESLicense license2) {
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
}
