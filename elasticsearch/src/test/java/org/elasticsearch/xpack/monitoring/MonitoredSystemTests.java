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

package org.elasticsearch.xpack.monitoring;

import org.elasticsearch.test.ESTestCase;

import java.util.Locale;

import static org.hamcrest.Matchers.containsString;

/**
 * Tests {@link MonitoredSystem}.
 */
public class MonitoredSystemTests extends ESTestCase {

    public void testGetSystem() {
        // everything is just lowercased...
        for (final MonitoredSystem system : MonitoredSystem.values()) {
            assertEquals(system.name().toLowerCase(Locale.ROOT), system.getSystem());
        }
    }

    public void testFromSystem() {
        for (final MonitoredSystem system : MonitoredSystem.values()) {
            final String lowercased = system.name().toLowerCase(Locale.ROOT);

            assertSame(system, MonitoredSystem.fromSystem(system.name()));
            assertSame(system, MonitoredSystem.fromSystem(lowercased));
        }
    }

    public void testFromUnknownSystem() {
        final String unknownSystem = randomAsciiOfLengthBetween(3, 4);

        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> {
            MonitoredSystem.fromSystem(unknownSystem);
        });

        assertThat(e.getMessage(), containsString(unknownSystem));
    }

}
