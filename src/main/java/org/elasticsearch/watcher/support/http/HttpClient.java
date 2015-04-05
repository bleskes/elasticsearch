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

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.base.Charsets;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Map;

/**
 * Client class to wrap http connections
 */
public class HttpClient extends AbstractComponent {

    private static final String SETTINGS_SSL_PREFIX = "watcher.http.ssl.";
    private static final String SETTINGS_SSL_SHIELD_PREFIX = "shield.ssl.";

    public static final String SETTINGS_SSL_PROTOCOL = SETTINGS_SSL_PREFIX + "protocol";
    private static final String SETTINGS_SSL_SHIELD_CONTEXT_ALGORITHM = SETTINGS_SSL_SHIELD_PREFIX + "context.algorithm";
    public static final String SETTINGS_SSL_TRUSTSTORE = SETTINGS_SSL_PREFIX + "truststore.path";
    private static final String SETTINGS_SSL_SHIELD_TRUSTSTORE = SETTINGS_SSL_SHIELD_PREFIX + "truststore.path";
    public static final String SETTINGS_SSL_TRUSTSTORE_PASSWORD = SETTINGS_SSL_PREFIX + "truststore.password";
    private static final String SETTINGS_SSL_SHIELD_TRUSTSTORE_PASSWORD = SETTINGS_SSL_SHIELD_PREFIX + "truststore.password";
    public static final String SETTINGS_SSL_TRUSTSTORE_ALGORITHM = SETTINGS_SSL_PREFIX + "truststore.algorithm";
    private static final String SETTINGS_SSL_SHIELD_TRUSTSTORE_ALGORITHM = SETTINGS_SSL_SHIELD_PREFIX + "truststore.algorithm";

    final SSLSocketFactory sslSocketFactory;

    @Inject
    public HttpClient(Settings settings) {
        super(settings);
        if (!settings.getByPrefix(SETTINGS_SSL_PREFIX).getAsMap().isEmpty() ||
                !settings.getByPrefix(SETTINGS_SSL_SHIELD_PREFIX).getAsMap().isEmpty()) {
            sslSocketFactory = createSSLSocketFactory(settings);
        } else {
            logger.trace("no ssl context configured");
            sslSocketFactory = null;
        }
    }

    public HttpResponse execute(HttpRequest request) throws IOException {
        String queryString = null;
        if (request.params() != null) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : request.params().entrySet()) {
                if (builder.length() != 0) {
                    builder.append('&');
                }
                builder.append(URLEncoder.encode(entry.getKey(), "utf-8"))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue(), "utf-8"));
            }
            queryString = builder.toString();
        }

        URI uri;
        try {
            uri = new URI(request.scheme().scheme(), null, request.host(), request.port(), request.path(), queryString, null);
        } catch (URISyntaxException e) {
            throw ExceptionsHelper.convertToElastic(e);
        }
        URL url = uri.toURL();

        logger.debug("making [{}] request to [{}]", request.method().method(), url);
        logger.trace("sending [{}] as body of request", request.body());
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (urlConnection instanceof HttpsURLConnection && sslSocketFactory != null) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) urlConnection;
            httpsConn.setSSLSocketFactory(sslSocketFactory);
        }

        urlConnection.setRequestMethod(request.method().method());
        if (request.headers() != null) {
            for (Map.Entry<String, String> entry : request.headers().entrySet()) {
                urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        if (request.auth() != null) {
            request.auth().update(urlConnection);
        }
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Accept-Charset", Charsets.UTF_8.name());
        if (request.body() != null) {
            urlConnection.setDoOutput(true);
            byte[] bytes = request.body().getBytes(Charsets.UTF_8.name());
            urlConnection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            urlConnection.getOutputStream().write(bytes);
            urlConnection.getOutputStream().close();
        }
        urlConnection.connect();

        byte[] body = Streams.copyToByteArray(urlConnection.getInputStream());

        HttpResponse response = new HttpResponse(urlConnection.getResponseCode(), body);
        logger.debug("http status code: {}", response.status());
        return response;
    }

    /** SSL Initialization * */
    private SSLSocketFactory createSSLSocketFactory(Settings settings) {
        SSLContext sslContext;
        // Initialize sslContext
        try {
            String sslContextProtocol = settings.get(SETTINGS_SSL_PROTOCOL, settings.get(SETTINGS_SSL_SHIELD_CONTEXT_ALGORITHM, "TLS"));
            String trustStore = settings.get(SETTINGS_SSL_TRUSTSTORE, settings.get(SETTINGS_SSL_SHIELD_TRUSTSTORE, System.getProperty("javax.net.ssl.trustStore")));
            String trustStorePassword = settings.get(SETTINGS_SSL_TRUSTSTORE_PASSWORD, settings.get(SETTINGS_SSL_SHIELD_TRUSTSTORE_PASSWORD, System.getProperty("javax.net.ssl.trustStorePassword")));
            String trustStoreAlgorithm = settings.get(SETTINGS_SSL_TRUSTSTORE_ALGORITHM, settings.get(SETTINGS_SSL_SHIELD_TRUSTSTORE_ALGORITHM, System.getProperty("ssl.TrustManagerFactory.algorithm")));

            if (trustStore == null) {
                throw new RuntimeException("truststore is not configured, use " + SETTINGS_SSL_TRUSTSTORE);
            }

            if (trustStoreAlgorithm == null) {
                trustStoreAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            }

            logger.debug("SSL: using trustStore[{}], trustAlgorithm[{}]", trustStore, trustStoreAlgorithm);
            Path path = Paths.get(trustStore);
            if (Files.notExists(path)) {
                throw new ElasticsearchIllegalStateException("Truststore at path [" + trustStore + "] does not exist");
            }

            KeyManager[] keyManagers;
            TrustManager[] trustManagers;
            try (InputStream trustStoreStream = Files.newInputStream(path)) {
                // Load TrustStore
                KeyStore ks = KeyStore.getInstance("jks");
                ks.load(trustStoreStream, trustStorePassword == null ? null : trustStorePassword.toCharArray());

                KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                factory.init(ks, trustStorePassword == null ? null : trustStorePassword.toCharArray());
                keyManagers = factory.getKeyManagers();

                // Initialize a trust manager factory with the trusted store
                TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(trustStoreAlgorithm);
                trustFactory.init(ks);

                // Retrieve the trust managers from the factory
                trustManagers = trustFactory.getTrustManagers();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize a TrustManagerFactory", e);
            }

            sslContext = SSLContext.getInstance(sslContextProtocol);
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
        } catch (Exception e) {
            throw new RuntimeException("[http.client] failed to initialize the SSLContext", e);
        }
        return sslContext.getSocketFactory();
    }

}
