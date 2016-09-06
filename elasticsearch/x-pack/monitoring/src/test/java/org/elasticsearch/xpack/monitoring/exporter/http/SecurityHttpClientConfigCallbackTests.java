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
package org.elasticsearch.xpack.monitoring.exporter.http;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.elasticsearch.test.ESTestCase;

import static org.mockito.Mockito.mock;

/**
 * Tests {@link SecurityHttpClientConfigCallback}.
 */
public class SecurityHttpClientConfigCallbackTests extends ESTestCase {

    private final CredentialsProvider credentialsProvider = mock(CredentialsProvider.class);
    private final SSLIOSessionStrategy sslStrategy = mock(SSLIOSessionStrategy.class);
    /**
     * HttpAsyncClientBuilder's methods are {@code final} and therefore not verifiable.
     */
    private final HttpAsyncClientBuilder builder = mock(HttpAsyncClientBuilder.class);

    public void testSSLIOSessionStrategyNullThrowsException() {
        final CredentialsProvider optionalCredentialsProvider = randomFrom(credentialsProvider, null);

        expectThrows(NullPointerException.class, () -> new SecurityHttpClientConfigCallback(null, optionalCredentialsProvider));
    }

    public void testCustomizeHttpClient() {
        final SecurityHttpClientConfigCallback callback = new SecurityHttpClientConfigCallback(sslStrategy, credentialsProvider);

        assertSame(credentialsProvider, callback.getCredentialsProvider());
        assertSame(sslStrategy, callback.getSSLStrategy());

        assertSame(builder, callback.customizeHttpClient(builder));
    }

    public void testCustomizeHttpClientWithOptionalParameters() {
        final CredentialsProvider optionalCredentialsProvider = randomFrom(credentialsProvider, null);

        final SecurityHttpClientConfigCallback callback =
            new SecurityHttpClientConfigCallback(sslStrategy, optionalCredentialsProvider);

        assertSame(builder, callback.customizeHttpClient(builder));
        assertSame(optionalCredentialsProvider, callback.getCredentialsProvider());
        assertSame(sslStrategy, callback.getSSLStrategy());
    }

}
