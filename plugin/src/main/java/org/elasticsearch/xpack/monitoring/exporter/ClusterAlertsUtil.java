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
package org.elasticsearch.xpack.monitoring.exporter;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.Streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * {@code ClusterAlertsUtil} provides static methods to easily load the JSON resources that
 * represent watches for Cluster Alerts.
 */
public class ClusterAlertsUtil {

    /**
     * The name of the Watch resource when substituted by the high-level watch ID.
     */
    private static final String WATCH_FILE = "/monitoring/watches/%s.json";
    /**
     * Replace the <code>${monitoring.watch.cluster_uuid}</code> field in the watches.
     */
    private static final Pattern CLUSTER_UUID_PROPERTY =
            Pattern.compile(Pattern.quote("${monitoring.watch.cluster_uuid}"));
    /**
     * Replace the <code>${monitoring.watch.id}</code> field in the watches.
     */
    private static final Pattern WATCH_ID_PROPERTY =
            Pattern.compile(Pattern.quote("${monitoring.watch.id}"));
    /**
     * Replace the <code>${monitoring.watch.unique_id}</code> field in the watches.
     *
     * @see #createUniqueWatchId(ClusterService, String)
     */
    private static final Pattern UNIQUE_WATCH_ID_PROPERTY =
            Pattern.compile(Pattern.quote("${monitoring.watch.unique_id}"));

    /**
     * The last time that all watches were updated. For now, all watches have been updated in the same version and should all be replaced
     * together.
     */
    public static final int LAST_UPDATED_VERSION = Version.V_5_5_0_UNRELEASED.id;

    /**
     * An unsorted list of Watch IDs representing resource files for Monitoring Cluster Alerts.
     */
    public static final String[] WATCH_IDS = {
        "elasticsearch_cluster_status",
        "elasticsearch_version_mismatch",
        "kibana_version_mismatch",
        "logstash_version_mismatch"
    };

    /**
     * Create a unique identifier for the watch and cluster.
     *
     * @param clusterService The cluster service used to fetch the latest cluster state.
     * @param watchId The watch's ID.
     * @return Never {@code null}.
     * @see #WATCH_IDS
     */
    public static String createUniqueWatchId(final ClusterService clusterService, final String watchId) {
        return createUniqueWatchId(clusterService.state().metaData().clusterUUID(), watchId);
    }

    /**
     * Create a unique identifier for the watch and cluster.
     *
     * @param clusterUuid The cluster's UUID.
     * @param watchId The watch's ID.
     * @return Never {@code null}.
     * @see #WATCH_IDS
     */
    private static String createUniqueWatchId(final String clusterUuid, final String watchId) {
        return clusterUuid + "_" + watchId;
    }

    /**
     * Create a unique watch ID and load the {@code watchId} resource by replacing variables,
     * such as the cluster's UUID.
     *
     * @param clusterService The cluster service used to fetch the latest cluster state.
     * @param watchId The watch's ID.
     * @return Never {@code null}. The key is the unique watch ID. The value is the Watch source.
     * @throws RuntimeException if the watch does not exist
     */
    public static String loadWatch(final ClusterService clusterService, final String watchId) {
        final String resource = String.format(Locale.ROOT, WATCH_FILE, watchId);

        try {
            final String clusterUuid = clusterService.state().metaData().clusterUUID();
            final String uniqueWatchId = createUniqueWatchId(clusterUuid, watchId);

            // load the resource as-is
            String source = loadResource(resource).utf8ToString();

            source = CLUSTER_UUID_PROPERTY.matcher(source).replaceAll(clusterUuid);
            source = WATCH_ID_PROPERTY.matcher(source).replaceAll(watchId);
            source = UNIQUE_WATCH_ID_PROPERTY.matcher(source).replaceAll(uniqueWatchId);

            return source;
        } catch (final IOException e) {
            throw new RuntimeException("Unable to load Watch [" + watchId + "]", e);
        }
    }

    private static BytesReference loadResource(final String resource) throws IOException {
        try (InputStream is = ClusterAlertsUtil.class.getResourceAsStream(resource)) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Streams.copy(is, out);

                return new BytesArray(out.toByteArray());
            }
        }
    }

}
