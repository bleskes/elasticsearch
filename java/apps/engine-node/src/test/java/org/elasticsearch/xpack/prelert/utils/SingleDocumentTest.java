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

package org.elasticsearch.xpack.prelert.utils;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.support.AbstractWireSerializingTestCase;

public class SingleDocumentTest extends AbstractWireSerializingTestCase<SingleDocument<Influencer>> {

    public void testConstructorWithNullDocument() {
        assertFalse(new SingleDocument<>("string", (BytesReference) null).isExists());
    }

    public void testEmptyDocument() {
        SingleDocument<?> doc = SingleDocument.empty("string");

        assertFalse(doc.isExists());
        assertEquals("string", doc.getType());
        assertNull(doc.getDocumentBytes());
        assertEquals(RestStatus.NOT_FOUND, doc.status());
    }

    public void testExistingDocument() {
        SingleDocument<?> doc = new SingleDocument<>("string", new BytesArray("the doc"));

        assertTrue(doc.isExists());
        assertEquals("string", doc.getType());
        assertEquals("the doc", doc.getDocumentBytes().utf8ToString());
        assertEquals(RestStatus.OK, doc.status());
    }

    @Override
    protected SingleDocument<Influencer> createTestInstance() {
        SingleDocument<Influencer> document;
        if (randomBoolean()) {
            document = new SingleDocument<Influencer>(randomAsciiOfLengthBetween(1, 20),
                    new BytesArray(randomAsciiOfLengthBetween(1, 2000)));
        } else {
            document = new SingleDocument<Influencer>(randomAsciiOfLengthBetween(1, 20),
                    new Influencer(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20)));
        }
        return document;
    }

    @Override
    protected Reader<SingleDocument<Influencer>> instanceReader() {
        return (in) -> new SingleDocument<>(in, Influencer::new);
    }
}
