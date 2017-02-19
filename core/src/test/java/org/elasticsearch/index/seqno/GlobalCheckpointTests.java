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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.IndexSettingsModule;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.elasticsearch.index.seqno.SequenceNumbersService.UNASSIGNED_SEQ_NO;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;

public class GlobalCheckpointTests extends ESTestCase {

    GlobalCheckpointTracker tracker;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        tracker =
            new GlobalCheckpointTracker(
                new ShardId("test", "_na_", 0),
                IndexSettingsModule.newIndexSettings("test", Settings.EMPTY),
                UNASSIGNED_SEQ_NO);
    }

    public void testEmptyShards() {
        tracker.updateCheckpointOnPrimary();
        assertThat("checkpoint shouldn't be updated when the are no active shards", tracker.getCheckpoint(), equalTo(UNASSIGNED_SEQ_NO));
    }

    private final AtomicInteger aIdGenerator = new AtomicInteger();

    private Map<String, Long> randomAllocationsWithLocalCheckpoints(int min, int max) {
        Map<String, Long> allocations = new HashMap<>();
        for (int i = randomIntBetween(min, max); i > 0; i--) {
            allocations.put("id_" + aIdGenerator.incrementAndGet(), (long) randomInt(1000));
        }
        return allocations;
    }

    public void testGlobalCheckpointUpdate() {
        Map<String, Long> allocations = new HashMap<>();
        Map<String, Long> activeWithCheckpoints = randomAllocationsWithLocalCheckpoints(1, 5);
        Set<String> active = new HashSet<>(activeWithCheckpoints.keySet());
        allocations.putAll(activeWithCheckpoints);
        Map<String, Long> initializingWithCheckpoints = randomAllocationsWithLocalCheckpoints(0, 5);
        Set<String> initializing = new HashSet<>(initializingWithCheckpoints.keySet());
        allocations.putAll(initializingWithCheckpoints);
        assertThat(allocations.size(), equalTo(active.size() + initializing.size()));

        // note: allocations can never be empty in practice as we always have at least one primary shard active/in sync
        // it is however nice not to assume this on this level and check we do the right thing.
        long maxLocalCheckpoint = allocations.values().stream().min(Long::compare).orElse(UNASSIGNED_SEQ_NO);

        assertThat(tracker.getCheckpoint(), equalTo(UNASSIGNED_SEQ_NO));

        logger.info("--> using allocations");
        allocations.keySet().forEach(aId -> {
            final String type;
            if (active.contains(aId)) {
                type = "active";
            } else if (initializing.contains(aId)) {
                type = "init";
            } else {
                throw new IllegalStateException(aId + " not found in any map");
            }
            logger.info("  - [{}], local checkpoint [{}], [{}]", aId, allocations.get(aId), type);
        });

        tracker.updateAllocationIdsFromMaster(active, initializing);
        initializing.forEach(aId -> tracker.markAllocationIdAsInSync(aId, allocations.get(aId)));
        active.forEach(aId -> tracker.updateLocalCheckpoint(aId, allocations.get(aId)));


        assertThat(tracker.getCheckpoint(), equalTo(UNASSIGNED_SEQ_NO));

        tracker.updateCheckpointOnPrimary();
        assertThat(tracker.getCheckpoint(), equalTo(maxLocalCheckpoint));

        // increment checkpoints
        active.forEach(aId -> allocations.put(aId, allocations.get(aId) + 1 + randomInt(4)));
        initializing.forEach(aId -> allocations.put(aId, allocations.get(aId) + 1 + randomInt(4)));
        allocations.keySet().forEach(aId -> tracker.updateLocalCheckpoint(aId, allocations.get(aId)));

        // now insert an unknown active/insync id , the checkpoint shouldn't change but a refresh should be requested.
        final String extraId = "extra_" + randomAsciiOfLength(5);

        // first check that adding it without the master blessing doesn't change anything.
        tracker.updateLocalCheckpoint(extraId, maxLocalCheckpoint + 1 + randomInt(4));
        assertThat(tracker.getLocalCheckpointForAllocationId(extraId), equalTo(UNASSIGNED_SEQ_NO));

        Set<String> newIntializing = new HashSet<>(initializing);
        newIntializing.add(extraId);
        tracker.updateAllocationIdsFromMaster(active, newIntializing);

        // initializing shards don't prevent updates
        tracker.updateCheckpointOnPrimary();
        assertThat(tracker.getCheckpoint(), greaterThan(maxLocalCheckpoint));

        // now notify for the new id
        maxLocalCheckpoint = allocations.values().stream().min(Long::compare).orElse(UNASSIGNED_SEQ_NO);
        allocations.put(extraId, maxLocalCheckpoint);
        tracker.markAllocationIdAsInSync(extraId, maxLocalCheckpoint);

        // increment checkpoints for active shards
        Stream.concat(active.stream(), initializing.stream())
            .forEach(aId -> allocations.put(aId, allocations.get(aId) + 1 + randomInt(4)));
        allocations.forEach(tracker::updateLocalCheckpoint);
        assertFalse(tracker.updateCheckpointOnPrimary());
        // checkpoint shouldn't be incremented previous maxLocalCheckpoint, as the extra id is in sync
        assertThat(tracker.getCheckpoint(), equalTo(maxLocalCheckpoint));

        allocations.put(extraId, maxLocalCheckpoint + randomIntBetween(1, 4));
        tracker.updateLocalCheckpoint(extraId, allocations.get(extraId));
        // now it should be incremented
        assertTrue(tracker.updateCheckpointOnPrimary());
        maxLocalCheckpoint = allocations.values().stream().min(Long::compare).orElse(UNASSIGNED_SEQ_NO);
        maxLocalCheckpoint = Math.min(maxLocalCheckpoint, allocations.get(extraId));
        assertThat(tracker.getCheckpoint(), equalTo(maxLocalCheckpoint));
    }

    public void testMissingActiveIdsPreventAdvance() {
        final Map<String, Long> active = randomAllocationsWithLocalCheckpoints(1, 5);
        final Map<String, Long> initializing = randomAllocationsWithLocalCheckpoints(0, 5);
        final Map<String, Long> assigned = new HashMap<>();
        assigned.putAll(active);
        assigned.putAll(initializing);
        tracker.updateAllocationIdsFromMaster(
            new HashSet<>(randomSubsetOf(randomInt(active.size() - 1), active.keySet())),
            initializing.keySet());
        randomSubsetOf(initializing.keySet()).forEach((aId) -> tracker.markAllocationIdAsInSync(aId, initializing.get(aId)));
        assigned.forEach(tracker::updateLocalCheckpoint);

        // now mark all active shards
        tracker.updateAllocationIdsFromMaster(active.keySet(), initializing.keySet());

        // global checkpoint can't be advanced
        assertFalse(tracker.updateCheckpointOnPrimary());
        assertThat(tracker.getCheckpoint(), equalTo(UNASSIGNED_SEQ_NO));

        // update again
        assigned.forEach(tracker::updateLocalCheckpoint);
        assertTrue(tracker.updateCheckpointOnPrimary());
        assertThat(tracker.getCheckpoint(), not(equalTo(UNASSIGNED_SEQ_NO)));
    }

// TODO: transfer in sync aid or cancel recoveries on primary relocation?
//    public void testMissingInSyncIdsPreventAdvance() {
//        final Map<String, Long> active = randomAllocationsWithLocalCheckpoints(0, 5);
//        final Map<String, Long> initializing = randomAllocationsWithLocalCheckpoints(1, 5);
//        tracker.updateAllocationIdsFromMaster(active.keySet(), initializing.keySet());
//        initializing.keySet().forEach(tracker::markAllocationIdAsInSync);
//        randomSubsetOf(randomInt(initializing.size() - 1),
//            initializing.keySet()).forEach(aId -> tracker.updateLocalCheckpoint(aId, initializing.get(aId)));
//
//        active.forEach(tracker::updateLocalCheckpoint);
//
//        // global checkpoint can't be advanced, but we need a sync
//        assertTrue(tracker.updateCheckpointOnPrimary());
//        assertThat(tracker.getCheckpoint(), equalTo(UNASSIGNED_SEQ_NO));
//
//        // update again
//        initializing.forEach(tracker::updateLocalCheckpoint);
//        assertTrue(tracker.updateCheckpointOnPrimary());
//        assertThat(tracker.getCheckpoint(), not(equalTo(UNASSIGNED_SEQ_NO)));
//    }

    public void testInSyncIdsAreIgnoredIfNotValidatedByMaster() {
        final Map<String, Long> active = randomAllocationsWithLocalCheckpoints(1, 5);
        final Map<String, Long> initializing = randomAllocationsWithLocalCheckpoints(1, 5);
        final Map<String, Long> nonApproved = randomAllocationsWithLocalCheckpoints(1, 5);
        tracker.updateAllocationIdsFromMaster(active.keySet(), initializing.keySet());
        initializing.forEach(tracker::markAllocationIdAsInSync);
        nonApproved.forEach(tracker::markAllocationIdAsInSync);

        List<Map<String, Long>> allocations = Arrays.asList(active, initializing, nonApproved);
        Collections.shuffle(allocations, random());
        allocations.forEach(a -> a.forEach(tracker::updateLocalCheckpoint));

        // global checkpoint can be advanced
        assertTrue(tracker.updateCheckpointOnPrimary());
        assertThat(tracker.getCheckpoint(), not(equalTo(UNASSIGNED_SEQ_NO)));
    }

    public void testInSyncIdsAreRemovedIfNotValidatedByMaster() {
        final Map<String, Long> activeToStay = randomAllocationsWithLocalCheckpoints(1, 5);
        final Map<String, Long> initializingToStay = randomAllocationsWithLocalCheckpoints(1, 5);
        final Map<String, Long> activeToBeRemoved = randomAllocationsWithLocalCheckpoints(1, 5);
        final Map<String, Long> initializingToBeRemoved = randomAllocationsWithLocalCheckpoints(1, 5);
        final Set<String> active = Sets.union(activeToStay.keySet(), activeToBeRemoved.keySet());
        final Set<String> initializing = Sets.union(initializingToStay.keySet(), initializingToBeRemoved.keySet());
        final Map<String, Long> allocations = new HashMap<>();
        allocations.putAll(activeToStay);
        if (randomBoolean()) {
            allocations.putAll(activeToBeRemoved);
        }
        allocations.putAll(initializingToStay);
        if (randomBoolean()) {
            allocations.putAll(initializingToBeRemoved);
        }
        tracker.updateAllocationIdsFromMaster(active, initializing);
        if (randomBoolean()) {
            initializingToStay.forEach(tracker::markAllocationIdAsInSync);
        } else {
            initializing.forEach((allocationId) -> tracker.markAllocationIdAsInSync(allocationId, allocations.get(allocationId)));
        }
        if (randomBoolean()) {
            allocations.forEach(tracker::updateLocalCheckpoint);
        }

        // global checkpoint may be advanced
        tracker.updateCheckpointOnPrimary();

        // now remove shards
        if (randomBoolean()) {
            tracker.updateAllocationIdsFromMaster(activeToStay.keySet(), initializingToStay.keySet());
            allocations.forEach((aid, ckp) -> tracker.updateLocalCheckpoint(aid, ckp + 10L));
        } else {
            allocations.forEach((aid, ckp) -> tracker.updateLocalCheckpoint(aid, ckp + 10L));
            tracker.updateAllocationIdsFromMaster(activeToStay.keySet(), initializingToStay.keySet());
        }

        final long checkpoint = Stream.concat(activeToStay.values().stream(), initializingToStay.values().stream())
            .min(Long::compare).get() + 10; // we added 10 to make sure it's advanced in the second time

        // global checkpoint is advanced and we need a sync
        assertTrue(tracker.updateCheckpointOnPrimary());
        assertThat(tracker.getCheckpoint(), equalTo(checkpoint));
    }
}
