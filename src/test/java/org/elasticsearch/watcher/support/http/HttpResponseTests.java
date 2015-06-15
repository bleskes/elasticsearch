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

package org.elasticsearch.watcher.support.http;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.watcher.test.WatcherTestUtils.xContentParser;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class HttpResponseTests extends ElasticsearchTestCase {

    @Test @Repeat(iterations = 20)
    public void testParse_SelfGenerated() throws Exception {
        int status = randomIntBetween(200, 600);
        ImmutableMap<String, String[]> headers = ImmutableMap.of();
        if (randomBoolean()) {
            headers = ImmutableMap.of("key", new String[] { "value" });
        }
        String body = randomBoolean() ? "body" : null;
        final HttpResponse response;
        if (randomBoolean() && headers.isEmpty() && body == null) {
            response = new HttpResponse(status);
        } else if (body != null ){
            switch (randomIntBetween(0, 2)) {
                case 0:
                    response = new HttpResponse(status, body, headers);
                    break;
                case 1:
                    response = new HttpResponse(status, body.getBytes(StandardCharsets.UTF_8), headers);
                    break;
                default: // 2
                    response = new HttpResponse(status, new BytesArray(body), headers);
                    break;
            }
        } else { // body is null
            switch (randomIntBetween(0, 3)) {
                case 0:
                    response = new HttpResponse(status, (String) null, headers);
                    break;
                case 1:
                    response = new HttpResponse(status, (byte[]) null, headers);
                    break;
                case 2:
                    response = new HttpResponse(status, (BytesReference) null, headers);
                    break;
                default: //3
                    response = new HttpResponse(status, headers);
                    break;
            }
        }

        XContentBuilder builder = jsonBuilder().value(response);
        XContentParser parser = xContentParser(builder);
        parser.nextToken();
        HttpResponse parsedResponse = HttpResponse.parse(parser);
        assertThat(parsedResponse, notNullValue());
        assertThat(parsedResponse.status(), is(status));
        if (body == null) {
            assertThat(parsedResponse.body(), nullValue());
        } else {
            assertThat(parsedResponse.body().toUtf8(), is(body));
        }
        assertThat(parsedResponse.headers().size(), is(headers.size()));
        for (Map.Entry<String, String[]> header : parsedResponse.headers().entrySet()) {
            assertThat(header.getValue(), arrayContaining(headers.get(header.getKey())));
        }
    }
}
