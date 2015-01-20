/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.index.sequence;

import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.util.concurrent.UncategorizedExecutionException;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

@TestLogging("index.sequence:TRACE")
public class BasicIntegrationTests extends ElasticsearchIntegrationTest {

    @Test
    public void testSimpleSeqNoAssignment() throws Exception {
        prepareCreate("test")
                .setSettings(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1,
                        IndexMetaData.SETTING_NUMBER_OF_REPLICAS, randomIntBetween(0, cluster().numDataNodes() - 1)
                ).get();
        ensureGreen();
        IndexResponse response = client().prepareIndex("test", "type1", "1").setSource("f", "").get();
        SequenceNo expected = new SequenceNo(0, 0);
        assertThat(response.getSequenceNo(), equalTo(expected));
        assertAllShardsSeqNo(expected, expected, null);
        // TODO: get the doc and check the seqNo

        response = client().prepareIndex("test", "type1", "2").setSource("f", "").get();
        expected = new SequenceNo(0, 1);
        assertThat(response.getSequenceNo(), equalTo(expected));
        assertAllShardsSeqNo(expected, expected, null);

        final SequenceNo finalExpected = expected;
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertAllShardsSeqNo(finalExpected, finalExpected, finalExpected);
            }
        });
    }

    @Test
    public void testSimpleSeqNoWithError() throws Exception {
        prepareCreate("test")
                .setSettings(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1,
                        IndexMetaData.SETTING_NUMBER_OF_REPLICAS, randomIntBetween(0, cluster().numDataNodes() - 1)
                )
                .addMapping("type1", "f", "type=string,index=not_analyzed").get();
        ensureGreen();
        IndexResponse response = client().prepareIndex("test", "type1", "1").setSource("f", "").get();
        SequenceNo expected = new SequenceNo(0, 0);
        assertThat(response.getSequenceNo(), equalTo(expected));
        assertAllShardsSeqNo(expected, expected, null);
        // TODO: get the doc and check the seqNo

        ElasticsearchAssertions.assertThrows(client().prepareIndex("test", "type1", "2").setSource("f", randomRealisticUnicodeOfLength(13000)),
                UncategorizedExecutionException.class);

        expected = new SequenceNo(0, 1);
        assertAllShardsSeqNo(expected, expected, null);
        final SequenceNo finalExpected = expected;
        assertBusy(new Runnable() {
            @Override
            public void run() {
                assertAllShardsSeqNo(finalExpected, finalExpected, finalExpected);
            }
        });
    }

    private void assertAllShardsSeqNo(SequenceNo maxSeqNo, SequenceNo maxConsecutiveSeqNo, SequenceNo consensusNo) {
        IndicesStatsResponse response = client().admin().indices().prepareStats("test").clear().setSequence(true).get();
        for (ShardStats stats : response.getShards()) {
            if (maxSeqNo != null) {
                assertThat("maxSeqNo mismatch", stats.getStats().getSequenceStats().maxSequenceNo(), equalTo(maxSeqNo));
            }
            if (maxConsecutiveSeqNo != null) {
                assertThat("maxConsecutiveSeqNo mismatch", stats.getStats().getSequenceStats().maxConsecutiveSeqNo(), equalTo(maxConsecutiveSeqNo));
            }
            if (consensusNo != null) {
                assertThat("consensusNo mismatch", stats.getStats().getSequenceStats().consensusNo(), equalTo(consensusNo));
            }
        }
    }
}
