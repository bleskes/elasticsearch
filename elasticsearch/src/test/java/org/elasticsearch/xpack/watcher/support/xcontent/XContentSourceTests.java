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

package org.elasticsearch.xpack.watcher.support.xcontent;


import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.test.ESTestCase;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.smileBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.yamlBuilder;

/**
 *
 */
public class XContentSourceTests extends ESTestCase {
    public void testToXContent() throws Exception {
        XContentBuilder builder = randomBoolean() ? jsonBuilder() : randomBoolean() ? yamlBuilder() : smileBuilder();
        BytesReference bytes = randomBoolean() ?
                builder.startObject().field("key", "value").endObject().bytes() :
                builder.startObject()
                        .field("key_str", "value")
                        .startArray("array_int").value(randomInt(10)).endArray()
                        .nullField("key_null")
                        .endObject()
                        .bytes();
        XContentSource source = new XContentSource(bytes, builder.contentType());
        XContentBuilder builder2 = XContentFactory.contentBuilder(builder.contentType());
        BytesReference bytes2 = source.toXContent(builder2, ToXContent.EMPTY_PARAMS).bytes();
        assertEquals(bytes.toBytesRef(), bytes2.toBytesRef());
    }
}
