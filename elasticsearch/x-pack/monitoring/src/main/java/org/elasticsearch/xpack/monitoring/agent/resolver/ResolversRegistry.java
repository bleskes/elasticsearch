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

package org.elasticsearch.xpack.monitoring.agent.resolver;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkDoc;
import org.elasticsearch.xpack.monitoring.action.MonitoringIndex;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterInfoMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterStateMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterStateNodeMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.DiscoveryNodeMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.indices.IndexRecoveryMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.indices.IndexStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.indices.IndicesStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.node.NodeStatsMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.collector.shards.ShardMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringTemplateUtils;
import org.elasticsearch.xpack.monitoring.agent.resolver.bulk.MonitoringBulkDataResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.bulk.MonitoringBulkTimestampedResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.cluster.ClusterInfoResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.cluster.ClusterStateNodeResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.cluster.ClusterStateResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.cluster.ClusterStatsResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.cluster.DiscoveryNodeResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.indices.IndexRecoveryResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.indices.IndexStatsResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.indices.IndicesStatsResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.node.NodeStatsResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.shards.ShardsResolver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class ResolversRegistry implements Iterable<MonitoringIndexNameResolver> {

    private final List<Registration> registrations = new ArrayList<>();

    public ResolversRegistry(Settings settings) {
        // register built-in defaults resolvers
        registerBuiltIn(MonitoredSystem.ES, settings);

        // register resolvers for monitored systems
        registerMonitoredSystem(MonitoredSystem.KIBANA, settings);
    }

    /**
     * Registers resolvers for elasticsearch documents collected by the monitoring plugin
     */
    private void registerBuiltIn(MonitoredSystem id, Settings settings) {
        registrations.add(resolveByClass(ClusterInfoMonitoringDoc.class, new ClusterInfoResolver()));
        registrations.add(resolveByClass(ClusterStateNodeMonitoringDoc.class, new ClusterStateNodeResolver(id, settings)));
        registrations.add(resolveByClass(ClusterStateMonitoringDoc.class, new ClusterStateResolver(id, settings)));
        registrations.add(resolveByClass(ClusterStatsMonitoringDoc.class, new ClusterStatsResolver(id, settings)));
        registrations.add(resolveByClass(DiscoveryNodeMonitoringDoc.class, new DiscoveryNodeResolver()));
        registrations.add(resolveByClass(IndexRecoveryMonitoringDoc.class, new IndexRecoveryResolver(id, settings)));
        registrations.add(resolveByClass(IndexStatsMonitoringDoc.class, new IndexStatsResolver(id, settings)));
        registrations.add(resolveByClass(IndicesStatsMonitoringDoc.class, new IndicesStatsResolver(id, settings)));
        registrations.add(resolveByClass(NodeStatsMonitoringDoc.class, new NodeStatsResolver(id, settings)));
        registrations.add(resolveByClass(ShardMonitoringDoc.class, new ShardsResolver(id, settings)));
    }

    /**
     * Registers resolvers for monitored systems
     */
    private void registerMonitoredSystem(MonitoredSystem id, Settings settings) {
        final MonitoringBulkDataResolver dataResolver =  new MonitoringBulkDataResolver();
        final MonitoringBulkTimestampedResolver timestampedResolver =  new MonitoringBulkTimestampedResolver(id, settings);

        final String currentApiVersion = MonitoringTemplateUtils.TEMPLATE_VERSION;

        // Note: We resolve requests by the API version that is supplied; this allows us to translate and up-convert any older
        // requests that come through the _xpack/monitoring/_bulk endpoint
        registrations.add(resolveByClassSystemVersion(id, dataResolver, MonitoringIndex.DATA, currentApiVersion));
        registrations.add(resolveByClassSystemVersion(id, timestampedResolver, MonitoringIndex.TIMESTAMPED, currentApiVersion));
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
                                                    String apiVersion) {
        return new Registration(resolver, doc -> {
            try {
                if (doc instanceof MonitoringBulkDoc == false || index != ((MonitoringBulkDoc)doc).getIndex()) {
                    return false;
                }
                if (system != MonitoredSystem.fromSystem(doc.getMonitoringId())) {
                    return false;
                }
                return apiVersion.equals(doc.getMonitoringVersion());
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
