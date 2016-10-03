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
package org.elasticsearch.xpack.monitoring.exporter.http;

import org.elasticsearch.test.ESTestCase;

import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests {@link Scheme}.
 */
public class SchemeTests extends ESTestCase {

    public void testToString() {
        for (final Scheme scheme : Scheme.values()) {
            assertThat(scheme.toString(), equalTo(scheme.name().toLowerCase(Locale.ROOT)));
        }
    }

    public void testFromString() {
        for (final Scheme scheme : Scheme.values()) {
            assertThat(Scheme.fromString(scheme.name()), sameInstance(scheme));
            assertThat(Scheme.fromString(scheme.name().toLowerCase(Locale.ROOT)), sameInstance(scheme));
        }
    }

    public void testFromStringMalformed() {
        assertIllegalScheme("htp");
        assertIllegalScheme("htttp");
        assertIllegalScheme("httpd");
        assertIllegalScheme("ftp");
        assertIllegalScheme("ws");
        assertIllegalScheme("wss");
        assertIllegalScheme("gopher");
    }

    private void assertIllegalScheme(final String scheme) {
        try {
            Scheme.fromString(scheme);
            fail("scheme should be unknown: [" + scheme + "]");
        } catch (final IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("[" + scheme + "]"));
        }
    }

}
