/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.IndexTemplateMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.template.TemplateUtils;

import static org.elasticsearch.common.xcontent.XContentHelper.convertToMap;

/**
 * Manages the lifecycle of a single index, its template, mapping and and data upgrades/migrations.
 */
public class IndexLifecycleManager extends AbstractComponent {

    private static final String SECURITY_VERSION_STRING = "security-version";
    public static final String TEMPLATE_VERSION_PATTERN =
            Pattern.quote("${security.template.version}");

    private static final int MAX_MIGRATE_ATTEMPTS = 10;

    private final String indexName;
    private final String templateName;
    private final InternalClient client;
    private final IndexDataMigrator migrator;
    private final ClusterService clusterService;
    private final ThreadPool threadPool;

    private final AtomicBoolean templateCreationPending = new AtomicBoolean(false);
    private final AtomicBoolean updateMappingPending = new AtomicBoolean(false);

    private final AtomicReference<UpgradeState> migrateDataState = new AtomicReference<>(UpgradeState.NOT_STARTED);
    private final AtomicInteger migrateDataAttempts = new AtomicInteger(0);

    private volatile boolean templateIsUpToDate;
    private volatile boolean indexExists;
    private volatile boolean indexAvailable;
    private volatile boolean canWriteToIndex;
    private volatile boolean mappingIsUpToDate;
    private volatile Version mappingVersion;

    public enum UpgradeState {
        NOT_STARTED, IN_PROGRESS, COMPLETE, FAILED
    }

    public interface IndexDataMigrator {
        void performUpgrade(@Nullable Version previousVersion, ActionListener<Boolean> listener);
    }

    public static final IndexDataMigrator NULL_MIGRATOR =  (version, listener) -> listener.onResponse(false);

    public IndexLifecycleManager(Settings settings, InternalClient client, ClusterService clusterService, ThreadPool threadPool,
                                 String indexName, String templateName, IndexDataMigrator migrator) {
        super(settings);
        this.client = client;
        this.indexName = indexName;
        this.templateName = templateName;
        this.migrator = migrator;
        this.clusterService = clusterService;
        this.threadPool = threadPool;
    }

    public boolean isTemplateUpToDate() {
        return templateIsUpToDate;
    }

    public boolean isTemplateCreationPending() {
        return templateCreationPending.get();
    }

    public boolean isMappingUpToDate() {
        return mappingIsUpToDate;
    }

    public Version getMappingVersion() {
        return mappingVersion;
    }

    public boolean checkMappingVersion(Predicate<Version> requiredVersion) {
        return this.mappingVersion == null || requiredVersion.test(this.mappingVersion);
    }

    public boolean isMappingUpdatePending() {
        return this.updateMappingPending.get();
    }

    public boolean indexExists() {
        return indexExists;
    }

    public boolean isAvailable() {
        return indexAvailable;
    }

    public boolean isWritable() {
        return canWriteToIndex;
    }

    public UpgradeState getMigrationState() {
        return this.migrateDataState.get();
    }

    public void clusterChanged(ClusterChangedEvent event) {
        final ClusterState state = event.state();
        processClusterState(state);
    }

    private void processClusterState(ClusterState state) {
        assert state != null;
        this.indexExists = resolveConcreteIndex(indexName, state.metaData()) != null;
        this.indexAvailable = checkIndexAvailable(state);
        this.templateIsUpToDate = checkTemplateExistsAndIsUpToDate(state);
        this.mappingIsUpToDate = checkIndexMappingUpToDate(state);
        this.canWriteToIndex = templateIsUpToDate && mappingIsUpToDate;
        this.mappingVersion = oldestIndexMappingVersion(state);

        if (state.nodes().isLocalNodeElectedMaster()) {
            if (templateIsUpToDate == false) {
                updateTemplate();
            }
            if (indexAvailable && mappingIsUpToDate == false) {
                migrateData(state, this::updateMapping);
            }
        }
    }

    private boolean checkIndexAvailable(ClusterState state) {
        final IndexRoutingTable routingTable = getIndexRoutingTable(state);
        if (routingTable != null && routingTable.allPrimaryShardsActive()) {
            return true;
        }
        logger.debug("Security index [{}] is not yet active", indexName);
        return false;
    }


    /**
     * Returns the routing-table for this index, or <code>null</code> if the index does not exist.
     */
    private IndexRoutingTable getIndexRoutingTable(ClusterState clusterState) {
        IndexMetaData metaData = resolveConcreteIndex(indexName, clusterState.metaData());
        if (metaData == null) {
            return null;
        } else {
            return clusterState.routingTable().index(metaData.getIndex());
        }
    }

    private boolean checkTemplateExistsAndIsUpToDate(ClusterState state) {
        return checkTemplateExistsAndVersionMatches(templateName, state, logger,
                Version.CURRENT::equals);
    }

    public static boolean checkTemplateExistsAndVersionMatches(
            String templateName, ClusterState state, Logger logger, Predicate<Version> predicate) {

        IndexTemplateMetaData templateMeta = state.metaData().templates().get(templateName);
        if (templateMeta == null) {
            return false;
        }
        ImmutableOpenMap<String, CompressedXContent> mappings = templateMeta.getMappings();
        // check all mappings contain correct version in _meta
        // we have to parse the source here which is annoying
        for (Object typeMapping : mappings.values().toArray()) {
            CompressedXContent typeMappingXContent = (CompressedXContent) typeMapping;
            try {
                Map<String, Object> typeMappingMap = convertToMap(
                        new BytesArray(typeMappingXContent.uncompressed()), false,
                        XContentType.JSON).v2();
                // should always contain one entry with key = typename
                assert (typeMappingMap.size() == 1);
                String key = typeMappingMap.keySet().iterator().next();
                // get the actual mapping entries
                @SuppressWarnings("unchecked")
                Map<String, Object> mappingMap = (Map<String, Object>) typeMappingMap.get(key);
                if (containsCorrectVersion(mappingMap, predicate) == false) {
                    return false;
                }
            } catch (ElasticsearchParseException e) {
                logger.error(new ParameterizedMessage(
                        "Cannot parse the template [{}]", templateName), e);
                throw new IllegalStateException("Cannot parse the template " + templateName, e);
            }
        }
        return true;
    }

    private static boolean containsCorrectVersion(Map<String, Object> typeMappingMap,
                                                  Predicate<Version> predicate) {
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) typeMappingMap.get("_meta");
        if (meta == null) {
            // pre 5.0, cannot be up to date
            return false;
        }
        return predicate.test(Version.fromString((String) meta.get(SECURITY_VERSION_STRING)));
    }

    private boolean checkIndexMappingUpToDate(ClusterState clusterState) {
        return checkIndexMappingVersionMatches(clusterState, Version.CURRENT::equals);
    }

    private boolean checkIndexMappingVersionMatches(ClusterState clusterState,
                                                    Predicate<Version> predicate) {
        return checkIndexMappingVersionMatches(indexName, clusterState, logger, predicate);
    }

    public static boolean checkIndexMappingVersionMatches(String indexName,
                                                          ClusterState clusterState, Logger logger,
                                                          Predicate<Version> predicate) {
        return loadIndexMappingVersions(indexName, clusterState, logger)
                .stream().allMatch(predicate);
    }

    private Version oldestIndexMappingVersion(ClusterState clusterState) {
        final Set<Version> versions = loadIndexMappingVersions(indexName, clusterState, logger);
        return versions.stream().min(Comparator.comparingInt(v -> v.id)).orElse(null);
    }

    private static Set<Version> loadIndexMappingVersions(String indexName,
                                                         ClusterState clusterState, Logger logger) {
        Set<Version> versions = new HashSet<>();
        IndexMetaData indexMetaData = resolveConcreteIndex(indexName, clusterState.metaData());
        if (indexMetaData != null) {
            for (Object object : indexMetaData.getMappings().values().toArray()) {
                MappingMetaData mappingMetaData = (MappingMetaData) object;
                if (mappingMetaData.type().equals(MapperService.DEFAULT_MAPPING)) {
                    continue;
                }
                versions.add(readMappingVersion(indexName, mappingMetaData, logger));
            }
        }
        return versions;
    }

    /**
     * Resolves a concrete index name or alias to a {@link IndexMetaData} instance.  Requires
     * that if supplied with an alias, the alias resolves to at most one concrete index.
     */
    private static IndexMetaData resolveConcreteIndex(final String indexOrAliasName, final MetaData metaData) {
        final AliasOrIndex aliasOrIndex = metaData.getAliasAndIndexLookup().get(indexOrAliasName);
        if (aliasOrIndex != null) {
            final List<IndexMetaData> indices = aliasOrIndex.getIndices();
            if (aliasOrIndex.isAlias() && indices.size() > 1) {
                throw new IllegalStateException("Alias [" + indexOrAliasName + "] points to more than one index: " +
                        indices.stream().map(imd -> imd.getIndex().getName()).collect(Collectors.toList()));
            }
            return indices.get(0);
        }
        return null;
    }

    private static Version readMappingVersion(String indexName, MappingMetaData mappingMetaData,
                                              Logger logger) {
        try {
            Map<String, Object> meta =
                    (Map<String, Object>) mappingMetaData.sourceAsMap().get("_meta");
            if (meta == null) {
                logger.info("Missing _meta field in mapping [{}] of index [{}]", mappingMetaData.type(), indexName);
                // something pre-5.0, but we don't know what. Use 2.3.0 as a placeholder for "old"
                return Version.V_2_3_0;
            }
            return Version.fromString((String) meta.get(SECURITY_VERSION_STRING));
        } catch (ElasticsearchParseException e) {
            logger.error(new ParameterizedMessage(
                    "Cannot parse the mapping for index [{}]", indexName), e);
            throw new ElasticsearchException(
                    "Cannot parse the mapping for index [{}]", e, indexName);
        }
    }

    private void updateTemplate() {
        // only put the template if this is not already in progress
        if (templateCreationPending.compareAndSet(false, true)) {
            putTemplate();
        }
    }

    private boolean migrateData(ClusterState state, Runnable andThen) {
        // only update the data if this is not already in progress
        if (migrateDataState.compareAndSet(UpgradeState.NOT_STARTED, UpgradeState.IN_PROGRESS)) {
            final Version previousVersion = oldestIndexMappingVersion(state);
            migrator.performUpgrade(previousVersion, new ActionListener<Boolean>() {

                @Override
                public void onResponse(Boolean upgraded) {
                    migrateDataState.set(UpgradeState.COMPLETE);
                    andThen.run();
                }

                @Override
                public void onFailure(Exception e) {
                    migrateDataState.set(UpgradeState.FAILED);
                    final int attempts = migrateDataAttempts.incrementAndGet();
                    logger.error(new ParameterizedMessage(
                                    "failed to upgrade security [{}] data from version [{}] (Attempt {} of {})",
                                    indexName, previousVersion, attempts, MAX_MIGRATE_ATTEMPTS),
                            e);
                    if (attempts < MAX_MIGRATE_ATTEMPTS) {
                        // The first retry is (1^5)ms = 1ms
                        // The last retry is (9^5)ms = 59s
                        final TimeValue retry = TimeValue.timeValueMillis((long) Math.pow(attempts, 5));
                        logger.info("Will attempt upgrade again in {}", retry);
                        threadPool.schedule(retry, ThreadPool.Names.SAME, IndexLifecycleManager.this::retryDataMigration);
                    } else {
                        logger.error("Security migration has failed after {} attempts. Restart the master node to try again.",
                                MAX_MIGRATE_ATTEMPTS);
                    }
                }

                @Override
                public String toString() {
                    return getClass() + "{" + indexName + " migrator}";
                }
            });
            return true;
        } else {
            if (migrateDataState.get() == UpgradeState.COMPLETE) {
                andThen.run();
            }
            return false;
        }
    }

    private void retryDataMigration() {
        if (migrateDataState.compareAndSet(UpgradeState.FAILED, UpgradeState.NOT_STARTED)) {
            processClusterState(clusterService.state());
        }
    }

    private void updateMapping() {
        // only update the mapping if this is not already in progress
        if (updateMappingPending.compareAndSet(false, true)) {
            putMappings();
        }
    }

    private void putMappings() {
        String template = TemplateUtils.loadTemplate("/" + templateName + ".json",
                Version.CURRENT.toString(), TEMPLATE_VERSION_PATTERN);
        Map<String, Object> typeMappingMap;
        try {
            typeMappingMap = convertToMap(JsonXContent.jsonXContent, template, false);
        } catch (ElasticsearchParseException e) {
            updateMappingPending.set(false);
            logger.error(new ParameterizedMessage(
                    "failed to parse index template {}", templateName), e);
            throw new ElasticsearchException("failed to parse index template {}", e, templateName);
        }

        // here go over all types found in the template and update them
        // we need to wait for all types
        final Map<String, PutMappingResponse> updateResults =
                ConcurrentCollections.newConcurrentMap();

        @SuppressWarnings("unchecked")
        Map<String, Object> typeMappings = (Map<String, Object>) typeMappingMap.get("mappings");
        int expectedResults = typeMappings.size();
        for (String type : typeMappings.keySet()) {
            // get the mappings from the template definition
            @SuppressWarnings("unchecked")
            Map<String, Object> typeMapping = (Map<String, Object>) typeMappings.get(type);
            // update the mapping
            putMapping(updateResults, expectedResults, type, typeMapping);
        }
    }

    private void putMapping(final Map<String, PutMappingResponse> updateResults,
                            int expectedResults, final String type,
                            Map<String, Object> typeMapping) {
        logger.debug("updating mapping of the [{}] index for type [{}]", indexName, type);

        PutMappingRequest putMappingRequest = client.admin().indices()
                .preparePutMapping(indexName).setSource(typeMapping).setType(type).request();
        client.admin().indices().putMapping(putMappingRequest,
                new ActionListener<PutMappingResponse>() {
                    @Override
                    public void onResponse(PutMappingResponse putMappingResponse) {
                        if (putMappingResponse.isAcknowledged() == false) {
                            updateMappingPending.set(false);
                            throw new ElasticsearchException("update mapping for type [{}]" +
                                    " in index [{}] was not acknowledged", type, indexName);
                        } else {
                            updateResults.put(type, putMappingResponse);
                            if (updateResults.size() == expectedResults) {
                                updateMappingPending.set(false);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        updateMappingPending.set(false);
                        logger.warn((Supplier<?>) () -> new ParameterizedMessage(
                                "failed to update mapping for type [{}] on index [{}]",
                                type, indexName), e);
                    }

                    @Override
                    public String toString() {
                        return getClass() + "{" + indexName + " PutMapping}";
                    }
                });
    }

    private void putTemplate() {
        logger.debug("putting the template [{}]", templateName);
        String template = TemplateUtils.loadTemplate("/" + templateName + ".json",
                Version.CURRENT.toString(), TEMPLATE_VERSION_PATTERN);

        PutIndexTemplateRequest putTemplateRequest = client.admin().indices()
                .preparePutTemplate(templateName)
                .setSource(
                        new BytesArray(template.getBytes(StandardCharsets.UTF_8)),
                        XContentType.JSON)
                .request();
        client.admin().indices().putTemplate(putTemplateRequest,
                new ActionListener<PutIndexTemplateResponse>() {
                    @Override
                    public void onResponse(PutIndexTemplateResponse putIndexTemplateResponse) {
                        templateCreationPending.set(false);
                        if (putIndexTemplateResponse.isAcknowledged()) {
                            templateIsUpToDate = true;
                        } else {
                            throw new ElasticsearchException(
                                    "put template [{}] was not acknowledged", templateName
                            );
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        templateCreationPending.set(false);
                        logger.warn(new ParameterizedMessage(
                                "failed to put template [{}]", templateName), e);
                    }

                    @Override
                    public String toString() {
                        return getClass() + "{" + indexName + " PutTemplate}";
                    }
                });
    }
}
