/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.index.engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.Similarity;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.index.codec.CodecService;
import org.elasticsearch.index.deletionpolicy.SnapshotDeletionPolicy;
import org.elasticsearch.index.indexing.ShardIndexingService;
import org.elasticsearch.index.merge.policy.MergePolicyProvider;
import org.elasticsearch.index.merge.scheduler.MergeSchedulerProvider;
import org.elasticsearch.index.settings.IndexSettingsService;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.store.Store;
import org.elasticsearch.index.translog.Translog;
import org.elasticsearch.indices.IndicesWarmer;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.concurrent.TimeUnit;

/*
 * Holds all the configuration that is used to create an {@link Engine}.
 * Once {@link Engine} has been created with this object, changes to this
 * object will affect the {@link Engine} instance.
 */
public final class EngineConfig {
    private final ShardId shardId;
    private volatile boolean failOnMergeFailure = true;
    private volatile boolean failEngineOnCorruption = true;
    private volatile ByteSizeValue indexingBufferSize;
    private volatile int indexConcurrency = IndexWriterConfig.DEFAULT_MAX_THREAD_STATES;
    private volatile boolean compoundOnFlush = true;
    private long gcDeletesInMillis = DEFAULT_GC_DELETES.millis();
    private volatile boolean enableGcDeletes = true;
    private volatile String codecName = DEFAULT_CODEC_NAME;
    private volatile boolean checksumOnMerge;
    private final ThreadPool threadPool;
    private final ShardIndexingService indexingService;
    private final IndexSettingsService indexSettingsService;
    @Nullable
    private final IndicesWarmer warmer;
    private final Store store;
    private final SnapshotDeletionPolicy deletionPolicy;
    private final Translog translog;
    private final MergePolicyProvider mergePolicyProvider;
    private final MergeSchedulerProvider mergeScheduler;
    private final Analyzer analyzer;
    private final Similarity similarity;
    private final CodecService codecService;
    private final Engine.FailedEngineListener failedEngineListener;

    /**
     * Index setting for index concurrency / number of threadstates in the indexwriter.
     * The default is depending on the number of CPUs in the system. We use a 0.65 the number of CPUs or at least {@value org.apache.lucene.index.IndexWriterConfig#DEFAULT_MAX_THREAD_STATES}
     * This setting is realtime updateable
     */
    public static final String INDEX_CONCURRENCY_SETTING = "index.index_concurrency";

    /**
     * Index setting for compound file on flush. This setting is realtime updateable.
     */
    public static final String INDEX_COMPOUND_ON_FLUSH = "index.compound_on_flush";

    /**
     * Index setting to enable / disable deletes garbage collection.
     * This setting is realtime updateable
     */
    public static final String INDEX_GC_DELETES_SETTING = "index.gc_deletes";

    /**
     * Index setting to enable / disable engine failures on merge exceptions. Default is <code>true</code> / <tt>enabled</tt>.
     * This setting is realtime updateable.
     */
    public static final String INDEX_FAIL_ON_MERGE_FAILURE_SETTING = "index.fail_on_merge_failure";

    /**
     * Index setting to enable / disable engine failures on detected index corruptions. Default is <code>true</code> / <tt>enabled</tt>.
     * This setting is realtime updateable.
     */
    public static final String INDEX_FAIL_ON_CORRUPTION_SETTING = "index.fail_on_corruption";

    /**
     * Index setting to control the initial index buffer size.
     * This setting is <b>not</b> realtime updateable.
     */
    public static final String INDEX_BUFFER_SIZE_SETTING = "index.buffer_size";

    /**
     * Index setting to change the low level lucene codec used for writing new segments.
     * This setting is realtime updateable.
     */
    public static final String INDEX_CODEC_SETTING = "index.codec";


    /**
     * Index setting to enable / disable checksum checks on merge
     * This setting is realtime updateable.
     */
    public static final String INDEX_CHECKSUM_ON_MERGE = "index.checksum_on_merge";


    public static final TimeValue DEFAULT_REFRESH_INTERVAL = new TimeValue(1, TimeUnit.SECONDS);
    public static final TimeValue DEFAULT_GC_DELETES = TimeValue.timeValueSeconds(60);
    public static final ByteSizeValue DEFAUTL_INDEX_BUFFER_SIZE = new ByteSizeValue(64, ByteSizeUnit.MB);
    public static final ByteSizeValue INACTIVE_SHARD_INDEXING_BUFFER = ByteSizeValue.parseBytesSizeValue("500kb");

    private static final String DEFAULT_CODEC_NAME = "default";


    /**
     * Creates a new {@link org.elasticsearch.index.engine.EngineConfig}
     */
    public EngineConfig(ShardId shardId, ThreadPool threadPool, ShardIndexingService indexingService, IndexSettingsService indexSettingsService, IndicesWarmer warmer, Store store, SnapshotDeletionPolicy deletionPolicy, Translog translog, MergePolicyProvider mergePolicyProvider, MergeSchedulerProvider mergeScheduler, Analyzer analyzer, Similarity similarity, CodecService codecService, Engine.FailedEngineListener failedEngineListener) {
        this.shardId = shardId;
        this.threadPool = threadPool;
        this.indexingService = indexingService;
        this.indexSettingsService = indexSettingsService;
        this.warmer = warmer;
        this.store = store;
        this.deletionPolicy = deletionPolicy;
        this.translog = translog;
        this.mergePolicyProvider = mergePolicyProvider;
        this.mergeScheduler = mergeScheduler;
        this.analyzer = analyzer;
        this.similarity = similarity;
        this.codecService = codecService;
        this.failedEngineListener = failedEngineListener;
        Settings indexSettings = indexSettingsService.getSettings();
        this.compoundOnFlush = indexSettings.getAsBoolean(EngineConfig.INDEX_COMPOUND_ON_FLUSH, compoundOnFlush);
        this.indexConcurrency = indexSettings.getAsInt(EngineConfig.INDEX_CONCURRENCY_SETTING, Math.max(IndexWriterConfig.DEFAULT_MAX_THREAD_STATES, (int) (EsExecutors.boundedNumberOfProcessors(indexSettings) * 0.65)));
        codecName = indexSettings.get(EngineConfig.INDEX_CODEC_SETTING, EngineConfig.DEFAULT_CODEC_NAME);
        indexingBufferSize = indexSettings.getAsBytesSize(INDEX_BUFFER_SIZE_SETTING, DEFAUTL_INDEX_BUFFER_SIZE);
        failEngineOnCorruption = indexSettings.getAsBoolean(INDEX_FAIL_ON_CORRUPTION_SETTING, true);
        failOnMergeFailure = indexSettings.getAsBoolean(INDEX_FAIL_ON_MERGE_FAILURE_SETTING, true);
        gcDeletesInMillis = indexSettings.getAsTime(INDEX_GC_DELETES_SETTING, EngineConfig.DEFAULT_GC_DELETES).millis();
    }

    /**
     * Sets the indexing buffer
     */
    public void setIndexingBufferSize(ByteSizeValue indexingBufferSize) {
        this.indexingBufferSize = indexingBufferSize;
    }

    /**
     * Sets the index concurrency
     * @see #getIndexConcurrency()
     */
    public void setIndexConcurrency(int indexConcurrency) {
        this.indexConcurrency = indexConcurrency;
    }


    /**
     * Enables / disables gc deletes
     *
     * @see #isEnableGcDeletes()
     */
    public void setEnableGcDeletes(boolean enableGcDeletes) {
        this.enableGcDeletes = enableGcDeletes;
    }

    /**
     * Returns <code>true</code> iff the engine should be failed if a merge error is hit. Defaults to <code>true</code>
     */
    public boolean isFailOnMergeFailure() {
        return failOnMergeFailure;
    }

    /**
     * Returns <code>true</code> if the engine should be failed in the case of a corrupted index. Defaults to <code>true</code>
     */
    public boolean isFailEngineOnCorruption() {
        return failEngineOnCorruption;
    }

    /**
     * Returns the initial index buffer size. This setting is only read on startup and otherwise controlled by {@link org.elasticsearch.indices.memory.IndexingMemoryController}
     */
    public ByteSizeValue getIndexingBufferSize() {
        return indexingBufferSize;
    }

    /**
     * Returns the index concurrency that directly translates into the number of thread states used in the engines
     * {@code IndexWriter}.
     *
     * @see org.apache.lucene.index.IndexWriterConfig#getMaxThreadStates()
     */
    public int getIndexConcurrency() {
        return indexConcurrency;
    }

    /**
     * Returns <code>true</code> iff flushed segments should be written as compound file system. Defaults to <code>true</code>
     */
    public boolean isCompoundOnFlush() {
        return compoundOnFlush;
    }

    /**
     * Returns the GC deletes cycle in milliseconds.
     */
    public long getGcDeletesInMillis() {
        return gcDeletesInMillis;
    }

    /**
     * Returns <code>true</code> iff delete garbage collection in the engine should be enabled. This setting is updateable
     * in realtime and forces a volatile read. Consumers can safely read this value directly go fetch it's latest value. The default is <code>true</code>
     * <p>
     *     Engine GC deletion if enabled collects deleted documents from in-memory realtime data structures after a certain amount of
     *     time ({@link #getGcDeletesInMillis()} if enabled. Before deletes are GCed they will cause re-adding the document that was deleted
     *     to fail.
     * </p>
     */
    public boolean isEnableGcDeletes() {
        return enableGcDeletes;
    }

    /**
     * Returns the {@link Codec} used in the engines {@link org.apache.lucene.index.IndexWriter}
     * <p>
     *     Note: this settings is only read on startup and if a new writer is created. This happens either due to a
     *     settings change in the {@link org.elasticsearch.index.engine.EngineConfig.EngineSettingsListener} or if
     *     {@link Engine#flush(org.elasticsearch.index.engine.Engine.FlushType, boolean, boolean)} with {@link org.elasticsearch.index.engine.Engine.FlushType#NEW_WRITER} is executed.
     * </p>
     */
    public Codec getCodec() {
        return codecService.codec(codecName);
    }

    /**
     * Returns a thread-pool mainly used to get estimated time stamps from {@link org.elasticsearch.threadpool.ThreadPool#estimatedTimeInMillis()} and to schedule
     * async force merge calls on the {@link org.elasticsearch.threadpool.ThreadPool.Names#OPTIMIZE} thread-pool
     */
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Returns a {@link org.elasticsearch.index.indexing.ShardIndexingService} used inside the engine to inform about
     * pre and post index and create operations. The operations are used for statistic purposes etc.
     *
     * @see org.elasticsearch.index.indexing.ShardIndexingService#postCreate(org.elasticsearch.index.engine.Engine.Create)
     * @see org.elasticsearch.index.indexing.ShardIndexingService#preCreate(org.elasticsearch.index.engine.Engine.Create)
     *
     */
    public ShardIndexingService getIndexingService() {
        return indexingService;
    }

    /**
     * Returns an {@link org.elasticsearch.index.settings.IndexSettingsService} used to register a {@link org.elasticsearch.index.engine.EngineConfig.EngineSettingsListener} instance
     * in order to get notification for realtime changeable settings exposed in this {@link org.elasticsearch.index.engine.EngineConfig}.
     */
    public IndexSettingsService getIndexSettingsService() {
        return indexSettingsService;
    }

    /**
     * Returns an {@link org.elasticsearch.indices.IndicesWarmer} used to warm new searchers before they are used for searching.
     * Note: This method might retrun <code>null</code>
     */
    @Nullable
    public IndicesWarmer getWarmer() {
        return warmer;
    }

    /**
     * Returns the {@link org.elasticsearch.index.store.Store} instance that provides access to the {@link org.apache.lucene.store.Directory}
     * used for the engines {@link org.apache.lucene.index.IndexWriter} to write it's index files to.
     * <p>
     * Note: In order to use this instance the consumer needs to increment the stores reference before it's used the first time and hold
     * it's reference until it's not needed anymore.
     * </p>
     */
    public Store getStore() {
        return store;
    }

    /**
     * Returns a {@link org.elasticsearch.index.deletionpolicy.SnapshotDeletionPolicy} used in the engines
     * {@link org.apache.lucene.index.IndexWriter}.
     */
    public SnapshotDeletionPolicy getDeletionPolicy() {
        return deletionPolicy;
    }

    /**
     * Returns a {@link Translog instance}
     */
    public Translog getTranslog() {
        return translog;
    }

    /**
     * Returns the {@link org.elasticsearch.index.merge.policy.MergePolicyProvider} used to obtain
     * a {@link org.apache.lucene.index.MergePolicy} for the engines {@link org.apache.lucene.index.IndexWriter}
     */
    public MergePolicyProvider getMergePolicyProvider() {
        return mergePolicyProvider;
    }

    /**
     * Returns the {@link org.elasticsearch.index.merge.scheduler.MergeSchedulerProvider} used to obtain
     * a {@link org.apache.lucene.index.MergeScheduler} for the engines {@link org.apache.lucene.index.IndexWriter}
     */
    public MergeSchedulerProvider getMergeScheduler() {
        return mergeScheduler;
    }

    /**
     * Returns a listener that should be called on engine failure
     */
    public Engine.FailedEngineListener getFailedEngineListener() {
        return failedEngineListener;
    }

    /**
     * Returns the latest index settings directly from the index settings service.
     */
    public Settings getIndexSettings() {
        return indexSettingsService.getSettings();
    }

    /**
     * Returns the engines shard ID
     */
    public ShardId getShardId() { return shardId; }

    /**
     * Returns the analyzer as the default analyzer in the engines {@link org.apache.lucene.index.IndexWriter}
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Returns the {@link org.apache.lucene.search.similarities.Similarity} used for indexing and searching.
     */
    public Similarity getSimilarity() {
        return similarity;
    }

    public boolean isChecksumOnMerge() {
        return checksumOnMerge;
    }

    /**
     * Basic realtime updateable settings listener that can be used ot receive notification
     * if an index setting changed.
     */
    public static abstract class EngineSettingsListener implements IndexSettingsService.Listener {

        private final ESLogger logger;
        private final EngineConfig config;

        public EngineSettingsListener(ESLogger logger, EngineConfig config) {
            this.logger = logger;
            this.config = config;
        }

        @Override
        public final void onRefreshSettings(Settings settings) {
            boolean change = false;
            long gcDeletesInMillis = settings.getAsTime(EngineConfig.INDEX_GC_DELETES_SETTING, TimeValue.timeValueMillis(config.getGcDeletesInMillis())).millis();
            if (gcDeletesInMillis != config.getGcDeletesInMillis()) {
                logger.info("updating {} from [{}] to [{}]", EngineConfig.INDEX_GC_DELETES_SETTING, TimeValue.timeValueMillis(config.getGcDeletesInMillis()), TimeValue.timeValueMillis(gcDeletesInMillis));
                config.gcDeletesInMillis = gcDeletesInMillis;
                change = true;
            }

            final boolean compoundOnFlush = settings.getAsBoolean(EngineConfig.INDEX_COMPOUND_ON_FLUSH, config.isCompoundOnFlush());
            if (compoundOnFlush != config.isCompoundOnFlush()) {
                logger.info("updating {} from [{}] to [{}]", EngineConfig.INDEX_COMPOUND_ON_FLUSH, config.isCompoundOnFlush(), compoundOnFlush);
                config.compoundOnFlush = compoundOnFlush;
                change = true;
            }

            final boolean failEngineOnCorruption = settings.getAsBoolean(EngineConfig.INDEX_FAIL_ON_CORRUPTION_SETTING, config.isFailEngineOnCorruption());
            if (failEngineOnCorruption != config.isFailEngineOnCorruption()) {
                logger.info("updating {} from [{}] to [{}]", EngineConfig.INDEX_FAIL_ON_CORRUPTION_SETTING, config.isFailEngineOnCorruption(), failEngineOnCorruption);
                config.failEngineOnCorruption = failEngineOnCorruption;
                change = true;
            }
            int indexConcurrency = settings.getAsInt(EngineConfig.INDEX_CONCURRENCY_SETTING, config.getIndexConcurrency());
            if (indexConcurrency != config.getIndexConcurrency()) {
                logger.info("updating index.index_concurrency from [{}] to [{}]", config.getIndexConcurrency(), indexConcurrency);
                config.setIndexConcurrency(indexConcurrency);
                // we have to flush in this case, since it only applies on a new index writer
                change = true;
            }
            final String codecName = settings.get(EngineConfig.INDEX_CODEC_SETTING, config.codecName);
            if (!codecName.equals(config.codecName)) {
                logger.info("updating {} from [{}] to [{}]", EngineConfig.INDEX_CODEC_SETTING, config.codecName, codecName);
                config.codecName = codecName;
                // we want to flush in this case, so the new codec will be reflected right away...
                change = true;
            }
            final boolean failOnMergeFailure = settings.getAsBoolean(EngineConfig.INDEX_FAIL_ON_MERGE_FAILURE_SETTING, config.isFailOnMergeFailure());
            if (failOnMergeFailure != config.isFailOnMergeFailure()) {
                logger.info("updating {} from [{}] to [{}]", EngineConfig.INDEX_FAIL_ON_MERGE_FAILURE_SETTING, config.isFailOnMergeFailure(), failOnMergeFailure);
                config.failOnMergeFailure = failOnMergeFailure;
                change = true;
            }
            final boolean checksumOnMerge = settings.getAsBoolean(INDEX_CHECKSUM_ON_MERGE, config.checksumOnMerge);
            if (checksumOnMerge != config.isChecksumOnMerge()) {
                logger.info("updating {} from [{}] to [{}]", INDEX_CHECKSUM_ON_MERGE, config.checksumOnMerge, checksumOnMerge);
                config.checksumOnMerge = checksumOnMerge;
                change = true;
            }

            if (change) {
               onChange();
            }
        }

        /**
         * This method is called if  any of the settings that are exposed as realtime updateble settings has changed.
         * This method should be overwritten by subclasses to react on settings changes.
         */
        protected abstract void onChange();
    }
}
