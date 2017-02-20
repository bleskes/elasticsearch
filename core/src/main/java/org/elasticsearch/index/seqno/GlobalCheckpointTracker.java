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

package org.elasticsearch.index.seqno;

import com.carrotsearch.hppc.ObjectLongHashMap;
import com.carrotsearch.hppc.ObjectLongMap;
import com.carrotsearch.hppc.cursors.ObjectLongCursor;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.ShardId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.elasticsearch.index.seqno.SequenceNumbersService.UNASSIGNED_SEQ_NO;

/**
 * This class is responsible of tracking the global checkpoint. The global checkpoint is the highest sequence number for which all lower (or
 * equal) sequence number have been processed on all shards that are currently active. Since shards count as "active" when the master starts
 * them, and before this primary shard has been notified of this fact, we also include shards that have completed recovery. These shards
 * have received all old operations via the recovery mechanism and are kept up to date by the various replications actions. The set of
 * shards that are taken into account for the global checkpoint calculation are called the "in-sync shards".
 * <p>
 * The global checkpoint is maintained by the primary shard and is replicated to all the replicas (during normal operations and
 * via {@link GlobalCheckpointSyncAction}).
 */
public class GlobalCheckpointTracker extends AbstractIndexShardComponent {

    /**
     * This map holds the last known local checkpoint for every active shard and initializing shard copies that has been brought up to speed
     * through recovery. These shards are treated as valid copies and participate in determining the global checkpoint. This map is keyed by
     * allocation IDs. All accesses to this set are guarded by a lock on this.
     */
    private final ObjectLongMap<String> inSyncLocalCheckpoints;

    /**
     * This map holds the last known local checkpoint for every initializing shard copy that is still undergoing recovery.
     * These shards <strong>do not</strong> participate in determining the global checkpoint. This map is needed to make sure that when
     * shards are promoted to {@link #inSyncLocalCheckpoints} we use the highest known checkpoint, even if we index concurrently
     * while recovering the shard.
     * Keyed by allocation ids
     */
    private final ObjectLongMap<String> trackingLocalCheckpoint;

    /**
     * The current global checkpoint for this shard. Note that this field is guarded by a lock on this and thus this field does not need to
     * be volatile.
     */
    private long globalCheckpoint;

    /**
     * true if there is a pending in sync shard which waits for it's local checkpoint to advance above the global checkpoint.
     * we delay the global checkpoint from advancing in that case
     */
    final List<String> pendingInSync;

    /**
     * Initialize the global checkpoint service. The specified global checkpoint should be set to the last known global checkpoint, or
     * {@link SequenceNumbersService#UNASSIGNED_SEQ_NO}.
     *
     * @param shardId          the shard ID
     * @param indexSettings    the index settings
     * @param globalCheckpoint the last known global checkpoint for this shard, or {@link SequenceNumbersService#UNASSIGNED_SEQ_NO}
     */
    GlobalCheckpointTracker(final ShardId shardId, final IndexSettings indexSettings, final long globalCheckpoint) {
        super(shardId, indexSettings);
        assert globalCheckpoint >= UNASSIGNED_SEQ_NO : "illegal initial global checkpoint: " + globalCheckpoint;
        inSyncLocalCheckpoints = new ObjectLongHashMap<>(1 + indexSettings.getNumberOfReplicas());
        trackingLocalCheckpoint = new ObjectLongHashMap<>(indexSettings.getNumberOfReplicas());
        pendingInSync = new ArrayList<>();
        this.globalCheckpoint = globalCheckpoint;
    }

    /**
     * Notifies the service to update the local checkpoint for the shard with the provided allocation ID. If the checkpoint is lower than
     * the currently known one, this is a no-op. Last, if the allocation id is not yet known, it is ignored. This is to prevent late
     * arrivals from shards that are removed to be re-added.
     *
     * @param allocationId the allocation ID of the shard to update the local checkpoint for
     * @param checkpoint   the local checkpoint for the shard
     */
    @SuppressForbidden(reason = "need to notify pending in-sync shards")
    public synchronized void updateLocalCheckpoint(final String allocationId, final long checkpoint) {
        final boolean updated;
        if (updateLocalCheckpointInMap(allocationId, checkpoint, inSyncLocalCheckpoints, "inSync")) {
            updated = true;
        } else if (updateLocalCheckpointInMap(allocationId, checkpoint, trackingLocalCheckpoint, "tracking")) {
            updated = true;
        } else {
            logger.trace("[{}] isn't tracked. ignoring local checkpoint of [{}].", allocationId, checkpoint);
            updated = false;
        }
        if (updated) {
            this.notifyAll();
        }
    }

    private boolean updateLocalCheckpointInMap(String allocationId, long localCheckpoint,
                                               ObjectLongMap<String> checkpointsMap, String name) {
        assert Thread.holdsLock(this);
        int indexOfKey = checkpointsMap.indexOf(allocationId);
        if (indexOfKey >= 0) {
            long current = checkpointsMap.indexGet(indexOfKey);
            if (current < localCheckpoint) {
                checkpointsMap.indexReplace(indexOfKey, localCheckpoint);
                if (logger.isTraceEnabled()) {
                    logger.trace("updated local checkpoint of [{}] to [{}] (type [{}])", allocationId, localCheckpoint,
                        name);
                }
            } else {
                logger.trace("skipping update local checkpoint [{}], current check point is higher " +
                        "(current [{}], incoming [{}], type [{}])",
                    allocationId, current, localCheckpoint, name);
            }
            return true;
        } else {
            return false;
        }
    }


    /**
     * Scans through the currently known local checkpoint and updates the global checkpoint accordingly.
     *
     * @return {@code true} if the checkpoint has been updated
     */
    synchronized boolean updateCheckpointOnPrimary() {
        long minCheckpoint = Long.MAX_VALUE;
        if (inSyncLocalCheckpoints.isEmpty() || pendingInSync.isEmpty() == false) {
            return false;
        }
        for (final ObjectLongCursor<String> cp : inSyncLocalCheckpoints) {
            if (cp.value == UNASSIGNED_SEQ_NO) {
                logger.trace("unknown local checkpoint for active allocationId [{}], requesting a sync", cp.key);
                return false;
            }
            minCheckpoint = Math.min(cp.value, minCheckpoint);
        }
        assert minCheckpoint != UNASSIGNED_SEQ_NO : "newly calculated checkpoint can't be unassigned";
        if (minCheckpoint < globalCheckpoint) {
            final String message =
                String.format(Locale.ROOT, "new global checkpoint [%d] is lower than previous one [%d]", minCheckpoint, globalCheckpoint);
            throw new IllegalStateException(message);
        }
        if (globalCheckpoint != minCheckpoint) {
            logger.trace("global checkpoint updated to [{}]", minCheckpoint);
            globalCheckpoint = minCheckpoint;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the global checkpoint for the shard.
     *
     * @return the global checkpoint
     */
    public synchronized long getCheckpoint() {
        return globalCheckpoint;
    }

    /**
     * Updates the global checkpoint on a replica shard after it has been updated by the primary.
     *
     * @param checkpoint the global checkpoint
     */
    synchronized void updateCheckpointOnReplica(final long checkpoint) {
        /*
         * The global checkpoint here is a local knowledge which is updated under the mandate of the primary. It can happen that the primary
         * information is lagging compared to a replica (e.g., if a replica is promoted to primary but has stale info relative to other
         * replica shards). In these cases, the local knowledge of the global checkpoint could be higher than sync from the lagging primary.
         */
        if (this.globalCheckpoint <= checkpoint) {
            this.globalCheckpoint = checkpoint;
            logger.trace("global checkpoint updated from primary to [{}]", checkpoint);
        }
    }

    /**
     * Notifies the service of the current allocation ids in the cluster state. This method trims any shards that have been removed.
     *
     * @param activeAllocationIds       the allocation IDs of the currently active shard copies
     * @param initializingAllocationIds the allocation IDs of the currently initializing shard copies
     */
    public synchronized void updateAllocationIdsFromMaster(final Set<String> activeAllocationIds,
                                                           final Set<String> initializingAllocationIds) {
        inSyncLocalCheckpoints.removeAll(key ->
            activeAllocationIds.contains(key) == false && initializingAllocationIds.contains(key) == false);
        for (String activeId : activeAllocationIds) {
            if (inSyncLocalCheckpoints.containsKey(activeId) == false) {
                long knownCheckpoint = trackingLocalCheckpoint.getOrDefault(activeId, SequenceNumbersService.UNASSIGNED_SEQ_NO);
                inSyncLocalCheckpoints.put(activeId, knownCheckpoint);
                logger.trace("marking [{}] as in sync via cluster state. known checkpoint [{}]", activeId, knownCheckpoint);
            }
        }
        trackingLocalCheckpoint.removeAll(key -> initializingAllocationIds.contains(key) == false);
        // add initializing shards to tracking
        for (String initID : initializingAllocationIds) {
            if (inSyncLocalCheckpoints.containsKey(initID)) {
                continue;
            }
            if (trackingLocalCheckpoint.containsKey(initID)) {
                continue;
            }
            trackingLocalCheckpoint.put(initID, SequenceNumbersService.UNASSIGNED_SEQ_NO);
            logger.trace("added [{}] to the tracking map due to a CS update", initID);

        }
    }

    /**
     * Marks the shard with the provided allocation ID as in-sync with the primary shard. The method will wait
     * until the shard's local checkpoint is above the current global checkpoint.
     *
     * @param allocationId    the allocation ID of the shard to mark as in-sync
     * @param localCheckpoint the local checkpoint of the shard marked in-sync
     */
    @SuppressForbidden(reason = "need to wait for local checkpoint to be above global")
    public synchronized void markAllocationIdAsInSync(final String allocationId, long localCheckpoint) throws InterruptedException {
        if (trackingLocalCheckpoint.containsKey(allocationId) == false) {
            // master has removed this allocation, ignore
            return;
        }
        updateLocalCheckpointInMap(allocationId, localCheckpoint, trackingLocalCheckpoint, "tracking");

        boolean success = false;
        pendingInSync.add(allocationId);
        try {
            do {
                long current = trackingLocalCheckpoint.get(allocationId);
                if (current >= globalCheckpoint) {
                    logger.trace("marked [{}] as in sync with a local checkpoint of [{}]", allocationId, localCheckpoint);
                    trackingLocalCheckpoint.remove(allocationId);
                    inSyncLocalCheckpoints.put(allocationId, current);
                    success = true;
                } else {
                    this.wait(TimeValue.timeValueSeconds(30).millis());
                }
            } while (success == false);
        } finally {
            pendingInSync.remove(allocationId);
        }
    }

    /**
     * Returns the local checkpoint for the shard with the specified allocation ID, or {@link SequenceNumbersService#UNASSIGNED_SEQ_NO} if
     * the shard is not in-sync.
     *
     * @param allocationId the allocation ID of the shard to obtain the local checkpoint for
     * @return the local checkpoint, or {@link SequenceNumbersService#UNASSIGNED_SEQ_NO}
     */
    synchronized long getLocalCheckpointForAllocationId(final String allocationId) {
        if (inSyncLocalCheckpoints.containsKey(allocationId)) {
            return inSyncLocalCheckpoints.get(allocationId);
        }
        return UNASSIGNED_SEQ_NO;
    }

}
