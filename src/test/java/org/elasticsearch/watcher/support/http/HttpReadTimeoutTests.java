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

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.support.http.auth.HttpAuthRegistry;
import org.elasticsearch.watcher.support.secret.SecretService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.BindException;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;

/**
 */
public class HttpReadTimeoutTests extends ElasticsearchTestCase {

    private MockWebServer webServer;
    private SecretService secretService;
    private int webPort;

    @Before
    public void init() throws Exception {
        secretService = new SecretService.PlainText();
        for (webPort = 9200; webPort < 9300; webPort++) {
            try {
                webServer = new MockWebServer();
                webServer.start(webPort);
                return;
            } catch (BindException be) {
                logger.warn("port [{}] was already in use trying next port", webPort);
            }
        }
        throw new ElasticsearchException("unable to find open port between 9200 and 9300");
    }

    @After
    public void after() throws Exception {
        webServer.shutdown();
    }

    @Test
    public void testDefaultTimeout() throws Exception {
        HttpClient httpClient = new HttpClient(ImmutableSettings.EMPTY, mock(HttpAuthRegistry.class)).start();

        // we're not going to enqueue an response... so the server will just hang

        HttpRequest request = HttpRequest.builder("localhost", webPort)
                .method(HttpMethod.POST)
                .path("/" + randomAsciiOfLength(5))
                .build();

        long start = System.nanoTime();
        try {
            httpClient.execute(request);
            fail("expected read timeout after 10 seconds (default)");
        } catch (ElasticsearchTimeoutException ete) {
            // expected

            TimeValue timeout = TimeValue.timeValueNanos(System.nanoTime() - start);
            logger.info("http connection timed out after {}", timeout.format());

            // it's supposed to be 10, but we'll give it an error margin of 2 seconds
            assertThat(timeout.seconds(), greaterThan(8L));
            assertThat(timeout.seconds(), lessThan(12L));

            // lets enqueue a response to relese the server.
            webServer.enqueue(new MockResponse());
        }
    }

    @Test
    public void testDefaultTimeout_Custom() throws Exception {
        HttpClient httpClient = new HttpClient(ImmutableSettings.builder()
                .put("watcher.http.default_read_timeout", "5s")
                .build()
                , mock(HttpAuthRegistry.class)).start();

        // we're not going to enqueue an response... so the server will just hang

        HttpRequest request = HttpRequest.builder("localhost", webPort)
                .method(HttpMethod.POST)
                .path("/" + randomAsciiOfLength(5))
                .build();

        long start = System.nanoTime();
        try {
            httpClient.execute(request);
            fail("expected read timeout after 5 seconds (default)");
        } catch (ElasticsearchTimeoutException ete) {
            // expected

            TimeValue timeout = TimeValue.timeValueNanos(System.nanoTime() - start);
            logger.info("http connection timed out after {}", timeout.format());

            // it's supposed to be 5, but we'll give it an error margin of 2 seconds
            assertThat(timeout.seconds(), greaterThan(3L));
            assertThat(timeout.seconds(), lessThan(7L));

            // lets enqueue a response to relese the server.
            webServer.enqueue(new MockResponse());
        }
    }

    @Test
    public void testTimeout_CustomPerRequest() throws Exception {
        HttpClient httpClient = new HttpClient(ImmutableSettings.builder()
                .put("watcher.http.default_read_timeout", "10s")
                .build()
                , mock(HttpAuthRegistry.class)).start();

        // we're not going to enqueue an response... so the server will just hang

        HttpRequest request = HttpRequest.builder("localhost", webPort)
                .readTimeout(TimeValue.timeValueSeconds(5))
                .method(HttpMethod.POST)
                .path("/" + randomAsciiOfLength(5))
                .build();

        long start = System.nanoTime();
        try {
            httpClient.execute(request);
            fail("expected read timeout after 5 seconds (default)");
        } catch (ElasticsearchTimeoutException ete) {
            // expected

            TimeValue timeout = TimeValue.timeValueNanos(System.nanoTime() - start);
            logger.info("http connection timed out after {}", timeout.format());

            // it's supposed to be 5, but we'll give it an error margin of 2 seconds
            assertThat(timeout.seconds(), greaterThan(3L));
            assertThat(timeout.seconds(), lessThan(7L));

            // lets enqueue a response to relese the server.
            webServer.enqueue(new MockResponse());
        }
    }
}
