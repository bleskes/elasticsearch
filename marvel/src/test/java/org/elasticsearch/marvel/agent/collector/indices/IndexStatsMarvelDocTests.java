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

package org.elasticsearch.marvel.agent.collector.indices;

import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

public class IndexStatsMarvelDocTests extends ElasticsearchTestCase {

    @Test
    public void testCreateMarvelDoc() {
        String cluster = randomUnicodeOfLength(10);
        String type = randomUnicodeOfLength(10);
        long timestamp = randomLong();
        String index = randomUnicodeOfLength(10);
        long docsCount = randomLong();
        long storeSize = randomLong();
        long storeThrottle = randomLong();
        long indexingThrottle = randomLong();

        IndexStatsMarvelDoc marvelDoc = IndexStatsMarvelDoc.createMarvelDoc(cluster, type, timestamp,
                index, docsCount, storeSize, storeThrottle, indexingThrottle);

        assertNotNull(marvelDoc);
        assertThat(marvelDoc.clusterName(), equalTo(cluster));
        assertThat(marvelDoc.type(), equalTo(type));
        assertThat(marvelDoc.timestamp(), equalTo(timestamp));
        assertThat(marvelDoc.getIndex(), equalTo(index));
        assertNotNull(marvelDoc.getDocs());
        assertThat(marvelDoc.getDocs().getCount(), equalTo(docsCount));
        assertNotNull(marvelDoc.getStore());
        assertThat(marvelDoc.getStore().getSizeInBytes(), equalTo(storeSize));
        assertThat(marvelDoc.getStore().getThrottleTimeInMillis(), equalTo(storeThrottle));
        assertNotNull(marvelDoc.getIndexing());
        assertThat(marvelDoc.getIndexing().getThrottleTimeInMillis(), equalTo(indexingThrottle));
    }
}
