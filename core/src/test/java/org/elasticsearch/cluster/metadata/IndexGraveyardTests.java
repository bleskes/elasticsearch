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

package org.elasticsearch.cluster.metadata;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.Index;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Tests for the {@link IndexGraveyard} class
 */
public class IndexGraveyardTests extends ESTestCase {

    public void testIsEquals() {
        final IndexGraveyard graveyard1 = new IndexGraveyard();
        assertTrue("empty graveyards are equal", graveyard1.isEquals(new IndexGraveyard(graveyard1)));
        final IndexGraveyard graveyard2 = createRandom();
        assertTrue("same graveyards are equal", graveyard2.isEquals(new IndexGraveyard(graveyard2)));
    }

    public void testSerialization() throws IOException {
        final int numIterations = 5;
        for (int i = 0; i < numIterations; i++) {
            final IndexGraveyard graveyard = createRandom();
            final BytesStreamOutput out = new BytesStreamOutput();
            graveyard.writeTo(out);
            final ByteBufferStreamInput in = new ByteBufferStreamInput(ByteBuffer.wrap(out.bytes().toBytes()));
            assertTrue("writing to and reading from stream must produce the same index graveyard",
                graveyard.isEquals(new IndexGraveyard(in)));
        }
    }

    public void testXContent() throws IOException {
        final int numIterations = 5;
        for (int i = 0; i < numIterations; i++) {
            final IndexGraveyard graveyard = createRandom();
            final XContentBuilder builder = JsonXContent.contentBuilder();
            graveyard.toXContent(builder, ToXContent.EMPTY_PARAMS);
            XContentParser parser = XContentType.JSON.xContent().createParser(builder.bytes());
            parser.nextToken(); // the beginning of the parser
            parser.nextToken(); // the START_ARRAY token
            assertTrue("writing to and reading from x-content must produce the same index graveyard",
                graveyard.isEquals(IndexGraveyard.fromXContent(parser)));
        }
    }

    public void testAddTombstones() {
        final int numIterations = 5;
        for (int i = 0; i < numIterations; i++) {
            final IndexGraveyard graveyard1 = createRandom();
            final IndexGraveyard graveyard2 = new IndexGraveyard(graveyard1);
            final int numAdds = randomIntBetween(0, 4);
            for (int j = 0; j < numAdds; j++) {
                graveyard2.addTombstone(new Index("nidx-" + j, Strings.randomBase64UUID()));
            }
            if (numAdds == 0) {
                assertTrue("no tombstones added, so should be equal", graveyard1.isEquals(graveyard2));
                assertThat(graveyard1.getTombstones(), equalTo(graveyard2.getTombstones()));
            } else {
                assertFalse("tombstones added, so should not be equal", graveyard1.isEquals(graveyard2));
                assertThat(graveyard1.getTombstones().size(), lessThan(graveyard2.getTombstones().size()));
                assertThat(Collections.indexOfSubList(graveyard2.getTombstones(), graveyard1.getTombstones()), equalTo(0));
            }
        }
    }

    public void testPurge() {
        final int numIterations = 5;
        for (int i = 0; i < numIterations; i++) {
            final IndexGraveyard graveyard = new IndexGraveyard();
            final int numAdd = randomIntBetween(1, 5);
            // set deletion date to greater than the expiration window
            final long deletionDate = getExpiredDate();
            for (int j = 0; j < numAdd; j++) {
                graveyard.addTombstone(new Index("idx-" + i, Strings.randomBase64UUID()), deletionDate, 0L);
            }
            final int numPurged = graveyard.purge();
            assertThat(numPurged, equalTo(numAdd));
            // after the purge, there should be no more entries
            assertThat(graveyard.getTombstones().size(), equalTo(0));
        }
    }

    public void testDiffs() {
        final Comparator<Index> sortFunction = (Index i1, Index i2) -> i1.getName().compareTo(i2.getName());
        final int numIterations = 5;
        for (int i = 0; i < numIterations; i++) {
            final IndexGraveyard graveyard1 = new IndexGraveyard();
            final int numToPurge = randomIntBetween(0, 4);
            final List<Index> removals = new ArrayList<>();
            for (int j = 0; j < numToPurge; j++) {
                final Index indexToRemove = new Index("ridx-" + j, Strings.randomBase64UUID());
                graveyard1.addTombstone(indexToRemove, getExpiredDate(), 0L);
                removals.add(indexToRemove);
            }
            final int numTombstones = randomIntBetween(0, 4);
            for (int j = 0; j < numTombstones; j++) {
                graveyard1.addTombstone(new Index("idx-" + j, Strings.randomBase64UUID()));
            }
            final IndexGraveyard graveyard2 = new IndexGraveyard(graveyard1);
            final int numPurged = graveyard2.purge();
            assertThat(numPurged, equalTo(numToPurge));
            final int numToAdd = randomIntBetween(0, 4);
            final List<Index> additions = new ArrayList<>();
            for (int j = 0; j < numToAdd; j++) {
                final Index indexToAdd = new Index("nidx-" + j, Strings.randomBase64UUID());
                graveyard2.addTombstone(indexToAdd);
                additions.add(indexToAdd);
            }
            final IndexGraveyard.IndexGraveyardDiff diff = new IndexGraveyard.IndexGraveyardDiff(graveyard1, graveyard2);
            final List<Index> actualAdded = diff.getAdded().stream().map(t -> t.getIndex()).collect(Collectors.toList());
            final List<Index> actualRemoved = diff.getRemoved().stream().map(t -> t.getIndex()).collect(Collectors.toList());
            Collections.sort(additions, sortFunction);
            Collections.sort(removals, sortFunction);
            Collections.sort(actualAdded, sortFunction);
            Collections.sort(actualRemoved, sortFunction);
            assertThat(actualAdded, equalTo(additions));
            assertThat(actualRemoved, equalTo(removals));
        }
    }

    private static IndexGraveyard createRandom() {
        final IndexGraveyard graveyard = new IndexGraveyard();
        final int numTombstones = randomIntBetween(0, 4);
        for (int i = 0; i < numTombstones; i++) {
            graveyard.addTombstone(new Index("idx-" + i, Strings.randomBase64UUID()));
        }
        return graveyard;
    }

    private static long getExpiredDate() {
        return System.currentTimeMillis() - (IndexGraveyard.EXPIRATION_WINDOW + 5000);
    }

}
