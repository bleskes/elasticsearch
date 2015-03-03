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

package org.elasticsearch.alerts.support;

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Test;

import java.util.Map;

import static org.elasticsearch.alerts.support.AlertUtils.flattenModel;
import static org.elasticsearch.alerts.support.AlertsDateUtils.formatDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class AlertUtilsTests {

    @Test
    public void testFlattenModel() throws Exception {
        DateTime now = new DateTime();
        Map<String, Object> map = ImmutableMap.<String, Object>builder()
                .put("a", ImmutableMap.builder().put("a1", new int[] { 0, 1, 2 }).build())
                .put("b", new String[] { "b0", "b1", "b2" })
                .put("c", ImmutableList.of(TimeValue.timeValueSeconds(0), TimeValue.timeValueSeconds(1)))
                .put("d", now)
                .build();

        Map<String, String> result = flattenModel(map);
        assertThat(result.size(), is(9));
        assertThat(result, hasEntry("a.a1.0", "0"));
        assertThat(result, hasEntry("a.a1.1", "1"));
        assertThat(result, hasEntry("a.a1.2", "2"));
        assertThat(result, hasEntry("b.0", "b0"));
        assertThat(result, hasEntry("b.1", "b1"));
        assertThat(result, hasEntry("b.2", "b2"));
        assertThat(result, hasEntry("c.0", "0"));
        assertThat(result, hasEntry("c.1", "1000"));
        assertThat(result, hasEntry("d", formatDate(now)));
    }
}
