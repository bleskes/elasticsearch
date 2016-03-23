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

package org.elasticsearch.cluster.metadata;

import org.elasticsearch.cluster.Diff;
import org.elasticsearch.cluster.Diffable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamInputReader;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.Index;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A collection of tombstones for explicitly marking indices as deleted in the cluster state.
 *
 * The cluster state contains a list of index tombstones for indices that have been
 * deleted in the cluster.  Because cluster states are processed asynchronously by
 * nodes and a node could be removed from the cluster for a period of time, the
 * tombstones remain in the cluster state for a fixed period of time, after which
 * they are purged.
 */
final public class IndexGraveyard implements ToXContent, Diffable<IndexGraveyard> {

    // X-Content key for index tombstones
    public static final String TOMBSTONES_KEY = "index_tombstones";

    // Setting for the maximum tombstones allowed in the cluster state;
    // prevents the cluster state size from exploding too large, but it opens the
    // very unlikely risk that if there are greater than MAX_TOMBSTONES index
    // deletions while a node was offline, when it comes back online, it will have
    // missed index deletions that it may need to process.
    private static final int MAX_TOMBSTONES = 10000;

    // Setting for the expiration window of tombstone entries.  Any tombstone entries
    // that have been in the list longer than EXPIRATION_WINDOW will be purged on the
    // next cluster update.
    static final long EXPIRATION_WINDOW = 1000 * 60 * 60 * 24 * 14; // 14 days

    // holds the index tombstones
    private final Deque<Tombstone> tombstones;

    public IndexGraveyard() {
        tombstones = new ArrayDeque<>();
    }

    // copy constructor
    public IndexGraveyard(final IndexGraveyard that) {
        this.tombstones = new ArrayDeque<>(that.tombstones);
    }

    public IndexGraveyard(final StreamInput in) throws IOException {
        final int queueSize = in.readVInt();
        tombstones = new ArrayDeque<>(queueSize);
        for (int i = 0; i < queueSize; i++) {
            tombstones.add(new Tombstone(in));
        }
    }

    // only used by fromXContent
    private IndexGraveyard(final List<Tombstone> list) {
        this.tombstones = new ArrayDeque<>(list);
    }

    // only used by IndexGraveyardDiff
    private IndexGraveyard(final Deque<Tombstone> queue) {
        this.tombstones = new ArrayDeque<>(queue);
    }

    /**
     * Do they have the same index tombstones (deletions)?
     */
    public boolean isEquals(IndexGraveyard that) {
        // array deque doesn't implement equals
        return Objects.equals(new ArrayList<>(tombstones), new ArrayList<>(that.tombstones));
    }

    /**
     * Add a deleted index to the list of tombstones in the cluster state.  Returns the
     * purged tombstone if the size of the list grew too big, else return none.
     */
    public Optional<Tombstone> addTombstone(final Index index) {
        return addTombstone(index, System.currentTimeMillis(), System.nanoTime());
    }

    /**
     * Add a deleted index to the list of tombstones in the cluster state, along with
     * a specified deletion date and nanosecond date.  Returns the purged tombstone
     * if the size of the list grew too big, else return none.
     */
    Optional<Tombstone> addTombstone(final Index index, final long deletionDate, final long deletionDateNS) {
        final Optional<Tombstone> purged;
        if (tombstones.size() == MAX_TOMBSTONES) {
            // purge the oldest entry
            purged = Optional.of(tombstones.removeFirst());
        } else {
            purged = Optional.empty();
        }
        tombstones.addLast(new Tombstone(index, deletionDate, deletionDateNS));
        return purged;
    }

    /**
     * Get a copy of the current index tombstones.
     */
    List<Tombstone> getTombstones() {
        return new ArrayList<>(tombstones);
    }

    /**
     * Purge tombstone entries by date, if they have passed a certain expiration window.
     * Returns the number of entries that were purged.
     */
    public int purge() {
        int count = 0;
        final long now = System.currentTimeMillis();
        while (tombstones.isEmpty() == false) {
            // start from the beginning, it has the oldest entries
            final Tombstone tombstone = tombstones.getFirst();
            if (now - tombstone.deleteDate > EXPIRATION_WINDOW) {
                // passed the expiration window, remove the entry
                tombstones.removeFirst();
                count++;
            } else {
                // still in the expiration window, we know that everything else after it will also still fit in the window, so exit
                break;
            }
        }
        return count;
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final Params params) throws IOException {
        builder.startArray(TOMBSTONES_KEY);
        for (Tombstone tombstone : tombstones) {
            tombstone.toXContent(builder, params);
        }
        return builder.endArray();
    }

    public static IndexGraveyard fromXContent(final XContentParser parser) throws IOException {
        List<Tombstone> tombstones = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            tombstones.add(Tombstone.fromXContent(parser));
        }
        return new IndexGraveyard(tombstones);
    }

    @Override
    public String toString() {
        return "IndexGraveyard[" + tombstones + "]";
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeVInt(tombstones.size());
        for (Tombstone tombstone : tombstones) {
            tombstone.writeTo(out);
        }
    }

    @Override
    public IndexGraveyard readFrom(final StreamInput in) throws IOException {
        return new IndexGraveyard(in);
    }

    @Override
    public IndexGraveyardDiff diff(final IndexGraveyard previous) {
        return new IndexGraveyardDiff(previous, this);
    }

    @Override
    public IndexGraveyardDiff readDiffFrom(final StreamInput in) throws IOException {
        return new IndexGraveyardDiff(in);
    }

    /**
     * A class representing a diff of two IndexGraveyard objects.
     */
    final public static class IndexGraveyardDiff implements Diff<IndexGraveyard> {

        private final List<Tombstone> added;
        private final List<Tombstone> removed;

        IndexGraveyardDiff(final StreamInput in) throws IOException {
            added = Collections.unmodifiableList(in.readList(Tombstone.PROTOTYPE));
            removed = Collections.unmodifiableList(in.readList(Tombstone.PROTOTYPE));
        }

        IndexGraveyardDiff(final IndexGraveyard previous, final IndexGraveyard current) {
            final Deque<Tombstone> previousTombstones = previous.tombstones;
            final Deque<Tombstone> currentTombstones = current.tombstones;
            final List<Tombstone> added = new ArrayList<>();
            final List<Tombstone> removed = new ArrayList<>();
            if (previousTombstones.isEmpty()) {
                // nothing will have been removed, and all entries in current are new
                added.addAll(currentTombstones);
            } else if (currentTombstones.isEmpty()) {
                // nothing will have been added, and all entries in previous are removed
                removed.addAll(previousTombstones);
            } else {
                // look through the back, starting from the end, for added tombstones
                final Tombstone lastAddedTombstone = previousTombstones.getLast();
                for (Iterator<Tombstone> iter = currentTombstones.descendingIterator(); iter.hasNext();) {
                    final Tombstone tombstone = iter.next();
                    if (lastAddedTombstone.equals(tombstone)) {
                        // already have this entry, so will have all previous entries as well
                        break;
                    } else {
                        added.add(tombstone);
                    }
                }
                // look through the front, starting at the beginning, for removed tombstones
                final Tombstone firstTombstone = currentTombstones.getFirst();
                for (Iterator<Tombstone> iter = previousTombstones.iterator(); iter.hasNext();) {
                    final Tombstone tombstone = iter.next();
                    if (firstTombstone.equals(tombstone)) {
                        // current list has this entry, so no more removing
                        break;
                    } else {
                        removed.add(tombstone);
                    }
                }
            }
            Collections.reverse(added); // added list was created from back to front, so have to reverse
            this.added = Collections.unmodifiableList(added);
            this.removed = Collections.unmodifiableList(removed);
        }

        @Override
        public void writeTo(final StreamOutput out) throws IOException {
            out.writeList(added);
            out.writeList(removed);
        }

        @Override
        public IndexGraveyard apply(final IndexGraveyard old) {
            final Deque<Tombstone> oldQueue = old.tombstones;
            final Deque<Tombstone> newQueue = new ArrayDeque<>(oldQueue);
            for (Tombstone tombstone : removed) {
                newQueue.remove(tombstone);
            }
            for (Tombstone tombstone : added) {
                newQueue.addLast(tombstone);
            }
            return new IndexGraveyard(newQueue);
        }

        /** The index tombstones (deletions) that were added between two states */
        public List<Tombstone> getAdded() {
            return added;
        }

        /** The index tombstones (deletions) that were removed between two states */
        public List<Tombstone> getRemoved() {
            return removed;
        }
    }

    /**
     * An individual tombstone entry for representing a deleted index.
     */
    final public static class Tombstone implements ToXContent, StreamInputReader<Tombstone>, Writeable<Tombstone> {

        public static final Tombstone PROTOTYPE = new Tombstone(new Index("_na_", "_na_"), 0L, 0L);
        private static final String INDEX_UUID_KEY = "index_uuid";
        private static final String INDEX_NAME_KEY = "index_name";
        private static final String DELETE_DATE_KEY = "delete_date";
        private static final String DELETE_DATE_NS_KEY = "delete_date_ns";
        static final ObjectParser<Tombstone.Builder, Void> TOMBSTONE_PARSER = new ObjectParser<>("tombstoneEntry");
        static {
            TOMBSTONE_PARSER.declareString(Tombstone.Builder::indexUUID, new ParseField(INDEX_UUID_KEY));
            TOMBSTONE_PARSER.declareString(Tombstone.Builder::indexName, new ParseField(INDEX_NAME_KEY));
            TOMBSTONE_PARSER.declareLong(Tombstone.Builder::deleteDate, new ParseField(DELETE_DATE_KEY));
            TOMBSTONE_PARSER.declareLong(Tombstone.Builder::deleteDateNS, new ParseField(DELETE_DATE_NS_KEY));
        }

        private final Index index;
        private final long deleteDate;
        private final long deleteDateNS;

        private Tombstone(final Index index, final long deleteDate, final long deleteDateNS) {
            Objects.requireNonNull(index, "index must be set");
            if (deleteDate < 0L) {
                throw new IllegalArgumentException("invalid deleteDate [" + deleteDate + "]");
            }
            if (deleteDateNS < 0L) {
                throw new IllegalArgumentException("invalid deleteDateNS [" + deleteDateNS + "]");
            }
            this.index = index;
            this.deleteDate = deleteDate;
            this.deleteDateNS = deleteDateNS;
        }

        // create from stream
        private Tombstone(StreamInput in) throws IOException {
            index = new Index(in);
            deleteDate = in.readLong();
            deleteDateNS = in.readLong();
        }

        /**
         * The deleted index.
         */
        public Index getIndex() {
            return index;
        }

        /**
         * The date in milliseconds that the index deletion event occurred, used for purging tombstone entries from the cluster state.
         */
        public long getDeleteDate() {
            return deleteDate;
        }

        /**
         * The nano time that the index deletion event occurred, mostly for logging/debugging purposes.
         */
        public long getDeleteDateNS() {
            return deleteDateNS;
        }

        @Override
        public void writeTo(final StreamOutput out) throws IOException {
            index.writeTo(out);
            out.writeLong(deleteDate);
            out.writeLong(deleteDateNS);
        }

        @Override
        public Tombstone readFrom(final StreamInput in) throws IOException {
            return read(in);
        }

        @Override
        public Tombstone read(final StreamInput in) throws IOException {
            return new Tombstone(in);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Tombstone that = (Tombstone) other;
            if (index.equals(that.index) == false) {
                return false;
            }
            if (deleteDate != that.deleteDate) {
                return false;
            }
            if (deleteDateNS != that.deleteDateNS) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, deleteDate, deleteDateNS);
        }

        @Override
        public String toString() {
            return "[index=" + index + ", deleteDate=" + deleteDate + ", deleteDateNS=" + deleteDateNS + "]";
        }

        @Override
        public XContentBuilder toXContent(final XContentBuilder builder, final Params params) throws IOException {
            builder.startObject();
            builder.field(INDEX_UUID_KEY, index.getUUID());
            builder.field(INDEX_NAME_KEY, index.getName());
            builder.field(DELETE_DATE_KEY, deleteDate);
            builder.field(DELETE_DATE_NS_KEY, deleteDateNS);
            return builder.endObject();
        }

        public static Tombstone fromXContent(final XContentParser parser) throws IOException {
            return TOMBSTONE_PARSER.parse(parser, new Tombstone.Builder()).build();
        }

        /**
         * A builder for building tombstone entries.
         */
        final private static class Builder {
            private String indexUUID;
            private String indexName;
            private long deleteDate = -1L;
            private long deleteDateNS = -1L;

            public void indexUUID(final String indexUUID) {
                this.indexUUID = indexUUID;
            }

            public void indexName(final String indexName) {
                this.indexName = indexName;
            }

            public void deleteDate(final long deleteDate) {
                this.deleteDate = deleteDate;
            }

            public void deleteDateNS(final long deleteDateNS) {
                this.deleteDateNS = deleteDateNS;
            }

            public Tombstone build() {
                // only using this from the object parser, so we know we will have an indexName and indexUUID
                return new Tombstone(new Index(indexName, indexUUID), deleteDate, deleteDateNS);
            }
        }
    }

}
