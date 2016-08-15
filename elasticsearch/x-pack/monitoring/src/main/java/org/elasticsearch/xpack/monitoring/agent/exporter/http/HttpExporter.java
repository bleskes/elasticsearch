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

package org.elasticsearch.xpack.monitoring.agent.exporter.http;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.Version;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;
import org.elasticsearch.xpack.monitoring.agent.exporter.ExportBulk;
import org.elasticsearch.xpack.monitoring.agent.exporter.ExportException;
import org.elasticsearch.xpack.monitoring.agent.exporter.Exporter;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.resolver.MonitoringIndexNameResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.ResolversRegistry;
import org.elasticsearch.xpack.monitoring.support.VersionUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * With the forthcoming addition of the HTTP-based Java Client for ES, we should be able to combine this class with the
 * {@code LocalExporter} implementation, with only a few minor differences:
 *
 * <ul>
 * <li>The {@code HttpExporter} needs to support configuring the certificates and authentication parameters.</li>
 * <li>Depending on how the REST client is implemented, it may or may not allow us to make some calls in the same way
 * (only time will tell; unknown unknowns).</li>
 * </ul>
 */
public class HttpExporter extends Exporter {

    public static final String TYPE = "http";

    public static final String HOST_SETTING = "host";
    public static final String CONNECTION_TIMEOUT_SETTING = "connection.timeout";
    public static final String CONNECTION_READ_TIMEOUT_SETTING = "connection.read_timeout";
    public static final String CONNECTION_KEEP_ALIVE_SETTING = "connection.keep_alive";
    public static final String AUTH_USERNAME_SETTING = "auth.username";
    public static final String AUTH_PASSWORD_SETTING = "auth.password";

    /**
     * A parent setting to header key/value pairs, whose names are user defined.
     */
    public static final String HEADERS = "headers";
    /**
     * Blacklist of headers that the user is not allowed to set.
     * <p>
     * Headers are blacklisted if they have the opportunity to break things and we won't be guaranteed to overwrite them.
     */
    public static final Set<String> BLACKLISTED_HEADERS = Collections.unmodifiableSet(Sets.newHashSet("Content-Length", "Content-Type"));

    /**
     * ES level timeout used when checking and writing templates (used to speed up tests)
     */
    public static final String TEMPLATE_CHECK_TIMEOUT_SETTING = "index.template.master_timeout";
    /**
     * ES level timeout used when checking and writing pipelines (used to speed up tests)
     */
    public static final String PIPELINE_CHECK_TIMEOUT_SETTING = "index.pipeline.master_timeout";

    public static final String SSL_SETTING = "ssl";
    public static final String SSL_PROTOCOL_SETTING = "protocol";
    public static final String SSL_TRUSTSTORE_SETTING = "truststore.path";
    public static final String SSL_TRUSTSTORE_PASSWORD_SETTING = "truststore.password";
    public static final String SSL_TRUSTSTORE_ALGORITHM_SETTING = "truststore.algorithm";
    public static final String SSL_HOSTNAME_VERIFICATION_SETTING = SSL_SETTING + ".hostname_verification";

    /**
     * Minimum supported version of the remote monitoring cluster.
     * <p>
     * We must have support for ingest pipelines, which requires a minimum of 5.0.
     */
    public static final Version MIN_SUPPORTED_CLUSTER_VERSION = Version.V_5_0_0_alpha5;

    private static final XContentType CONTENT_TYPE = XContentType.JSON;

    volatile String[] hosts;
    final TimeValue connectionTimeout;
    final TimeValue connectionReadTimeout;
    final BasicAuth auth;

    /**
     * https support *
     */
    final SSLSocketFactory sslSocketFactory;
    final boolean hostnameVerification;

    final Environment env;
    final ResolversRegistry resolvers;

    @Nullable
    final TimeValue templateCheckTimeout;

    @Nullable
    final TimeValue pipelineCheckTimeout;

    /**
     * Headers supplied by the user to send (likely to a proxy for routing).
     */
    @Nullable
    private final Map<String, String[]> headers;

    volatile boolean checkedAndUploadedIndexTemplate = false;
    volatile boolean checkedAndUploadedIndexPipeline = false;
    volatile boolean supportedClusterVersion = false;

    boolean keepAlive;
    final ConnectionKeepAliveWorker keepAliveWorker;
    Thread keepAliveThread;

    public HttpExporter(Config config, Environment env) {
        super(config);

        this.env = env;
        this.hosts = resolveHosts(config.settings());
        this.auth = resolveAuth(config.settings());
        // allow the user to configure headers
        this.headers = configureHeaders(config.settings());
        this.connectionTimeout = config.settings().getAsTime(CONNECTION_TIMEOUT_SETTING, TimeValue.timeValueMillis(6000));
        this.connectionReadTimeout = config.settings().getAsTime(CONNECTION_READ_TIMEOUT_SETTING,
                TimeValue.timeValueMillis(connectionTimeout.millis() * 10));

        templateCheckTimeout = parseTimeValue(TEMPLATE_CHECK_TIMEOUT_SETTING);
        pipelineCheckTimeout = parseTimeValue(PIPELINE_CHECK_TIMEOUT_SETTING);

        keepAlive = config.settings().getAsBoolean(CONNECTION_KEEP_ALIVE_SETTING, true);
        keepAliveWorker = new ConnectionKeepAliveWorker();

        sslSocketFactory = createSSLSocketFactory(config.settings().getAsSettings(SSL_SETTING));
        hostnameVerification = config.settings().getAsBoolean(SSL_HOSTNAME_VERIFICATION_SETTING, true);

        resolvers = new ResolversRegistry(config.settings());
        // Checks that required templates are loaded
        for (MonitoringIndexNameResolver resolver : resolvers) {
            if (resolver.template() == null) {
                throw new IllegalStateException("unable to find built-in template " + resolver.templateName());
            }
        }

        logger.debug("initialized with hosts [{}], index prefix [{}]",
                Strings.arrayToCommaDelimitedString(hosts), MonitoringIndexNameResolver.PREFIX);
    }

    private String[] resolveHosts(final Settings settings) {
        final String[] hosts = settings.getAsArray(HOST_SETTING);

        if (hosts.length == 0) {
            throw new SettingsException("missing required setting [" + settingFQN(HOST_SETTING) + "]");
        }

        for (String host : hosts) {
            try {
                HttpExporterUtils.parseHostWithPath(host, "");
            } catch (URISyntaxException | MalformedURLException e) {
                throw new SettingsException("[" + settingFQN(HOST_SETTING) + "] invalid host: [" + host + "]", e);
            }
        }

        return hosts;
    }

    private Map<String, String[]> configureHeaders(final Settings settings) {
        final Settings headerSettings = settings.getAsSettings(HEADERS);
        final Set<String> names = headerSettings.names();

        // Most users won't define headers
        if (names.isEmpty()) {
            return null;
        }

        final Map<String, String[]> headers = new HashMap<>();

        // record and validate each header as best we can
        for (final String name : names) {
            if (BLACKLISTED_HEADERS.contains(name)) {
                throw new SettingsException("[" + name + "] cannot be overwritten via [" + settingFQN("headers") + "]");
            }

            final String[] values = headerSettings.getAsArray(name);

            if (values.length == 0) {
                throw new SettingsException("headers must have values, missing for setting [" + settingFQN("headers." + name) + "]");
            }

            headers.put(name, values);
        }

        return Collections.unmodifiableMap(headers);
    }

    private TimeValue parseTimeValue(final String setting) {
        // HORRIBLE!!! We can't use settings.getAsTime(..) !!!
        // WE MUST FIX THIS IN CORE...
        // TimeValue SHOULD NOT SELECTIVELY CHOOSE WHAT FIELDS TO PARSE BASED ON THEIR NAMES!!!!
        final String checkTimeoutValue = config.settings().get(setting, null);

        return TimeValue.parseTimeValue(checkTimeoutValue, null, settingFQN(setting));
    }

    ResolversRegistry getResolvers() {
        return resolvers;
    }

    @Override
    public ExportBulk openBulk() {
        HttpURLConnection connection = openExportingConnection();
        return connection != null ? new Bulk(connection) : null;
    }

    @Override
    public void doClose() {
        if (keepAliveThread != null && keepAliveThread.isAlive()) {
            keepAliveWorker.closed = true;
            keepAliveThread.interrupt();
            try {
                keepAliveThread.join(6000);
            } catch (InterruptedException e) {
                // don't care.
            }
        }
    }

    private String buildQueryString() {
        StringBuilder queryString = new StringBuilder();

        if (bulkTimeout != null) {
            queryString.append("master_timeout=").append(bulkTimeout);
        }

        // allow the use of ingest pipelines to be completely optional
        if (config.settings().getAsBoolean(USE_INGEST_PIPELINE_SETTING, true)) {
            if (queryString.length() != 0) {
                queryString.append('&');
            }

            queryString.append("pipeline=").append(EXPORT_PIPELINE_NAME);
        }

        return queryString.length() != 0 ? '?' + queryString.toString() : "";
    }

    private HttpURLConnection openExportingConnection() {
        logger.trace("setting up an export connection");

        final String queryString = buildQueryString();
        HttpURLConnection conn = openAndValidateConnection("POST", "/_bulk" + queryString, CONTENT_TYPE.mediaType());
        if (conn != null && (keepAliveThread == null || !keepAliveThread.isAlive())) {
            // start keep alive upon successful connection if not there.
            initKeepAliveThread();
        }
        return conn;
    }

    private void render(MonitoringDoc doc, OutputStream out) throws IOException {
        try {
            MonitoringIndexNameResolver<MonitoringDoc> resolver = resolvers.getResolver(doc);
            if (resolver != null) {
                String index = resolver.index(doc);
                String type = resolver.type(doc);
                String id = resolver.id(doc);

                try (XContentBuilder builder = new XContentBuilder(CONTENT_TYPE.xContent(), out)) {
                    // Builds the bulk action metadata line
                    builder.startObject();
                    builder.startObject("index");
                    builder.field("_index", index);
                    builder.field("_type", type);
                    if (id != null) {
                        builder.field("_id", id);
                    }
                    builder.endObject();
                    builder.endObject();
                }

                // Adds action metadata line bulk separator
                out.write(CONTENT_TYPE.xContent().streamSeparator());

                // Render the monitoring document
                BytesRef bytesRef = resolver.source(doc, CONTENT_TYPE).toBytesRef();
                out.write(bytesRef.bytes, bytesRef.offset, bytesRef.length);

                // Adds final bulk separator
                out.write(CONTENT_TYPE.xContent().streamSeparator());

                if (logger.isTraceEnabled()) {
                    logger.trace("added index request [index={}, type={}, id={}]", index, type, id);
                }
            } else if (logger.isTraceEnabled()) {
                logger.trace("no resolver found for monitoring document [class={}, id={}, version={}]",
                        doc.getClass().getName(), doc.getMonitoringId(), doc.getMonitoringVersion());
            }
        } catch (Exception e) {
            logger.warn(new ParameterizedMessage("failed to render document [{}], skipping it", doc), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendCloseExportingConnection(HttpURLConnection conn) throws IOException {
        logger.trace("sending content");
        closeExportingConnection(conn);
        if (conn.getResponseCode() != 200) {
            logConnectionError("remote target didn't respond with 200 OK", conn);
            return;
        }

        InputStream inputStream = conn.getInputStream();
        try (XContentParser parser = CONTENT_TYPE.xContent().createParser(inputStream)) {
            Map<String, Object> response = parser.map();
            if (response.get("items") != null) {
                ArrayList<Object> list = (ArrayList<Object>) response.get("items");
                for (Object itemObject : list) {
                    Map<String, Object> actions = (Map<String, Object>) itemObject;
                    for (String actionKey : actions.keySet()) {
                        Map<String, Object> action = (Map<String, Object>) actions.get(actionKey);
                        if (action.get("error") != null) {
                            logger.error("{} failure (index:[{}] type: [{}]): {}", actionKey, action.get("_index"), action.get("_type"),
                                    action.get("error"));
                        }
                    }
                }
            }
        }
    }

    private void closeExportingConnection(HttpURLConnection connection) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            logger.debug("closing exporting connection [{}]", connection);
        }
    }

    /**
     * open a connection to any host, validating it has the template installed if needed
     *
     * @return a url connection to the selected host or null if no current host is available.
     */
    private HttpURLConnection openAndValidateConnection(String method, String path, String contentType) {
        // allows us to move faulty hosts to the end; the HTTP Client will make this code obsolete
        int hostIndex = 0;
        try {
            for (; hostIndex < hosts.length; hostIndex++) {
                String host = hosts[hostIndex];
                if (!supportedClusterVersion) {
                    try {
                        Version remoteVersion = loadRemoteClusterVersion(host);
                        if (remoteVersion == null) {
                            logger.warn("unable to check remote cluster version: no version found on host [{}]", host);
                            continue;
                        }
                        supportedClusterVersion = remoteVersion.onOrAfter(MIN_SUPPORTED_CLUSTER_VERSION);
                        if (!supportedClusterVersion) {
                            logger.error("remote cluster version [{}] is not supported, please use a cluster with minimum version [{}]",
                                    remoteVersion, MIN_SUPPORTED_CLUSTER_VERSION);
                            continue;
                        }
                    } catch (ElasticsearchException e) {
                        logger.error(new ParameterizedMessage("exception when checking remote cluster version on host [{}]", host), e);
                        continue;
                    }
                }

                // NOTE: This assumes that the user is configured properly and only sending to a single cluster
                if (checkedAndUploadedIndexTemplate == false || checkedAndUploadedIndexPipeline == false) {
                    checkedAndUploadedIndexTemplate = checkAndUploadIndexTemplate(host);
                    checkedAndUploadedIndexPipeline = checkedAndUploadedIndexTemplate && checkAndUploadIndexPipeline(host);

                    // did we fail?
                    if (checkedAndUploadedIndexTemplate == false || checkedAndUploadedIndexPipeline == false) {
                        continue;
                    }
                }

                HttpURLConnection connection = openConnection(host, method, path, contentType);
                if (connection != null) {
                    return connection;
                }
                // failed hosts - reset template & cluster versions check, someone may have restarted the target cluster and deleted
                // it's data folder. be safe.
                checkedAndUploadedIndexTemplate = false;
                checkedAndUploadedIndexPipeline = false;
                supportedClusterVersion = false;
            }
        } finally {
            if (hostIndex > 0 && hostIndex < hosts.length) {
                logger.debug("moving [{}] failed hosts to the end of the list", hostIndex);
                String[] newHosts = new String[hosts.length];
                System.arraycopy(hosts, hostIndex, newHosts, 0, hosts.length - hostIndex);
                System.arraycopy(hosts, 0, newHosts, hosts.length - hostIndex, hostIndex);
                hosts = newHosts;
                logger.debug("preferred target host is now [{}]", hosts[0]);
            }
        }

        logger.error("could not connect to any configured elasticsearch instances [{}]", Strings.arrayToCommaDelimitedString(hosts));

        return null;
    }

    /**
     * open a connection to the given hosts, returning null when not successful *
     */
    private HttpURLConnection openConnection(String host, String method, String path, @Nullable String contentType) {
        // the HTTP Client will make this code obsolete
        try {
            final URL url = HttpExporterUtils.parseHostWithPath(host, path);
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Custom Headers must be set before we manually apply headers, so that our headers beat custom ones
            if (headers != null) {
                // Headers can technically be duplicated, although it's not expected to be used frequently
                for (final Map.Entry<String, String[]> header : headers.entrySet()) {
                    for (final String value : header.getValue()) {
                        conn.addRequestProperty(header.getKey(), value);
                    }
                }
            }

            if (conn instanceof HttpsURLConnection && sslSocketFactory != null) {
                final HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                final SSLSocketFactory factory = sslSocketFactory;

                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    sm.checkPermission(new SpecialPermission());
                }
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    // Requires permission java.lang.RuntimePermission "setFactory";
                    httpsConn.setSSLSocketFactory(factory);

                    // Requires permission javax.net.ssl.SSLPermission "setHostnameVerifier";
                    if (hostnameVerification == false) {
                        httpsConn.setHostnameVerifier(TrustAllHostnameVerifier.INSTANCE);
                    }
                    return null;
                });
            }

            conn.setRequestMethod(method);
            conn.setConnectTimeout((int) connectionTimeout.getMillis());
            conn.setReadTimeout((int) connectionReadTimeout.getMillis());
            if (contentType != null) {
                conn.setRequestProperty("Content-Type", contentType);
            }
            if (auth != null) {
                auth.apply(conn);
            }
            conn.setUseCaches(false);
            if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
                conn.setDoOutput(true);
            }
            conn.connect();

            return conn;
        } catch (URISyntaxException e) {
            logger.error(new ParameterizedMessage("error parsing host [{}]", host), e);
        } catch (IOException e) {
            logger.error(new ParameterizedMessage("error connecting to [{}]", host), e);
        }
        return null;
    }

    /**
     * Get the version of the remote monitoring cluster
     */
    Version loadRemoteClusterVersion(final String host) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(host, "GET", "/", null);
            if (connection == null) {
                throw new ElasticsearchException("unable to check remote cluster version: no available connection for host [" + host + "]");
            }

            try (InputStream is = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Streams.copy(is, out);
                return VersionUtils.parseVersion(out.toByteArray());
            }
        } catch (IOException e) {
            throw new ElasticsearchException("failed to verify the remote cluster version on host [" + host + "]", e);
        } finally {
            if (connection != null) {
                try {
                    connection.getInputStream().close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Checks if the index pipeline already exists and, if not, uploads it.
     *
     * @return {@code true} if the pipeline exists after executing.
     * @throws RuntimeException if any error occurs that should prevent indexing
     */
    private boolean checkAndUploadIndexPipeline(final String host) {
        if (hasPipeline(host) == false) {
            logger.debug("monitoring pipeline [{}] not found", EXPORT_PIPELINE_NAME);

            return putPipeline(host);
        } else {
            logger.trace("monitoring pipeline [{}] found", EXPORT_PIPELINE_NAME);
        }

        return true;
    }

    private boolean hasPipeline(final String host) {
        final String url = urlWithMasterTimeout("_ingest/pipeline/" + EXPORT_PIPELINE_NAME, pipelineCheckTimeout);

        HttpURLConnection connection = null;
        try {
            logger.trace("checking if monitoring pipeline [{}] exists on the monitoring cluster", EXPORT_PIPELINE_NAME);
            connection = openConnection(host, "GET", url, null);
            if (connection == null) {
                throw new IOException("no available connection to check for monitoring pipeline [" + EXPORT_PIPELINE_NAME + "] existence");
            }

            // 200 means that the template has been found, 404 otherwise
            if (connection.getResponseCode() == 200) {
                logger.debug("monitoring pipeline [{}] found", EXPORT_PIPELINE_NAME);
                return true;
            }
        } catch (Exception e) {
            logger.error(new ParameterizedMessage("failed to verify the monitoring pipeline [{}] on [{}]", EXPORT_PIPELINE_NAME, host), e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.getInputStream().close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return false;
    }

    private boolean putPipeline(final String host) {
        logger.trace("installing pipeline [{}]", EXPORT_PIPELINE_NAME);

        HttpURLConnection connection = null;

        try {
            connection = openConnection(host, "PUT", "_ingest/pipeline/" + EXPORT_PIPELINE_NAME, XContentType.JSON.mediaType());
            if (connection == null) {
                logger.debug("no available connection to upload monitoring pipeline [{}]", EXPORT_PIPELINE_NAME);
                return false;
            }

            // Uploads the template and closes the outputstream
            Streams.copy(BytesReference.toBytes(emptyPipeline(XContentType.JSON).bytes()), connection.getOutputStream());
            if (connection.getResponseCode() != 200 && connection.getResponseCode() != 201) {
                logConnectionError("error adding the monitoring pipeline [" + EXPORT_PIPELINE_NAME + "] to [" + host + "]", connection);
                return false;
            }

            logger.info("monitoring pipeline [{}] set", EXPORT_PIPELINE_NAME);
            return true;
        } catch (IOException e) {
            logger.error(new ParameterizedMessage("failed to update monitoring pipeline [{}] on host [{}]", EXPORT_PIPELINE_NAME, host), e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.getInputStream().close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Checks if the index templates already exist and if not uploads it
     *
     * @return true if template exists after executing.
     * @throws RuntimeException if any error occurs that should prevent indexing
     */
    private boolean checkAndUploadIndexTemplate(final String host) {
        // List of distinct templates
        Map<String, String> templates = StreamSupport.stream(new ResolversRegistry(Settings.EMPTY).spliterator(), false)
                .collect(Collectors.toMap(MonitoringIndexNameResolver::templateName, MonitoringIndexNameResolver::template, (a, b) -> a));

        for (Map.Entry<String, String> template : templates.entrySet()) {
            if (hasTemplate(template.getKey(), host) == false) {
                logger.debug("template [{}] not found", template.getKey());
                if (putTemplate(host, template.getKey(), template.getValue()) == false) {
                    return false;
                }
            } else {
                logger.debug("template [{}] found", template.getKey());
            }
        }
        return true;
    }

    private boolean hasTemplate(String templateName, String host) {
        final String url = urlWithMasterTimeout("_template/" + templateName, templateCheckTimeout);

        HttpURLConnection connection = null;
        try {
            logger.debug("checking if monitoring template [{}] exists on the monitoring cluster", templateName);
            connection = openConnection(host, "GET", url, null);
            if (connection == null) {
                throw new IOException("no available connection to check for monitoring template [" + templateName + "] existence");
            }

            // 200 means that the template has been found, 404 otherwise
            if (connection.getResponseCode() == 200) {
                logger.debug("monitoring template [{}] found", templateName);
                return true;
            }
        } catch (Exception e) {
            logger.error(new ParameterizedMessage("failed to verify the monitoring template [{}] on [{}]", templateName, host), e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.getInputStream().close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return false;
    }

    boolean putTemplate(String host, String template, String source) {
        logger.debug("installing template [{}]", template);
        HttpURLConnection connection = null;
        try {
            connection = openConnection(host, "PUT", "_template/" + template, XContentType.JSON.mediaType());
            if (connection == null) {
                logger.debug("no available connection to update monitoring template [{}]", template);
                return false;
            }

            // Uploads the template and closes the outputstream
            Streams.copy(source.getBytes(StandardCharsets.UTF_8), connection.getOutputStream());
            if (connection.getResponseCode() != 200 && connection.getResponseCode() != 201) {
                logConnectionError("error adding the monitoring template [" + template + "] to [" + host + "]", connection);
                return false;
            }

            logger.info("monitoring template [{}] updated ", template);
            return true;
        } catch (IOException e) {
            logger.error(new ParameterizedMessage("failed to update monitoring template [{}] on host [{}]", template, host), e);
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.getInputStream().close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Get the {@code url} with the optional {@code masterTimeout}.
     * <p>
     * This method assumes that there is no query string applied yet!
     *
     * @param url The URL being used
     * @param masterTimeout The optional master_timeout
     * @return Never {@code null}
     */
    private String urlWithMasterTimeout(final String url, final TimeValue masterTimeout) {
        if (masterTimeout != null) {
            return url + "?master_timeout=" + masterTimeout;
        }

        return url;
    }

    private void logConnectionError(String msg, HttpURLConnection conn) {
        InputStream inputStream = conn.getErrorStream();
        String err = "";
        if (inputStream != null) {
            java.util.Scanner s = new java.util.Scanner(inputStream, "UTF-8").useDelimiter("\\A");
            err = s.hasNext() ? s.next() : "";
        }

        try {
            logger.error("{} response code [{} {}]. content: [{}]",
                    msg, conn.getResponseCode(),
                    conn.getResponseMessage(),
                    err);
        } catch (IOException e) {
            logger.error("{}. connection had an error while reporting the error. tough life.", msg);
        }
    }

    protected void initKeepAliveThread() {
        if (keepAlive) {
            keepAliveThread = new Thread(keepAliveWorker, "monitoring-exporter[" + config.name() + "][keep_alive]");
            keepAliveThread.setDaemon(true);
            keepAliveThread.start();
        }
    }

    /**
     * SSL Initialization *
     */
    public SSLSocketFactory createSSLSocketFactory(Settings settings) {
        if (settings.names().isEmpty()) {
            logger.trace("no ssl context configured");
            return null;
        }
        SSLContext sslContext;
        // Initialize sslContext
        try {
            String protocol = settings.get(SSL_PROTOCOL_SETTING, "TLS");
            String trustStore = settings.get(SSL_TRUSTSTORE_SETTING, System.getProperty("javax.net.ssl.trustStore"));
            String trustStorePassword = settings.get(SSL_TRUSTSTORE_PASSWORD_SETTING,
                    System.getProperty("javax.net.ssl.trustStorePassword"));
            String trustStoreAlgorithm = settings.get(SSL_TRUSTSTORE_ALGORITHM_SETTING,
                    System.getProperty("ssl.TrustManagerFactory.algorithm"));

            if (trustStore == null) {
                throw new SettingsException("missing required setting [" + SSL_TRUSTSTORE_SETTING + "]");
            }

            if (trustStoreAlgorithm == null) {
                trustStoreAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            }

            logger.debug("using ssl trust store [{}] with algorithm [{}]", trustStore, trustStoreAlgorithm);

            Path trustStorePath = env.configFile().resolve(trustStore);
            if (!Files.exists(trustStorePath)) {
                throw new SettingsException("could not find trust store file [" + trustStorePath + "]");
            }

            TrustManager[] trustManagers;
            try (InputStream trustStoreStream = Files.newInputStream(trustStorePath)) {
                // Load TrustStore
                KeyStore ks = KeyStore.getInstance("jks");
                ks.load(trustStoreStream, trustStorePassword == null ? null : trustStorePassword.toCharArray());

                // Initialize a trust manager factory with the trusted store
                TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(trustStoreAlgorithm);
                trustFactory.init(ks);

                // Retrieve the trust managers from the factory
                trustManagers = trustFactory.getTrustManagers();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize a TrustManagerFactory", e);
            }

            sslContext = SSLContext.getInstance(protocol);
            sslContext.init(null, trustManagers, null);

        } catch (Exception e) {
            throw new ElasticsearchException("failed to initialize ssl", e);
        }
        return sslContext.getSocketFactory();
    }

    BasicAuth resolveAuth(Settings setting) {
        String username = setting.get(AUTH_USERNAME_SETTING, null);
        String password = setting.get(AUTH_PASSWORD_SETTING, null);
        if (username == null && password == null) {
            return null;
        }
        if (username == null) {
            throw new SettingsException("invalid auth setting. missing [" + settingFQN(AUTH_USERNAME_SETTING) + "]");
        }
        return new BasicAuth(username, password);
    }

    /**
     * Trust all hostname verifier. This simply returns true to completely disable hostname verification
     */
    static class TrustAllHostnameVerifier implements HostnameVerifier {
        static final HostnameVerifier INSTANCE = new TrustAllHostnameVerifier();

        private TrustAllHostnameVerifier() {
        }

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    /**
     * Sadly we need to make sure we keep the connection open to the target ES a
     * Java's connection pooling closes connections if idle for 5sec.
     */
    class ConnectionKeepAliveWorker implements Runnable {
        volatile boolean closed = false;

        @Override
        public void run() {
            logger.trace("starting keep alive thread");
            while (!closed) {
                try {
                    Thread.sleep(1000);
                    if (closed) {
                        return;
                    }
                    String[] currentHosts = hosts;
                    if (currentHosts.length == 0) {
                        logger.trace("keep alive thread shutting down. no hosts defined");
                        return; // no hosts configured at the moment.
                    }
                    HttpURLConnection conn = openConnection(currentHosts[0], "GET", "", null);
                    if (conn == null) {
                        logger.trace("keep alive thread shutting down. failed to open connection to current host [{}]", currentHosts[0]);
                        return;
                    } else {
                        conn.getInputStream().close(); // close and release to connection pool.
                    }
                } catch (InterruptedException e) {
                    // ignore, if closed, good....
                } catch (Exception e) {
                    logger.debug("error in keep alive thread, shutting down (will be restarted after a successful connection has been " +
                            "made) {}", ExceptionsHelper.detailedMessage(e));
                    return;
                }
            }
        }
    }

    static class BasicAuth {

        String username;
        char[] password;

        public BasicAuth(String username, String password) {
            this.username = username;
            this.password = password != null ? password.toCharArray() : null;
        }

        void apply(HttpURLConnection connection) throws UnsupportedEncodingException {
            String userInfo = username + ":" + (password != null ? new String(password) : "");
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(userInfo.getBytes("ISO-8859-1"));
            connection.setRequestProperty("Authorization", basicAuth);
        }
    }

    class Bulk extends ExportBulk {

        private HttpURLConnection connection;
        private OutputStream out;

        public Bulk(HttpURLConnection connection) {
            super(name());
            this.connection = connection;
        }

        @Override
        public void doAdd(Collection<MonitoringDoc> docs) throws ExportException {
            try {
                if ((docs != null) && (!docs.isEmpty())) {
                    if (connection == null) {
                        connection = openExportingConnection();
                        if (connection == null) {
                            throw new IllegalStateException("No connection available to export documents");
                        }
                    }
                    if (out == null) {
                        out = connection.getOutputStream();
                    }

                    // We need to use a buffer to render each monitoring document
                    // because the renderer might close the outputstream (ex: XContentBuilder)
                    try (BytesStreamOutput buffer = new BytesStreamOutput()) {
                        for (MonitoringDoc monitoringDoc : docs) {
                            try {
                                render(monitoringDoc, buffer);
                                BytesRef bytesRef = buffer.bytes().toBytesRef();
                                // write the result to the connection
                                out.write(bytesRef.bytes, bytesRef.offset, bytesRef.length);
                            } finally {
                                buffer.reset();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new ExportException("failed to add documents to export bulk [{}]", name);
            }
        }

        @Override
        public void doFlush() throws ExportException {
            if (connection != null) {
                try {
                    sendCloseExportingConnection(connection);
                } catch (Exception e) {
                    throw new ExportException("failed to flush export bulk [{}]", e, name);
                } finally {
                    connection = null;
                }
            }
        }

        @Override
        protected void doClose() throws ExportException {
            if (connection != null) {
                try {
                    closeExportingConnection(connection);
                } catch (Exception e) {
                    throw new ExportException("failed to close export bulk [{}]", e, name);
                } finally {
                    connection = null;
                }
            }
        }
    }
}
