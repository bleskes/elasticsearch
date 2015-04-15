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

package org.elasticsearch.watcher.support.clock;

import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;
import static org.hamcrest.Matchers.equalTo;

/**
 */
public class ClockTests extends ElasticsearchTestCase {

    @Test
    public void test_now_UTC() {
        Clock clockMock = new ClockMock();
        assertThat(clockMock.now(UTC).getZone(), equalTo(UTC));
        assertThat(SystemClock.INSTANCE.now(UTC).getZone(), equalTo(UTC));
    }

}
