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

package org.elasticsearch.xpack.monitoring.resolver;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkDoc;
import org.elasticsearch.xpack.monitoring.action.MonitoringIndex;
import org.elasticsearch.xpack.monitoring.collector.cluster.ClusterStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.collector.indices.IndexRecoveryMonitoringDoc;
import org.elasticsearch.xpack.monitoring.collector.indices.IndexStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.collector.indices.IndicesStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.collector.ml.JobStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.collector.node.NodeStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.collector.shards.ShardMonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.resolver.bulk.MonitoringBulkTimestampedResolver;
import org.elasticsearch.xpack.monitoring.resolver.cluster.ClusterStatsResolver;
import org.elasticsearch.xpack.monitoring.resolver.indices.IndexRecoveryResolver;
import org.elasticsearch.xpack.monitoring.resolver.indices.IndexStatsResolver;
import org.elasticsearch.xpack.monitoring.resolver.indices.IndicesStatsResolver;
import org.elasticsearch.xpack.monitoring.resolver.ml.JobStatsResolver;
import org.elasticsearch.xpack.monitoring.resolver.node.NodeStatsResolver;
import org.elasticsearch.xpack.monitoring.resolver.shards.ShardsResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class ResolversRegistry implements Iterable<MonitoringIndexNameResolver> {

    private final List<Registration> registrations = new ArrayList<>();

    public ResolversRegistry(Settings settings) {
        // register built-in defaults resolvers
        registerBuiltIn(MonitoredSystem.ES, settings);

        final List<String> supportedApiVersions = Arrays.asList(
                MonitoringTemplateUtils.TEMPLATE_VERSION,
                MonitoringTemplateUtils.OLD_TEMPLATE_VERSION
        );

        // register resolvers for monitored systems
        registerMonitoredSystem(MonitoredSystem.KIBANA, settings, supportedApiVersions);
        registerMonitoredSystem(MonitoredSystem.LOGSTASH, settings, supportedApiVersions);
    }

    /**
     * Registers resolvers for elasticsearch documents collected by the monitoring plugin
     */
    private void registerBuiltIn(MonitoredSystem id, Settings settings) {
        registrations.add(resolveByClass(ClusterStatsMonitoringDoc.class, new ClusterStatsResolver(id, settings)));
        registrations.add(resolveByClass(IndexRecoveryMonitoringDoc.class, new IndexRecoveryResolver(id, settings)));
        registrations.add(resolveByClass(IndexStatsMonitoringDoc.class, new IndexStatsResolver(id, settings)));
        registrations.add(resolveByClass(IndicesStatsMonitoringDoc.class, new IndicesStatsResolver(id, settings)));
        registrations.add(resolveByClass(NodeStatsMonitoringDoc.class, new NodeStatsResolver(id, settings)));
        registrations.add(resolveByClass(ShardMonitoringDoc.class, new ShardsResolver(id, settings)));
        registrations.add(resolveByClass(JobStatsMonitoringDoc.class, new JobStatsResolver(id, settings)));
    }

    /**
     * Registers resolvers for monitored systems
     */
    private void registerMonitoredSystem(final MonitoredSystem id, final Settings settings, final List<String> supportedApiVersions) {
        final MonitoringBulkTimestampedResolver timestampedResolver =  new MonitoringBulkTimestampedResolver(id, settings);

        // Note: We resolve requests by the API version that is supplied; this allows us to translate and up-convert any older
        // requests that come through the _xpack/monitoring/_bulk endpoint
        registrations.add(resolveByClassSystemVersion(id, timestampedResolver, MonitoringIndex.TIMESTAMPED, supportedApiVersions));
    }

    /**
     * @return a Resolver that is able to resolver the given monitoring document
     */
    public MonitoringIndexNameResolver getResolver(MonitoringDoc document) {
        for (Registration registration : registrations) {
            if (registration.support(document)) {
                return registration.resolver();
            }
        }
        throw new IllegalArgumentException("No resolver found for monitoring document");
    }

    @Override
    public Iterator<MonitoringIndexNameResolver> iterator() {
        return registrations.stream().map(Registration::resolver).iterator();
    }

    static Registration resolveByClass(Class<? extends MonitoringDoc> type, MonitoringIndexNameResolver resolver) {
        return new Registration(resolver, type::isInstance);
    }

    static Registration resolveByClassSystemVersion(MonitoredSystem system, MonitoringIndexNameResolver  resolver, MonitoringIndex index,
                                                    List<String> supportedApiVersion) {
        return new Registration(resolver, doc -> {
            try {
                if (doc instanceof MonitoringBulkDoc == false || index != ((MonitoringBulkDoc)doc).getIndex()) {
                    return false;
                }
                if (system != MonitoredSystem.fromSystem(doc.getMonitoringId())) {
                    return false;
                }
                return supportedApiVersion.contains(doc.getMonitoringVersion());
            } catch (Exception e) {
                return false;
            }
        });
    }

    static class Registration {

        private final MonitoringIndexNameResolver resolver;
        private final Predicate<MonitoringDoc> predicate;

        Registration(MonitoringIndexNameResolver resolver, Predicate<MonitoringDoc> predicate) {
            this.resolver = Objects.requireNonNull(resolver);
            this.predicate = Objects.requireNonNull(predicate);
        }

        boolean support(MonitoringDoc document) {
            return predicate.test(document);
        }

        MonitoringIndexNameResolver resolver() {
            return resolver;
        }
    }
}
