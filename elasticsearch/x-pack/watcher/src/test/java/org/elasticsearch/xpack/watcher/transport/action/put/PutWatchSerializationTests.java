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

package org.elasticsearch.xpack.watcher.transport.action.put;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchRequest;

import static org.hamcrest.Matchers.is;

public class PutWatchSerializationTests extends ESTestCase {

    // https://github.com/elastic/x-plugins/issues/2490
    public void testPutWatchSerialization() throws Exception {
        PutWatchRequest request = new PutWatchRequest();
        request.setId(randomAsciiOfLength(10));
        request.setActive(randomBoolean());
        request.setSource(new BytesArray(randomAsciiOfLength(20)));

        BytesStreamOutput streamOutput = new BytesStreamOutput();
        request.writeTo(streamOutput);

        PutWatchRequest readRequest = new PutWatchRequest();
        readRequest.readFrom(streamOutput.bytes().streamInput());
        assertThat(readRequest.isActive(), is(request.isActive()));
        assertThat(readRequest.getId(), is(request.getId()));
        assertThat(readRequest.getSource(), is(request.getSource()));
    }

}
