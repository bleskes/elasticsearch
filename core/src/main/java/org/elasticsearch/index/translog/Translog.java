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

package org.elasticsearch.index.translog;

import org.apache.lucene.index.Term;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.ReleasablePagedBytesReference;
import org.elasticsearch.common.io.stream.ReleasableBytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.Callback;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.FutureUtils;
import org.elasticsearch.common.util.concurrent.ReleasableLock;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.seqno.SequenceNumbersService;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.IndexShardComponent;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A Translog is a per index shard component that records all non-committed index operations in a durable manner.
 * In Elasticsearch there is one Translog instance per {@link org.elasticsearch.index.engine.InternalEngine}. The engine
 * records the current translog generation {@link Translog#getGeneration()} in it's commit metadata using {@link #TRANSLOG_GENERATION_KEY}
 * to reference the generation that contains all operations that have not yet successfully been committed to the engines lucene index.
 * Additionally, since Elasticsearch 2.0 the engine also records a {@link #TRANSLOG_UUID_KEY} with each commit to ensure a strong association
 * between the lucene index an the transaction log file. This UUID is used to prevent accidential recovery from a transaction log that belongs to a
 * different engine.
 * <p>
 * Each Translog has only one translog file open at any time referenced by a translog generation ID. This ID is written to a <tt>translog.ckp</tt> file that is designed
 * to fit in a single disk block such that a write of the file is atomic. The checkpoint file is written on each fsync operation of the translog and records the number of operations
 * written, the current tranlogs file generation and it's fsynced offset in bytes.
 * </p>
 * <p>
 * When a translog is opened the checkpoint is use to retrieve the latest translog file generation and subsequently to open the last written file to recovery operations.
 * The {@link org.elasticsearch.index.translog.Translog.TranslogGeneration} on {@link TranslogConfig#getTranslogGeneration()} given when the translog is opened is compared against
 * the latest generation and all consecutive translog files singe the given generation and the last generation in the checkpoint will be recovered and preserved until the next
 * generation is committed using {@link Translog# commit()}. In the common case the translog file generation in the checkpoint and the generation passed to the translog on creation are
 * the same. The only situation when they can be different is when an actual translog commit fails in between {@link Translog#prepareCommit()} and {@link Translog#commit()}. In such a case
 * the currently being committed translog file will not be deleted since it's commit was not successful. Yet, a new/current translog file is already opened at that point such that there is more than
 * one translog file present. Such an uncommitted translog file always has a <tt>translog-${gen}.ckp</tt> associated with it which is an fsynced copy of the it's last <tt>translog.ckp</tt> such that in
 * disaster recovery last fsynced offsets, number of operation etc. are still preserved.
 * </p>
 */
public class Translog extends AbstractIndexShardComponent implements IndexShardComponent, Closeable {

    /*
     * TODO
     *  - we might need something like a deletion policy to hold on to more than one translog eventually (I think sequence IDs needs this) but we can refactor as we go
     *  - use a simple BufferedOuputStream to write stuff and fold BufferedTranslogWriter into it's super class... the tricky bit is we need to be able to do random access reads even from the buffer
     *  - we need random exception on the FileSystem API tests for all this.
     *  - we need to page align the last write before we sync, we can take advantage of ensureSynced for this since we might have already fsynced far enough
     */
    public static final String TRANSLOG_GENERATION_KEY = "translog_generation";
    public static final String TRANSLOG_UUID_KEY = "translog_uuid";
    public static final String TRANSLOG_FILE_PREFIX = "translog-";
    public static final String TRANSLOG_FILE_SUFFIX = ".tlog";
    public static final String CHECKPOINT_SUFFIX = ".ckp";
    private static final String LEGACY_CHECKPOINT_FILE_NAME = "translog" + CHECKPOINT_SUFFIX;


    static final Pattern PARSE_STRICT_ID_PATTERN = Pattern.compile("^" + TRANSLOG_FILE_PREFIX + "(\\d+)(\\.tlog)$");
    static final Pattern PARSE_CHECKPOINT_ID_PATTERN = Pattern.compile("^" + TRANSLOG_FILE_PREFIX + "(\\d+)(\\.ckp)$");

    private volatile ScheduledFuture<?> syncScheduler;
    // this is a concurrent set and is not protected by any of the locks. The main reason
    // is that is being accessed by two separate classes (additions & reading are done by FsTranslog, remove by FsView when closed)
    private final Set<View> outstandingViews = ConcurrentCollections.newConcurrentSet();
    private BigArrays bigArrays;
    protected final ReleasableLock readLock;
    protected final ReleasableLock writeLock;
    private final Path location;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final TranslogConfig config;
    private final String translogUUID;
    private Callback<View> onViewClose = new Callback<View>() {
        @Override
        public void handle(View view) {
            logger.trace("closing view starting at translog [{}]", view.minTranslogGeneration());
            boolean removed = outstandingViews.remove(view);
            assert removed : "View was never set but was supposed to be removed";
            trimUnreferencedReaders();
        }
    };

    private final List<ImmutableTranslogReader> translogReaders = new ArrayList<>();
    private final List<TranslogWriter> translogWriters = new ArrayList<>();

    /**
     * Creates a new Translog instance. This method will create a new transaction log unless the given {@link TranslogConfig} has
     * a non-null {@link org.elasticsearch.index.translog.Translog.TranslogGeneration}. If the generation is null this method
     * us destructive and will delete all files in the translog path given.
     *
     * @see TranslogConfig#getTranslogPath()
     */
    public Translog(TranslogConfig config) throws IOException {
        super(config.getShardId(), config.getIndexSettings());
        this.config = config;
        TranslogGeneration translogGeneration = config.getTranslogGeneration();

        if (translogGeneration == null || translogGeneration.translogUUID == null) { // legacy case
            translogUUID = Strings.randomBase64UUID();
        } else {
            translogUUID = translogGeneration.translogUUID;
        }
        bigArrays = config.getBigArrays();
        ReadWriteLock rwl = new ReentrantReadWriteLock();
        readLock = new ReleasableLock(rwl.readLock());
        writeLock = new ReleasableLock(rwl.writeLock());
        this.location = config.getTranslogPath();
        Files.createDirectories(this.location);
        if (config.getSyncInterval().millis() > 0 && config.getThreadPool() != null) {
            syncScheduler = config.getThreadPool().schedule(config.getSyncInterval(), ThreadPool.Names.SAME, new Sync());
        }

        try {
            if (translogGeneration != null) {
                recoverFromFiles();
                if (this.translogWriters.isEmpty()) {
                    throw new IllegalStateException("at least one reader must be recovered");
                }
                if (getReaderForGeneration(translogGeneration.translogFileGeneration) == null) {
                    throw new IllegalStateException("failed to recover generation [" + translogGeneration.translogFileGeneration + "]");
                }
            } else {
                IOUtils.rm(location);
                logger.debug("wipe translog location - creating new translog");
                Files.createDirectories(location);
                final long generation = 1;
                translogWriters.add(createWriter(generation, 0));

            }
            // now that we know which files are there, create a new current one.
        } catch (Throwable t) {
            // close the opened translog files if we fail to create a new translog...
            IOUtils.closeWhileHandlingException(translogReaders);
            IOUtils.closeWhileHandlingException(translogWriters);
            throw t;
        }
    }

    /** recover all translog files found on disk */
    private final void recoverFromFiles() throws IOException {
        boolean success = false;
        assert translogReaders.isEmpty();
        assert translogWriters.isEmpty();
        try (ReleasableLock ignored = writeLock.acquire()) {
            final Path legacyCheckpoint = location.resolve(LEGACY_CHECKPOINT_FILE_NAME);
            if (Files.exists(legacyCheckpoint)) {
                long gen = Checkpoint.read(legacyCheckpoint).generation;
                Files.move(legacyCheckpoint, location.resolve(getCommitCheckpointFileName(gen)), StandardCopyOption.ATOMIC_MOVE);
                IOUtils.fsync(legacyCheckpoint.getParent(), true);
            }

            List<Checkpoint> allCheckpoints = readOrderedCheckpoints();
            // split into readers and writers. The readers point at completed files. Writers point at fails that miss operations
            List<Checkpoint> readerCheckpoints = new ArrayList<>();
            List<Checkpoint> writerCheckpoints = new ArrayList<>();
            while (allCheckpoints.isEmpty() == false) {
                Checkpoint checkpoint = allCheckpoints.remove(0);
                final boolean legacy = checkpoint.minSeqNo == SequenceNumbersService.UNASSIGNED_SEQ_NO;
                if (legacy || checkpoint.pendingWrites() == false) {
                    readerCheckpoints.add(checkpoint);
                } else {
                    writerCheckpoints.add(checkpoint);
                    break;
                }
            }
            writerCheckpoints.addAll(allCheckpoints);
            if (logger.isTraceEnabled()) {
                readerCheckpoints.stream().forEach(ckp -> logger.trace("recovered read  checkpoint [{}]", ckp));
                writerCheckpoints.stream().forEach(ckp -> logger.trace("recovered write checkpoint [{}]", ckp));
            } else {
                logger.debug("recovered [{}] read checkpoints and [{}] write", readerCheckpoints.size(), writerCheckpoints.size());
            }


            for (Checkpoint checkpoint : readerCheckpoints) {
                Path committedTranslogFile = location.resolve(getFilename(checkpoint.generation));
                if (Files.exists(committedTranslogFile) == false) {
                    throw new IllegalStateException("translog file doesn't exist with generation " + checkpoint.generation);
                }
                final ImmutableTranslogReader reader = openReader(committedTranslogFile, checkpoint);
                this.translogReaders.add(reader);
                logger.debug("recovered local translog from checkpoint {}", checkpoint);
            }
            if (writerCheckpoints.isEmpty()) {
                final Checkpoint lastCheckpoint = readerCheckpoints.get(readerCheckpoints.size() - 1);
                assert readerCheckpoints.stream().mapToLong(c -> c.generation).max().getAsLong() == lastCheckpoint.generation :
                        "last checkpoint doesn't have the maximum generation";
                final long minSeqNo = lastCheckpoint.maxSeqNo == SequenceNumbersService.UNASSIGNED_SEQ_NO ? 0 : lastCheckpoint.maxSeqNo + 1;
                this.translogWriters.add(createWriter(lastCheckpoint.generation + 1, minSeqNo));
            } else {
                for (Checkpoint checkpoint : writerCheckpoints) {
                    Path committedTranslogFile = location.resolve(getFilename(checkpoint.generation));
                    if (Files.exists(committedTranslogFile) == false) {
                        throw new IllegalStateException("translog file doesn't exist with generation " + checkpoint.generation);
                    }
                    final TranslogWriter writer = openWriter(committedTranslogFile, checkpoint);
                    this.translogWriters.add(writer);
                    logger.debug("recovered local translog from checkpoint {}", checkpoint);
                }
            }
            success = true;
        } finally {
            if (success == false) {
                IOUtils.closeWhileHandlingException(this.translogReaders);
                IOUtils.closeWhileHandlingException(this.translogWriters);
                this.translogReaders.clear();
                this.translogWriters.clear();
            }
        }
    }


    ImmutableTranslogReader openReader(Path path, Checkpoint checkpoint) throws IOException {
        assert parseIdFromFileName(path) == checkpoint.generation : "generation in checkpoint " + checkpoint + " doesn't match path " + path;
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        try {
            final ChannelReference raf = new ChannelReference(path, checkpoint.generation, channel);
            ImmutableTranslogReader reader = ImmutableTranslogReader.open(raf, checkpoint, translogUUID);
            channel = null;
            return reader;
        } finally {
            IOUtils.close(channel);
        }
    }

    protected TranslogWriter openWriter(Path path, Checkpoint checkpoint) throws IOException {
        TranslogWriter newFile;
        try {
            newFile = TranslogWriter.open(config.getType(), shardId, checkpoint, path, translogUUID,
                    config.getBufferSize(), config.getCheckpointArraySize(), getChannelFactory());
        } catch (IOException e) {
            throw new TranslogException(shardId, "failed to create new translog file", e);
        }
        return newFile;
    }


    /**
     * Extracts the translog generation from a file name.
     *
     * @throws IllegalArgumentException if the path doesn't match the expected pattern.
     */
    public static long parseIdFromFileName(Path translogFile) {
        final String fileName = translogFile.getFileName().toString();
        final Matcher matcher = PARSE_STRICT_ID_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("number formatting issue in a file that passed PARSE_STRICT_ID_PATTERN: " + fileName + "]", e);
            }
        }
        throw new IllegalArgumentException("can't parse id from file: " + fileName);
    }

    public void updateBuffer(ByteSizeValue bufferSize) {
        config.setBufferSize(bufferSize.bytesAsInt());
        try (ReleasableLock ignored = writeLock.acquire()) {
            getLastWriter().updateBufferSize(config.getBufferSize());
        }
    }

    private TranslogWriter getLastWriter() {
        return translogWriters.get(translogWriters.size() - 1);
    }


    /** Returns {@code true} if this {@code Translog} is still open. */
    public boolean isOpen() {
        return closed.get() == false;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            try (ReleasableLock ignored = writeLock.acquire()) {
                try {
                    for (TranslogWriter writer : translogWriters) {
                        writer.sync();
                    }
                } finally {
                    IOUtils.close(translogReaders);
                    IOUtils.close(translogWriters);
                }
            } finally {
                FutureUtils.cancel(syncScheduler);
                logger.debug("translog closed");
            }
        }
    }

    /**
     * Returns all translog locations as absolute paths.
     * These paths don't contain actual translog files they are
     * directories holding the transaction logs.
     */
    public Path location() {
        return location;
    }

    /**
     * Returns the generation of the current transaction log.
     */
    public long currentFileGeneration() {
        try (ReleasableLock ignored = readLock.acquire()) {
            return getLastWriter().getGeneration();
        }
    }

    /**
     * Returns the number of operations in the transaction files that aren't committed to lucene..
     */
    private int totalOperations(long minGeneration) {
        try (ReleasableLock ignored = readLock.acquire()) {
            ensureOpen();
            return Stream.concat(translogReaders.stream(), translogWriters.stream())
                    .filter(r -> r.getGeneration() >= minGeneration)
                    .mapToInt(TranslogReader::totalOperations)
                    .sum();
        }
    }

    /**
     * Returns the size in bytes of the translog files that aren't committed to lucene.
     */
    private long sizeInBytes(long minGeneration) {
        try (ReleasableLock ignored = readLock.acquire()) {
            ensureOpen();
            return Stream.concat(translogReaders.stream(), translogWriters.stream())
                    .filter(r -> r.getGeneration() >= minGeneration)
                    .mapToLong(TranslogReader::sizeInBytes)
                    .sum();
        }
    }


    protected TranslogWriter createWriter(long fileGeneration, long minSeqNo) throws IOException {
        TranslogWriter newFile;
        try {
            newFile = TranslogWriter.create(config.getType(), shardId, translogUUID, fileGeneration, minSeqNo, location.resolve(getFilename(fileGeneration)),
                    config.getBufferSize(), config.getCheckpointArraySize(), getChannelFactory());
        } catch (IOException e) {
            throw new TranslogException(shardId, "failed to create new translog file", e);
        }
        return newFile;
    }

    void trimUnreferencedReaders() {
        try (ReleasableLock ignored = writeLock.acquire()) {
            if (closed.get() || outstandingViews.isEmpty()) {
                return;
            }
            long minReferencedGen = outstandingViews.stream().mapToLong(View::minTranslogGeneration).min().getAsLong();
            while (translogReaders.isEmpty() == false) {
                ImmutableTranslogReader reader = translogReaders.get(0);
                if (reader.getGeneration() >= minReferencedGen) {
                    return;
                }
                translogReaders.remove(0);
                Path translogPath = reader.channelReference.getPath();
                logger.trace("delete translog file - not referenced and not current anymore {}", translogPath);
                IOUtils.closeWhileHandlingException(reader);
                IOUtils.deleteFilesIgnoringExceptions(translogPath);
                IOUtils.deleteFilesIgnoringExceptions(translogPath.resolveSibling(getCommitCheckpointFileName(reader.getGeneration())));
            }
        }
    }

    private void convertCompletedWriters() throws IOException {
        try (ReleasableLock ignored = writeLock.acquire()) {
            ensureOpen();
            // we always leave the last writer for new writes
            while (translogWriters.size() > 1) {
                if (translogWriters.get(0).pendingWrites()) {
                    return;
                }
                TranslogWriter writer = translogWriters.remove(0);
                translogReaders.add(writer.closeIntoReader());
            }
        }
    }


    /**
     * Read the Operation object from the given location. This method will try to read the given location from
     * the current or from the currently committing translog file. If the location is in a file that has already
     * been closed or even removed the method will return <code>null</code> instead.
     */
    public Translog.Operation read(Location location) {
        try (ReleasableLock ignored = readLock.acquire()) {
            ensureOpen();
            TranslogReader reader = getReaderForGeneration(location.generation);
            if (reader == null) {
                if (currentFileGeneration() < location.generation) {
                    throw new IllegalStateException("location generation [" + location.generation + "] is greater than the current generation [" + currentFileGeneration() + "]");
                }
                return null;
            }
            return reader.read(location);
        } catch (IOException e) {
            throw new ElasticsearchException("failed to read source from translog location " + location, e);
        }
    }

    private final TranslogReader getReaderForGeneration(final long generation) {
        for (final TranslogReader reader : translogWriters) {
            if (reader.getGeneration() == generation) {
                return reader;
            }
        }
        for (final TranslogReader reader : translogReaders) {
            if (reader.getGeneration() == generation) {
                return reader;
            }
        }
        return null;
    }

    /**
     * Adds a delete / index operations to the transaction log.
     *
     * @see org.elasticsearch.index.translog.Translog.Operation
     * @see Index
     * @see org.elasticsearch.index.translog.Translog.Delete
     */
    public Location add(Operation operation) throws IOException {
        final ReleasableBytesStreamOutput out = new ReleasableBytesStreamOutput(bigArrays);
        boolean needsNewWriter = false;
        boolean needsToConvertCompletedWriters = false;
        try {
            final BufferedChecksumStreamOutput checksumStreamOutput = new BufferedChecksumStreamOutput(out);
            final long start = out.position();
            out.skip(RamUsageEstimator.NUM_BYTES_INT);
            writeOperationNoSize(checksumStreamOutput, operation);
            final long end = out.position();
            final int operationSize = (int) (end - RamUsageEstimator.NUM_BYTES_INT - start);
            out.seek(start);
            out.writeInt(operationSize);
            out.seek(end);
            final ReleasablePagedBytesReference bytes = out.bytes();
            try (ReleasableLock ignored = readLock.acquire()) {
                ensureOpen();
                TranslogWriter writer = null;
                boolean lastWriter = true;
                for (int i = translogWriters.size() - 1; i >= 0; i--) {
                    TranslogWriter potentialWriter = translogWriters.get(i);
                    if (potentialWriter.getMinSeqNo() <= operation.seqNo()) {
                        writer = potentialWriter;
                        break;
                    }
                    lastWriter = false;
                }
                if (writer == null) {
                    throw new IllegalStateException("failed to find writer for [" + operation.seqNo() + "]");
                }
                long bytesBeforeWrite = writer.sizeInBytes();
                boolean pendingWritesBefore = writer.pendingWrites();
                Location location = writer.add(operation.seqNo(), bytes);
                operation.location(location);
                if (logger.isTraceEnabled()) {
                    logger.trace("op [{}] is added at location [{}]", operation, location);
                }
                if (config.isSyncOnEachOperation()) {
                    writer.sync();
                }
                assert writer.assertBytesAtLocation(location, bytes);
                needsNewWriter = lastWriter && bytesBeforeWrite <= config.getBytesPerGeneration() && writer.sizeInBytes() > config.getBytesPerGeneration();
                needsToConvertCompletedWriters = lastWriter == false && pendingWritesBefore && writer.pendingWrites() == false;
                return location;
            }
        } catch (AlreadyClosedException | IOException ex) {
            closeOnTragicEvent(ex);
            throw ex;
        } catch (Throwable e) {
            closeOnTragicEvent(e);
            throw new TranslogException(shardId, "Failed to write operation [" + operation + "]", e);
        } finally {
            Releasables.close(out.bytes());
            if (needsNewWriter) {
                startNewGeneration();
            }
            if (needsToConvertCompletedWriters) {
                convertCompletedWriters();
            }
        }
    }

    /**
     * Snapshots the current transaction log allowing to safely iterate over the snapshot.
     * Snapshots are fixed in time and will not be updated with future operations.
     */
    Snapshot newSnapshot() {
        return createSnapshot(Long.MIN_VALUE);
    }

    private Snapshot createSnapshot(long minGeneration) {
        try (ReleasableLock ignored = readLock.acquire()) {
            ensureOpen();
            Snapshot[] snapshots = Stream.concat(translogReaders.stream(), translogWriters.stream())
                    .filter(reader -> reader.getGeneration() >= minGeneration)
                    .map(TranslogReader::newSnapshot).toArray(Snapshot[]::new);
            return new MultiSnapshot(snapshots);
        }
    }

    /**
     * Returns a view into the current translog that is guaranteed to retain all current operations
     * while receiving future ones as well
     */
    public Translog.View newView() {
        // we need to acquire the read lock to make sure no new translog is created
        // and will be missed by the view we're making
        try (ReleasableLock ignored = readLock.acquire()) {
            ensureOpen();
            final long minGeneration;
            if (translogReaders.isEmpty() == false) {
                minGeneration = translogReaders.get(0).getGeneration();
            } else {
                minGeneration = translogWriters.get(0).getGeneration();
            }
            View view = new View(minGeneration, onViewClose);
            // this is safe as we know that no new translog is being made at the moment
            // (we hold a read lock) and the view will be notified of any future one
            outstandingViews.add(view);
            return view;
        }
    }

    /**
     * Sync's the translog.
     */
    public void sync() throws IOException {
        try (ReleasableLock ignored = readLock.acquire()) {
            if (closed.get() == false) {
                for (TranslogWriter writer : translogWriters) {
                    writer.sync();
                }
            }
        } catch (Throwable ex) {
            closeOnTragicEvent(ex);
            throw ex;
        }
    }

    public boolean syncNeeded() {
        try (ReleasableLock ignored = readLock.acquire()) {
            if (closed.get()) {
                return false;
            }
            for (TranslogWriter writer : translogWriters) {
                if (writer.syncNeeded()) {
                    return true;
                }
            }
            return false;
        }
    }

    /** package private for testing */
    public static String getFilename(long generation) {
        return TRANSLOG_FILE_PREFIX + generation + TRANSLOG_FILE_SUFFIX;
    }

    static String getCommitCheckpointFileName(long generation) {
        return TRANSLOG_FILE_PREFIX + generation + CHECKPOINT_SUFFIX;
    }


    /**
     * Ensures that the given location has be synced / written to the underlying storage.
     *
     * @return Returns <code>true</code> iff this call caused an actual sync operation otherwise <code>false</code>
     */
    public boolean ensureSynced(Location location) throws IOException {
        try (ReleasableLock ignored = readLock.acquire()) {
            for (int i = translogWriters.size() - 1; i >= 0; i--) {
                TranslogWriter writer = translogWriters.get(i);
                if (writer.getGeneration() == location.generation) {
                    ensureOpen();
                    return writer.syncUpTo(location.translogLocation + location.size);
                }
            }
        } catch (Throwable ex) {
            closeOnTragicEvent(ex);
            throw ex;
        }
        return false;
    }

    private void closeOnTragicEvent(Throwable ex) {
        if (getTragicException() != null) {
            try {
                close();
            } catch (Exception inner) {
                ex.addSuppressed(inner);
            }
        }
    }

    /**
     * return stats
     */
    public TranslogStats stats() {
        // acquire lock to make the two numbers roughly consistent (no file change half way)
        try (ReleasableLock ignored = readLock.acquire()) {
            long maxSeqNo = Long.MIN_VALUE;
            long localCheckpoint = Long.MAX_VALUE;
            for (TranslogWriter writer : translogWriters) {
                maxSeqNo = Math.max(maxSeqNo, writer.getMaxSeqNo());
                if (writer.pendingWrites()) {
                    localCheckpoint = Math.min(localCheckpoint, writer.getLocalCheckpoint());
                }
            }
            if (localCheckpoint == Long.MAX_VALUE) {
                localCheckpoint = getLastWriter().getLocalCheckpoint();
            }
            return new TranslogStats(totalOperations(-1), sizeInBytes(-1), maxSeqNo, localCheckpoint);
        }
    }

    public TranslogConfig getConfig() {
        return config;
    }

    /**
     * a view into the translog, capturing all translog file at the moment of creation
     * and updated with any future translog.
     */
    public class View implements Closeable {

        AtomicBoolean closed = new AtomicBoolean();
        volatile long minGeneration;
        private final Callback<View> onClose;

        View(long minGeneration, Callback<View> onClose) {
            // clone so we can safely mutate..
            this.minGeneration = minGeneration;
            this.onClose = onClose;
        }

        /** this smallest translog generation in this view */
        public long minTranslogGeneration() {
            return minGeneration;
        }

        /**
         * The total number of operations in the view.
         */
        public int totalOperations() {
            return Translog.this.totalOperations(minGeneration);
        }

        /**
         * Returns the size in bytes of the files behind the view.
         */
        public long sizeInBytes() {
            return Translog.this.sizeInBytes(minGeneration);
        }

        /** create a snapshot from this view */
        public Snapshot snapshot() {
            ensureOpen();
            return Translog.this.createSnapshot(minGeneration);
        }

        public synchronized void incMinGeneration(long newMinGeneration) {
            if (newMinGeneration < minGeneration) {
                throw new IllegalArgumentException("generation can only increase. current [" + minGeneration + "], got [" + newMinGeneration + "]");
            }
            minGeneration = newMinGeneration;
            trimUnreferencedReaders();
        }

        void ensureOpen() {
            if (closed.get()) {
                throw new AlreadyClosedException("View is already closed");
            }
        }

        @Override
        public void close() {
            if (closed.getAndSet(true) == false) {
                if (onClose != null) {
                    onClose.handle(this);
                }
            }
        }
    }

    class Sync implements Runnable {
        @Override
        public void run() {
            // don't re-schedule  if its closed..., we are done
            if (closed.get()) {
                return;
            }
            final ThreadPool threadPool = config.getThreadPool();
            if (syncNeeded()) {
                threadPool.executor(ThreadPool.Names.FLUSH).execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sync();
                        } catch (Exception e) {
                            logger.warn("failed to sync translog", e);
                        }
                        if (closed.get() == false) {
                            syncScheduler = threadPool.schedule(config.getSyncInterval(), ThreadPool.Names.SAME, Sync.this);
                        }
                    }
                });
            } else {
                syncScheduler = threadPool.schedule(config.getSyncInterval(), ThreadPool.Names.SAME, Sync.this);
            }
        }
    }

    public static class Location implements Accountable, Comparable<Location> {

        public final long generation;
        public final long translogLocation;
        public final int size;

        Location(long generation, long translogLocation, int size) {
            this.generation = generation;
            this.translogLocation = translogLocation;
            this.size = size;
        }

        @Override
        public long ramBytesUsed() {
            return RamUsageEstimator.NUM_BYTES_OBJECT_HEADER + 2 * RamUsageEstimator.NUM_BYTES_LONG + RamUsageEstimator.NUM_BYTES_INT;
        }

        @Override
        public Collection<Accountable> getChildResources() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "[generation: " + generation + ", location: " + translogLocation + ", size: " + size + "]";
        }

        @Override
        public int compareTo(Location o) {
            if (generation == o.generation) {
                return Long.compare(translogLocation, o.translogLocation);
            }
            return Long.compare(generation, o.generation);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Location location = (Location) o;

            if (generation != location.generation) {
                return false;
            }
            if (translogLocation != location.translogLocation) {
                return false;
            }
            return size == location.size;

        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(generation);
            result = 31 * result + Long.hashCode(translogLocation);
            result = 31 * result + size;
            return result;
        }
    }

    /**
     * A snapshot of the transaction log, allows to iterate over all the transaction log operations.
     */
    public interface Snapshot {

        /**
         * The total number of operations in the translog.
         */
        int estimatedTotalOperations();

        /**
         * Returns the next operation in the snapshot or <code>null</code> if we reached the end.
         */
        Translog.Operation next() throws IOException;

    }

    /**
     * A generic interface representing an operation performed on the transaction log.
     * Each is associated with a type.
     */
    public interface Operation extends Streamable {

        enum Type {
            @Deprecated
            CREATE((byte) 1),
            INDEX((byte) 2),
            DELETE((byte) 3),
            FAILURE((byte) 4);

            private final byte id;

            private Type(byte id) {
                this.id = id;
            }

            public byte id() {
                return this.id;
            }

            public static Type fromId(byte id) {
                switch (id) {
                    case 1:
                        return CREATE;
                    case 2:
                        return INDEX;
                    case 3:
                        return DELETE;
                    case 4:
                        return FAILURE;
                    default:
                        throw new IllegalArgumentException("No type mapped for [" + id + "]");
                }
            }
        }

        Type opType();

        long estimateSize();

        Source getSource();

        long seqNo();

        void location(Location location);

        @Nullable
        Translog.Location location();

    }

    public static class Source {
        public final BytesReference source;
        public final String routing;
        public final String parent;
        public final long timestamp;
        public final long ttl;

        public Source(BytesReference source, String routing, String parent, long timestamp, long ttl) {
            this.source = source;
            this.routing = routing;
            this.parent = parent;
            this.timestamp = timestamp;
            this.ttl = ttl;
        }
    }

    public static class Index implements Operation {
        public static final int SERIALIZATION_FORMAT = 7;

        private String id;
        private String type;
        private long seqNo = -1;
        private long version = Versions.MATCH_ANY;
        private VersionType versionType = VersionType.INTERNAL;
        private BytesReference source;
        private String routing;
        private String parent;
        private long timestamp;
        private long ttl;
        private Location location;

        public Index() {
        }

        public Index(Engine.Index index) {
            this.id = index.id();
            this.type = index.type();
            this.source = index.source();
            this.routing = index.routing();
            this.parent = index.parent();
            this.seqNo = index.seqNo();
            this.version = index.version();
            this.timestamp = index.timestamp();
            this.ttl = index.ttl();
            this.versionType = index.versionType();
        }

        public Index(String type, String id, long seqNo, byte[] source) {
            this.type = type;
            this.id = id;
            this.source = new BytesArray(source);
            this.seqNo = seqNo;
            this.version = 0;
        }

        @Override
        public Type opType() {
            return Type.INDEX;
        }

        @Override
        public long estimateSize() {
            return ((id.length() + type.length()) * 2) + source.length() + 12;
        }

        public String type() {
            return this.type;
        }

        public String id() {
            return this.id;
        }

        public String routing() {
            return this.routing;
        }

        public String parent() {
            return this.parent;
        }

        public long timestamp() {
            return this.timestamp;
        }

        public long ttl() {
            return this.ttl;
        }

        public BytesReference source() {
            return this.source;
        }

        public long seqNo() {
            return seqNo;
        }

        public void location(Location location) {
            this.location = location;
        }

        @Override
        public Location location() {
            return location;
        }

        public long version() {
            return this.version;
        }

        public VersionType versionType() {
            return versionType;
        }

        @Override
        public Source getSource() {
            return new Source(source, routing, parent, timestamp, ttl);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            int version = in.readVInt(); // version
            id = in.readString();
            type = in.readString();
            source = in.readBytesReference();
            try {
                if (version >= 1) {
                    if (in.readBoolean()) {
                        routing = in.readString();
                    }
                }
                if (version >= 2) {
                    if (in.readBoolean()) {
                        parent = in.readString();
                    }
                }
                if (version >= 3) {
                    this.version = in.readLong();
                }
                if (version >= 4) {
                    this.timestamp = in.readLong();
                }
                if (version >= 5) {
                    this.ttl = in.readLong();
                }
                if (version >= 6) {
                    this.versionType = VersionType.fromValue(in.readByte());
                }
                if (version >= 7) {
                    this.seqNo = in.readVLong();
                }
            } catch (Exception e) {
                throw new ElasticsearchException("failed to read [" + type + "][" + id + "]", e);
            }

            assert versionType.validateVersionForWrites(version);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(SERIALIZATION_FORMAT);
            out.writeString(id);
            out.writeString(type);
            out.writeBytesReference(source);
            if (routing == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeString(routing);
            }
            if (parent == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                out.writeString(parent);
            }
            out.writeLong(version);
            out.writeLong(timestamp);
            out.writeLong(ttl);
            out.writeByte(versionType.getValue());
            out.writeVLong(seqNo);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Index index = (Index) o;

            if (version != index.version ||
                    seqNo != index.seqNo ||
                    timestamp != index.timestamp ||
                    ttl != index.ttl ||
                    id.equals(index.id) == false ||
                    type.equals(index.type) == false ||
                    versionType != index.versionType ||
                    source.equals(index.source) == false) {
                return false;
            }
            if (routing != null ? !routing.equals(index.routing) : index.routing != null) {
                return false;
            }
            return !(parent != null ? !parent.equals(index.parent) : index.parent != null);

        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + Long.hashCode(seqNo);
            result = 31 * result + Long.hashCode(version);
            result = 31 * result + versionType.hashCode();
            result = 31 * result + source.hashCode();
            result = 31 * result + (routing != null ? routing.hashCode() : 0);
            result = 31 * result + (parent != null ? parent.hashCode() : 0);
            result = 31 * result + Long.hashCode(timestamp);
            result = 31 * result + Long.hashCode(ttl);
            return result;
        }

        @Override
        public String toString() {
            return "Index{" +
                    "id='" + id + '\'' +
                    ", type='" + type + '\'' +
                    ", location=" + location +
                    '}';
        }
    }

    public static class Delete implements Operation {
        public static final int SERIALIZATION_FORMAT = 3;

        private Term uid;
        private long seqNo = -1L;
        private long version = Versions.MATCH_ANY;
        private VersionType versionType = VersionType.INTERNAL;
        private Location location;

        public Delete() {
        }

        public Delete(Engine.Delete delete) {
            this(delete.uid(), delete.seqNo(), delete.version(), delete.versionType());
        }

        /** utility for testing */
        public Delete(Term uid, long seqNo) {
            this(uid, seqNo, 0, VersionType.EXTERNAL);
        }

        public Delete(Term uid, long seqNo, long version, VersionType versionType) {
            this.uid = uid;
            this.version = version;
            this.versionType = versionType;
            this.seqNo = seqNo;
        }

        @Override
        public Type opType() {
            return Type.DELETE;
        }

        @Override
        public long estimateSize() {
            return ((uid.field().length() + uid.text().length()) * 2) + 20;
        }

        public Term uid() {
            return this.uid;
        }

        public long seqNo() {
            return seqNo;
        }

        public long version() {
            return this.version;
        }

        public VersionType versionType() {
            return this.versionType;
        }

        @Override
        public Source getSource() {
            throw new IllegalStateException("trying to read doc source from delete operation");
        }

        public void location(Location location) {
            this.location = location;
        }

        @Override
        public Location location() {
            return location;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            int version = in.readVInt(); // version
            uid = new Term(in.readString(), in.readString());
            if (version >= 1) {
                this.version = in.readLong();
            }
            if (version >= 2) {
                this.versionType = VersionType.fromValue(in.readByte());
            }
            if (version >= 3) {
                this.seqNo = in.readVLong();
            }
            assert versionType.validateVersionForWrites(version);

        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(SERIALIZATION_FORMAT);
            out.writeString(uid.field());
            out.writeString(uid.text());
            out.writeLong(version);
            out.writeByte(versionType.getValue());
            out.writeVLong(seqNo);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Delete delete = (Delete) o;

            return version == delete.version && seqNo == delete.seqNo &&
                    uid.equals(delete.uid) &&
                    versionType == delete.versionType;
        }

        @Override
        public int hashCode() {
            int result = uid.hashCode();
            result = 31 * result + Long.hashCode(seqNo);
            result = 31 * result + Long.hashCode(version);
            result = 31 * result + versionType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Delete{" +
                    "uid=" + uid +
                    ", location=" + location +
                    '}';
        }
    }

    public static class Failure implements Operation {
        public static final int SERIALIZATION_FORMAT = 0;

        private String description;
        private long seqNo;
        private Location location;

        Failure() {

        }

        public Failure(long seqNo, String description) {
            this.seqNo = seqNo;
            this.description = description;
        }

        @Override
        public Type opType() {
            return Type.FAILURE;
        }

        @Override
        public long estimateSize() {
            return description.length() * 2 + RamUsageEstimator.NUM_BYTES_INT + RamUsageEstimator.NUM_BYTES_LONG;
        }

        @Override
        public Source getSource() {
            throw new IllegalStateException("trying to read doc source from failure operation");
        }

        @Override
        public long seqNo() {
            return seqNo;
        }

        public String description() {
            return description;
        }

        public void location(Location location) {
            this.location = location;
        }

        @Override
        public Location location() {
            return location;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            int version = in.readVInt(); // version
            seqNo = in.readZLong();
            description = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(SERIALIZATION_FORMAT);
            out.writeZLong(seqNo);
            out.writeString(description);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Failure failure = (Failure) o;

            if (seqNo != failure.seqNo) {
                return false;
            }
            return description != null ? description.equals(failure.description) : failure.description == null;
        }

        @Override
        public int hashCode() {
            int result = description != null ? description.hashCode() : 0;
            result = 31 * result + (int) (seqNo ^ (seqNo >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "Failure{" +
                    "description='" + description + '\'' +
                    ", seqNo=" + seqNo +
                    ", location=" + location +
                    '}';
        }
    }


    public enum Durabilty {
        /**
         * Async durability - translogs are synced based on a time interval.
         */
        ASYNC,
        /**
         * Request durability - translogs are synced for each high levle request (bulk, index, delete)
         */
        REQUEST;

    }

    private static void verifyChecksum(BufferedChecksumStreamInput in) throws IOException {
        // This absolutely must come first, or else reading the checksum becomes part of the checksum
        long expectedChecksum = in.getChecksum();
        long readChecksum = in.readInt() & 0xFFFF_FFFFL;
        if (readChecksum != expectedChecksum) {
            throw new TranslogCorruptedException("translog stream is corrupted, expected: 0x" +
                    Long.toHexString(expectedChecksum) + ", got: 0x" + Long.toHexString(readChecksum));
        }
    }

    /**
     * Reads a list of operations written with {@link #writeOperations(StreamOutput, List)}
     */
    public static List<Operation> readOperations(StreamInput input) throws IOException {
        ArrayList<Operation> operations = new ArrayList<>();
        int numOps = input.readInt();
        final BufferedChecksumStreamInput checksumStreamInput = new BufferedChecksumStreamInput(input);
        for (int i = 0; i < numOps; i++) {
            operations.add(readOperation(checksumStreamInput));
        }
        return operations;
    }

    static Translog.Operation readOperation(BufferedChecksumStreamInput in) throws IOException {
        Translog.Operation operation;
        try {
            final int opSize = in.readInt();
            if (opSize < 4) { // 4byte for the checksum
                throw new AssertionError("operation size must be at least 4 but was: " + opSize);
            }
            in.resetDigest(); // size is not part of the checksum!
            if (in.markSupported()) { // if we can we validate the checksum first
                // we are sometimes called when mark is not supported this is the case when
                // we are sending translogs across the network with LZ4 compression enabled - currently there is no way s
                // to prevent this unfortunately.
                in.mark(opSize);

                in.skip(opSize - 4);
                verifyChecksum(in);
                in.reset();
            }
            Translog.Operation.Type type = Translog.Operation.Type.fromId(in.readByte());
            operation = newOperationFromType(type);
            operation.readFrom(in);
            verifyChecksum(in);
        } catch (EOFException e) {
            throw new TruncatedTranslogException("reached premature end of file, translog is truncated", e);
        } catch (AssertionError | Exception e) {
            throw new TranslogCorruptedException("translog corruption while reading from stream", e);
        }
        return operation;
    }

    /**
     * Writes all operations in the given iterable to the given output stream including the size of the array
     * use {@link #readOperations(StreamInput)} to read it back.
     */
    public static void writeOperations(StreamOutput outStream, List<Operation> toWrite) throws IOException {
        final ReleasableBytesStreamOutput out = new ReleasableBytesStreamOutput(BigArrays.NON_RECYCLING_INSTANCE);
        try {
            outStream.writeInt(toWrite.size());
            final BufferedChecksumStreamOutput checksumStreamOutput = new BufferedChecksumStreamOutput(out);
            for (Operation op : toWrite) {
                out.reset();
                final long start = out.position();
                out.skip(RamUsageEstimator.NUM_BYTES_INT);
                writeOperationNoSize(checksumStreamOutput, op);
                long end = out.position();
                int operationSize = (int) (out.position() - RamUsageEstimator.NUM_BYTES_INT - start);
                out.seek(start);
                out.writeInt(operationSize);
                out.seek(end);
                ReleasablePagedBytesReference bytes = out.bytes();
                bytes.writeTo(outStream);
            }
        } finally {
            Releasables.close(out.bytes());
        }

    }

    public static void writeOperationNoSize(BufferedChecksumStreamOutput out, Translog.Operation op) throws IOException {
        // This BufferedChecksumStreamOutput remains unclosed on purpose,
        // because closing it closes the underlying stream, which we don't
        // want to do here.
        out.resetDigest();
        out.writeByte(op.opType().id());
        op.writeTo(out);
        long checksum = out.getChecksum();
        out.writeInt((int) checksum);
    }

    /**
     * Returns a new empty translog operation for the given {@link Translog.Operation.Type}
     */
    static Translog.Operation newOperationFromType(Translog.Operation.Type type) throws IOException {
        switch (type) {
            case CREATE:
                // the deserialization logic in Index was identical to that of Create when create was deprecated
                return new Index();
            case DELETE:
                return new Translog.Delete();
            case INDEX:
                return new Index();
            case FAILURE:
                return new Failure();
            default:
                throw new IOException("No type for [" + type + "]");
        }
    }


    public long startNewGeneration() throws IOException {
        try (ReleasableLock ignored = writeLock.acquire()) {
            ensureOpen();
            TranslogWriter current = getLastWriter();
            final long newGeneration = current.getGeneration() + 1;
            this.translogWriters.add(createWriter(newGeneration, current.maxSeqNo + 1));
            logger.trace("current translog set to [{}]", current.getGeneration());
            convertCompletedWriters();
            return newGeneration;
        } catch (Throwable t) {
            IOUtils.closeWhileHandlingException(this); // tragic event
            throw t;
        }
    }

    /**
     * References a transaction log generation
     */
    public final static class TranslogGeneration {
        public final String translogUUID;
        public final long translogFileGeneration;

        public TranslogGeneration(String translogUUID, long translogFileGeneration) {
            this.translogUUID = translogUUID;
            this.translogFileGeneration = translogFileGeneration;
        }

    }

    /**
     * Returns the current generation of this translog. This corresponds to the latest uncommitted translog generation
     */
    public TranslogGeneration getGeneration() {
        try (ReleasableLock ignored = writeLock.acquire()) {
            return new TranslogGeneration(translogUUID, currentFileGeneration());
        }
    }

    /**
     * Returns <code>true</code> iff the given generation is the current generation of this translog
     */
    public boolean isCurrent(TranslogGeneration generation) {
        try (ReleasableLock ignored = writeLock.acquire()) {
            if (generation != null) {
                if (generation.translogUUID.equals(translogUUID) == false) {
                    throw new IllegalArgumentException("commit belongs to a different translog: " + generation.translogUUID + " vs. " + translogUUID);
                }
                return generation.translogFileGeneration == currentFileGeneration();
            }
        }
        return false;
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new AlreadyClosedException("translog is already closed", getTragicException());
        }
    }

    /**
     * The number of currently open views
     */
    int getNumOpenViews() {
        return outstandingViews.size();
    }

    TranslogWriter.ChannelFactory getChannelFactory() {
        return TranslogWriter.ChannelFactory.DEFAULT;
    }

    /**
     * If this {@code Translog} was closed as a side-effect of a tragic exception,
     * e.g. disk full while flushing a new segment, this returns the root cause exception.
     * Otherwise (no tragic exception has occurred) it returns null.
     */
    public Throwable getTragicException() {
        try (ReleasableLock ignored = readLock.acquire()) {
            Optional<TranslogWriter> maybeWriter = translogWriters.stream().filter(w -> w.getTragicException() != null).findAny();
            if (maybeWriter.isPresent()) {
                return maybeWriter.get().getTragicException();
            } else {
                return null;
            }
        }
    }

    /** Reads and returns the current checkpoint */
    final List<Checkpoint> readOrderedCheckpoints() throws IOException {
        List<Checkpoint> checkpoints = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(location)) {
            for (Path path : stream) {
                Matcher matcher = PARSE_CHECKPOINT_ID_PATTERN.matcher(path.getFileName().toString());
                if (matcher.matches()) {
                    long generation = Long.parseLong(matcher.group(1));
                    final Checkpoint checkpoint = Checkpoint.read(path);
                    if (checkpoint.generation != generation) {
                        throw new IllegalStateException("Checkpoint file " + path.getFileName() + " generation mismatch: content is " + checkpoint + " but file name indicates: " + generation);
                    }
                    checkpoints.add(checkpoint);
                }
            }
        }
        checkpoints.sort(Comparator.comparing(checkpoint -> checkpoint.generation));
        for (int i = 0; i < checkpoints.size() - 1; i++) {
            if (checkpoints.get(i).generation + 1 != checkpoints.get(i + 1).generation) {
                logger.warn("non-consecutive translog generations found {}", checkpoints);
                throw new IllegalStateException("translog ids must be consecutive. found gap between [" +
                        checkpoints.get(i).generation + "] and [" + checkpoints.get(i + 1).generation + "]");
            }
        }
        return checkpoints;
    }

}
