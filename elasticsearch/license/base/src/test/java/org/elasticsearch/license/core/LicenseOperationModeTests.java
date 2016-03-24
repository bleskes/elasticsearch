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

import org.elasticsearch.test.ESTestCase;

import static org.elasticsearch.license.core.License.OperationMode;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests {@link License.OperationMode} for correctness.
 * <p>
 * If you change the behavior of these tests, then it means that licensing changes across the products!
 */
public class LicenseOperationModeTests extends ESTestCase {
    public void testIsPaid() {
        assertThat(OperationMode.BASIC.isPaid(), equalTo(false));

        // EVERY other operation mode is expected to be equivalent to paid; loop will catch any new mode
        for (OperationMode mode : OperationMode.values()) {
            if (mode != OperationMode.BASIC) {
                assertThat(mode.isPaid(), equalTo(true));
            }
        }
    }

    public void testAllFeaturesEnabled() {
        assertThat(OperationMode.TRIAL.allFeaturesEnabled(), equalTo(true));
        assertThat(OperationMode.PLATINUM.allFeaturesEnabled(), equalTo(true));

        // EVERY other operation mode is expected to NOT enable everything; loop will catch any new mode
        for (OperationMode mode : OperationMode.values()) {
            if (mode != OperationMode.TRIAL && mode != OperationMode.PLATINUM) {
                assertThat(mode.allFeaturesEnabled(), equalTo(false));
            }
        }
    }

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

    public void testResolveGold() {
        // assert expected (2.x+) variant (note: no different 1.x variant of GOLD)
        assertResolve(OperationMode.BASIC, "SiLvEr", "gOlD", "silver", "gold");
    }

    public void testResolvePlatinum() {
        // assert 1.x BWC
        assertResolve(OperationMode.TRIAL, "iNtErNaL");
        // assert expected (2.x+) variant
        assertResolve(OperationMode.TRIAL, "PlAtINum", "platinum");
    }

    public void testResolveUnknown() {
        // standard will hopefully trip the upcoming standard license to add the test here for FWC
        String[] types = { "standard", "unknown", "fake" };

        for (String type : types) {
            try {
                OperationMode.resolve(type);

                fail(String.format("[%s] should not be recognized as an operation mode", type));
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
