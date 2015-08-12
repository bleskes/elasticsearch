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

package org.elasticsearch.shield;

import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class ShieldVersionTests extends ESTestCase {

    @Test
    public void testStrings() throws Exception {
        for (int i = 0; i < 100; i++) {
            boolean beta = randomBoolean();
            int buildNumber = beta ? randomIntBetween(0, 49) : randomIntBetween(0, 48);
            int major = randomIntBetween(0, 20);
            int minor = randomIntBetween(0, 20);
            int revision = randomIntBetween(0, 20);

            String build = buildNumber == 0 ? "" :
                    beta ? "-beta" + buildNumber : "-rc" + buildNumber;


            String versionName = new StringBuilder()
                    .append(major)
                    .append(".").append(minor)
                    .append(".").append(revision)
                    .append(build).toString();
            ShieldVersion version = ShieldVersion.fromString(versionName);

            logger.info("version: {}", versionName);

            assertThat(version.major, is((byte) major));
            assertThat(version.minor, is((byte) minor));
            assertThat(version.revision, is((byte) revision));
            if (buildNumber == 0) {
                assertThat(version.build, is((byte) 99));
            } else if (beta) {
                assertThat(version.build, is((byte) buildNumber));
            } else {
                assertThat(version.build, is((byte) (buildNumber + 50)));
            }

            assertThat(version.number(), equalTo(versionName));
        }
    }
}
