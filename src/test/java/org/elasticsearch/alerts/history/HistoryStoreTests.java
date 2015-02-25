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

package org.elasticsearch.alerts.history;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 */
public class HistoryStoreTests extends ElasticsearchTestCase {

    @Test
    public void testIndexNameGeneration() {
        assertThat(HistoryStore.getAlertHistoryIndexNameForTime(new DateTime(0, DateTimeZone.UTC)), equalTo(".alert_history_1970-01-01"));
        assertThat(HistoryStore.getAlertHistoryIndexNameForTime(new DateTime(100000000000L, DateTimeZone.UTC)), equalTo(".alert_history_1973-03-03"));
        assertThat(HistoryStore.getAlertHistoryIndexNameForTime(new DateTime(1416582852000L, DateTimeZone.UTC)), equalTo(".alert_history_2014-11-21"));
        assertThat(HistoryStore.getAlertHistoryIndexNameForTime(new DateTime(2833165811000L, DateTimeZone.UTC)), equalTo(".alert_history_2059-10-12"));
    }

}
