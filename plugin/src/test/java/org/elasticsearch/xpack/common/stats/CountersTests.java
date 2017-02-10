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

package org.elasticsearch.xpack.common.stats;

import org.elasticsearch.test.ESTestCase;

import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;

public class CountersTests extends ESTestCase {

    public void testCounters() {
        Counters counters = new Counters();
        counters.inc("f", 200);
        counters.inc("foo.bar");
        counters.inc("foo.baz");
        counters.inc("foo.baz");
        Map<String, Object> map = counters.toMap();
        assertThat(map, hasEntry("f", 200L));
        assertThat(map, hasKey("foo"));
        assertThat(map.get("foo"), instanceOf(Map.class));
        Map<String, Object> fooMap = (Map<String, Object>) map.get("foo");
        assertThat(fooMap, hasEntry("bar", 1L));
        assertThat(fooMap, hasEntry("baz", 2L));
    }
}
