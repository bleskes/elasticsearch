/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ccr.repository;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexFormatTooNewException;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.iterable.Iterables;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.snapshots.IndexShardRestoreFailedException;
import org.elasticsearch.index.snapshots.IndexShardSnapshotStatus;
import org.elasticsearch.index.store.Store;
import org.elasticsearch.index.store.StoreFileMetaData;
import org.elasticsearch.indices.recovery.RecoveryState;
import org.elasticsearch.repositories.IndexId;
import org.elasticsearch.repositories.Repository;
import org.elasticsearch.repositories.RepositoryData;
import org.elasticsearch.snapshots.SnapshotId;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.snapshots.SnapshotShardFailure;
import org.elasticsearch.snapshots.SnapshotState;
import org.elasticsearch.xpack.ccr.repository.TransportCreateRestoreSessionAction.Session;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteClusterRepository extends AbstractLifecycleComponent implements Repository {

    public final static SnapshotId SNAPSHOT_ID = new SnapshotId("_latest_", "_latest_");
    public final static String TYPE = "_remote_cluster_";

    private final RepositoryMetaData metadata;
    private final Client client;

    public RemoteClusterRepository(RepositoryMetaData metadata, Client client, Settings settings) {
        super(settings);
        this.metadata = metadata;
        this.client = client;
    }

    @Override
    public RepositoryMetaData getMetadata() {
        return metadata;
    }

    @Override
    public SnapshotInfo getSnapshotInfo(SnapshotId snapshotId) {
        assert snapshotId.equals(SNAPSHOT_ID);
        ClusterStateResponse response = client.admin().cluster().prepareState().clear().setMetaData(true).get();
        return new SnapshotInfo(snapshotId,
            Arrays.asList(response.getState().metaData().getConcreteAllIndices()),
            SnapshotState.SUCCESS);
    }

    @Override
    public MetaData getSnapshotGlobalMetaData(SnapshotId snapshotId) {
        assert snapshotId.equals(SNAPSHOT_ID);
        ClusterStateResponse response = client.admin().cluster().prepareState().clear().setMetaData(true).get();
        return response.getState().metaData();
    }

    @Override
    public IndexMetaData getSnapshotIndexMetaData(SnapshotId snapshotId, IndexId index) throws IOException {
        assert snapshotId.equals(SNAPSHOT_ID);
        ClusterStateResponse response = client.admin().cluster().prepareState().clear().setMetaData(true).get();
        return response.getState().metaData().index(index.getName());
    }

    @Override
    public RepositoryData getRepositoryData() {
        MetaData metaData = client.admin().cluster().prepareState().clear().setMetaData(true).get().getState().getMetaData();
        return new RepositoryData(1,
            Collections.singletonMap(SNAPSHOT_ID.getName(), SNAPSHOT_ID),
            Collections.singletonMap(SNAPSHOT_ID.getName(), SnapshotState.SUCCESS),
            Arrays.stream(metaData.getConcreteAllIndices()).collect(Collectors.toMap(
                i -> {
                    Index index = metaData.indices().get(i).getIndex();
                    return new IndexId(index.getName(), index.getUUID());
                },
                i -> Collections.singleton(SNAPSHOT_ID))),
            Collections.emptyList()
        );
    }

    @Override
    public void initializeSnapshot(SnapshotId snapshotId, List<IndexId> indices, MetaData metaData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SnapshotInfo finalizeSnapshot(SnapshotId snapshotId, List<IndexId> indices, long startTime, String failure, int totalShards, List<SnapshotShardFailure> shardFailures, long repositoryStateId, boolean includeGlobalState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSnapshot(SnapshotId snapshotId, long repositoryStateId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSnapshotThrottleTimeInNanos() {
        return 0;
    }

    @Override
    public long getRestoreThrottleTimeInNanos() {
        return 0;
    }

    @Override
    public String startVerification() {
        return null;
    }

    @Override
    public void endVerification(String verificationToken) {
    }

    @Override
    public void verify(String verificationToken, DiscoveryNode localNode) {

    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void snapshotShard(IndexShard shard, SnapshotId snapshotId, IndexId indexId, IndexCommit snapshotIndexCommit,
                              IndexShardSnapshotStatus snapshotStatus) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void restoreShard(IndexShard shard, SnapshotId snapshotId, Version version, IndexId indexId, ShardId snapshotShardId,
                             RecoveryState recoveryState) {
        Session session = TransportCreateRestoreSessionAction.createSession(client, snapshotShardId);
        try {
            RestoreContext context = new RestoreContext(shard, session, recoveryState);
            context.restore();
        } catch (IOException e) {
            throw new IndexShardRestoreFailedException(shard.shardId(), e.getMessage(), e);
        } finally {
            TransportCloseSessionAction.closeSession(client, session);
        }

    }

    @Override
    public IndexShardSnapshotStatus getShardSnapshotStatus(SnapshotId snapshotId, Version version, IndexId indexId, ShardId shardId) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() throws IOException {

    }

    /**
     * Context for restore operations
     */
    private class RestoreContext {

        private final int BUFFER_SIZE = 1024 * 100;

        private final IndexShard targetShard;

        private final RecoveryState recoveryState;
        private final Session remoteSession;

        /**
         * Constructs new restore context
         *
         * @param shard         shard to restore into
         * @param remoteSession restore session on remote node
         * @param recoveryState recovery state to report progress
         */
        RestoreContext(IndexShard shard, Session remoteSession, RecoveryState recoveryState) {
            this.recoveryState = recoveryState;
            this.targetShard = shard;
            this.remoteSession = remoteSession;
        }

        /**
         * Performs restore operation
         */
        public void restore() throws IOException {
            final Store store = targetShard.store();
            store.incRef();
            try {
                final ShardId shardId = targetShard.shardId();
                logger.debug("[{}] restoring to [{}] ...", metadata.name(), shardId);

                final Store.MetadataSnapshot sourceMetaData = remoteSession.getMetadata();

                Store.MetadataSnapshot recoveryTargetMetadata;
                try {
                    // this will throw an IOException if the store has no segments infos file. The
                    // store can still have existing files but they will be deleted just before being
                    // restored.
                    recoveryTargetMetadata = targetShard.snapshotStoreMetadata();
                } catch (IndexNotFoundException e) {
                    // happens when restore to an empty shard, not a big deal
                    logger.trace("[{}] [{}] restoring from to an empty shard", shardId, SNAPSHOT_ID);
                    recoveryTargetMetadata = Store.MetadataSnapshot.EMPTY;
                } catch (IOException e) {
                    logger.warn(() -> new ParameterizedMessage("{} Can't read metadata from store, will not reuse any local file while restoring", shardId), e);
                    recoveryTargetMetadata = Store.MetadataSnapshot.EMPTY;
                }

                final StoreFileMetaData restoredSegmentsFile = sourceMetaData.getSegmentsFile();
                if (restoredSegmentsFile == null) {
                    throw new IndexShardRestoreFailedException(shardId, "Snapshot has no segments file");
                }

                final Store.RecoveryDiff diff = sourceMetaData.recoveryDiff(recoveryTargetMetadata);
                for (StoreFileMetaData md : diff.identical) {
                    recoveryState.getIndex().addFileDetail(md.name(), md.length(), true);
                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] not_recovering [{}] from [{}], exists in local store and is same", shardId, md.name());
                    }
                }

                List<StoreFileMetaData> filesToRecover = new ArrayList<>();
                for (StoreFileMetaData md : Iterables.concat(diff.different, diff.missing)) {
                    filesToRecover.add(md);
                    recoveryState.getIndex().addFileDetail(md.name(), md.length(), false);
                    if (logger.isTraceEnabled()) {
                        logger.trace("[{}] recovering [{}], exists in local store but is different", shardId, md.name());
                    }
                }

                if (filesToRecover.isEmpty()) {
                    logger.trace("no files to recover, all exists within the local store");
                }

                try {
                    // list of all existing store files
                    final List<String> deleteIfExistFiles = Arrays.asList(store.directory().listAll());

                    // restore the files from the snapshot to the Lucene store
                    for (final StoreFileMetaData fileToRecover : filesToRecover) {
                        // if a file with a same physical name already exist in the store we need to delete it
                        // before restoring it from the snapshot. We could be lenient and try to reuse the existing
                        // store files (and compare their names/length/checksum again with the snapshot files) but to
                        // avoid extra complexity we simply delete them and restore them again like StoreRecovery
                        // does with dangling indices. Any existing store file that is not restored from the snapshot
                        // will be clean up by RecoveryTarget.cleanFiles().
                        final String name = fileToRecover.name();
                        if (deleteIfExistFiles.contains(name)) {
                            logger.trace("[{}] deleting pre-existing file [{}]", shardId, name);
                            store.directory().deleteFile(name);
                        }

                        logger.trace("[{}] restoring file [{}]", shardId, fileToRecover.name());
                        restoreFile(fileToRecover, store);
                    }
                } catch (IOException ex) {
                    throw new IndexShardRestoreFailedException(shardId, "Failed to recover index", ex);
                }

                // read the snapshot data persisted
                final SegmentInfos segmentCommitInfos;
                try {
                    segmentCommitInfos = Lucene.pruneUnreferencedFiles(restoredSegmentsFile.name(), store.directory());
                } catch (IOException e) {
                    throw new IndexShardRestoreFailedException(shardId, "Failed to fetch index version after copying it over", e);
                }
                recoveryState.getIndex().updateVersion(segmentCommitInfos.getVersion());

                /// now, go over and clean files that are in the store, but were not in the snapshot
                store.cleanupAndVerify("restore complete from remote", sourceMetaData);
            } finally {
                store.decRef();
            }
        }

        /**
         * Restores a file
         * This is asynchronous method. Upon completion of the operation latch is getting counted down and any failures are
         * added to the {@code failures} list
         *
         * @param fileInfo file to be restored
         */
        private void restoreFile(final StoreFileMetaData fileInfo, final Store store) throws IOException {
            boolean success = false;

            // TODO add rate limitting;
            try (InputStream stream = new SessionFileInputStream(remoteSession, fileInfo)) {
                try (IndexOutput indexOutput = store.createVerifyingOutput(fileInfo.name(), fileInfo, IOContext.DEFAULT)) {
                    final byte[] buffer = new byte[BUFFER_SIZE];
                    int length;
                    while ((length = stream.read(buffer)) > 0) {
                        indexOutput.writeBytes(buffer, 0, length);
                        recoveryState.getIndex().addRecoveredBytesToFile(fileInfo.name(), length);
                    }
                    Store.verify(indexOutput);
                    indexOutput.close();
                    store.directory().sync(Collections.singleton(fileInfo.name()));
                    success = true;
                } catch (CorruptIndexException | IndexFormatTooOldException | IndexFormatTooNewException ex) {
                    try {
                        store.markStoreCorrupted(ex);
                    } catch (IOException e) {
                        logger.warn("store cannot be marked as corrupted", e);
                    }
                    throw ex;
                } finally {
                    if (success == false) {
                        store.deleteQuiet(fileInfo.name());
                    }
                }
            }
        }
    }

    private class SessionFileInputStream extends InputStream {

        final Session session;
        final StoreFileMetaData fileInfo;

        long pos = 0;


        private SessionFileInputStream(Session session, StoreFileMetaData fileInfo) {
            this.session = session;
            this.fileInfo = fileInfo;
        }


        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (pos >= fileInfo.length()) {
                return 0;
            }
            byte[] remote = TransportFetchFileChunkAction.readBytesFromFile(client, session.getNodeId(), session.getSessionUUID(),
                fileInfo.name(), pos, (int)Math.min(fileInfo.length() - pos, len));
            System.arraycopy(remote, 0, bytes, off, remote.length);
            return remote.length;
        }
    }
}
