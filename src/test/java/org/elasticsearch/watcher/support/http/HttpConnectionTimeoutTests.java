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

import org.apache.lucene.util.LuceneTestCase.Slow;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.support.http.auth.HttpAuthRegistry;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;

/**
 */
public class HttpConnectionTimeoutTests extends ElasticsearchTestCase {

    // setting an unroutable IP to simulate a connection timeout
    private static final String UNROUTABLE_IP = "192.168.255.255";

    @Test @Slow
    public void testDefaultTimeout() throws Exception {
        HttpClient httpClient = new HttpClient(ImmutableSettings.EMPTY, mock(HttpAuthRegistry.class)).start();

        HttpRequest request = HttpRequest.builder(UNROUTABLE_IP, 12345)
                .method(HttpMethod.POST)
                .path("/" + randomAsciiOfLength(5))
                .build();

        long start = System.nanoTime();
        try {
            httpClient.execute(request);
            fail("expected timeout exception");
        } catch (ElasticsearchTimeoutException ete) {
            TimeValue timeout = TimeValue.timeValueNanos(System.nanoTime() - start);
            logger.info("http connection timed out after {}", timeout.format());
            // it's supposed to be 10, but we'll give it an error margin of 2 seconds
            assertThat(timeout.seconds(), greaterThan(8L));
            assertThat(timeout.seconds(), lessThan(12L));
            // expected
        }
    }

    @Test @Slow
    public void testDefaultTimeout_Custom() throws Exception {
        HttpClient httpClient = new HttpClient(ImmutableSettings.builder()
                .put("watcher.http.default_connection_timeout", "5s")
                .build()
                , mock(HttpAuthRegistry.class)).start();

        HttpRequest request = HttpRequest.builder(UNROUTABLE_IP, 12345)
                .method(HttpMethod.POST)
                .path("/" + randomAsciiOfLength(5))
                .build();

        long start = System.nanoTime();
        try {
            httpClient.execute(request);
            fail("expected timeout exception");
        } catch (ElasticsearchTimeoutException ete) {
            TimeValue timeout = TimeValue.timeValueNanos(System.nanoTime() - start);
            logger.info("http connection timed out after {}", timeout.format());
            // it's supposed to be 7, but we'll give it an error margin of 2 seconds
            assertThat(timeout.seconds(), greaterThan(3L));
            assertThat(timeout.seconds(), lessThan(7L));
            // expected
        }
    }

    @Test @Slow
    public void testTimeout_CustomPerRequest() throws Exception {
        HttpClient httpClient = new HttpClient(ImmutableSettings.builder()
                .put("watcher.http.default_connection_timeout", "10s")
                .build()
                , mock(HttpAuthRegistry.class)).start();

        HttpRequest request = HttpRequest.builder(UNROUTABLE_IP, 12345)
                .connectionTimeout(TimeValue.timeValueSeconds(5))
                .method(HttpMethod.POST)
                .path("/" + randomAsciiOfLength(5))
                .build();

        long start = System.nanoTime();
        try {
            httpClient.execute(request);
            fail("expected timeout exception");
        } catch (ElasticsearchTimeoutException ete) {
            TimeValue timeout = TimeValue.timeValueNanos(System.nanoTime() - start);
            logger.info("http connection timed out after {}", timeout.format());
            // it's supposed to be 7, but we'll give it an error margin of 2 seconds
            assertThat(timeout.seconds(), greaterThan(3L));
            assertThat(timeout.seconds(), lessThan(7L));
            // expected
        }
    }

}
