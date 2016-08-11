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

import org.elasticsearch.test.ESTestCase;

import java.util.Locale;

import static org.elasticsearch.license.License.OperationMode;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests {@link License.OperationMode} for correctness.
 * <p>
 * If you change the behavior of these tests, then it means that licensing changes across the products!
 */
public class LicenseOperationModeTests extends ESTestCase {
    public void testResolveTrial() {
        // assert 1.x BWC
        assertResolve(OperationMode.TRIAL, "nONE", "DEv", "deveLopment");
        // assert expected (2.x+) variant
        assertResolve(OperationMode.TRIAL, "tRiAl", "trial");
    }

    public void testResolveBasic() {
        // assert expected (2.x+) variant (note: no 1.x variant of BASIC)
        assertResolve(OperationMode.BASIC, "bAsIc", "basic");
    }

    public void testResolveStandard() {
        // assert expected (2.x+) variant (note: no 1.x variant of STANDARD)
        assertResolve(OperationMode.STANDARD, "StAnDARd", "standard");
    }

    public void testResolveGold() {
        // assert expected (2.x+) variant (note: no different 1.x variant of GOLD)
        assertResolve(OperationMode.GOLD, "SiLvEr", "gOlD", "silver", "gold");
    }

    public void testResolvePlatinum() {
        // assert 1.x BWC
        assertResolve(OperationMode.PLATINUM, "iNtErNaL");
        // assert expected (2.x+) variant
        assertResolve(OperationMode.PLATINUM, "PlAtINum", "platinum");
    }

    public void testResolveUnknown() {
        String[] types = { "unknown", "fake" };

        for (String type : types) {
            try {
                OperationMode.resolve(type);

                fail(String.format(Locale.ROOT, "[%s] should not be recognized as an operation mode", type));
            }
            catch (IllegalArgumentException e) {
                assertThat(e.getMessage(), equalTo("unknown type [" + type + "]"));
            }
        }
    }

    private static void assertResolve(OperationMode expected, String... types) {
        for (String type : types) {
            assertThat(OperationMode.resolve(type), equalTo(expected));
        }
    }
}
