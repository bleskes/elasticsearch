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

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.OutputStreamDataOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.Channels;
import org.elasticsearch.common.util.concurrent.ReleasableLock;
import org.elasticsearch.index.seqno.LocalCheckpointService;
import org.elasticsearch.index.seqno.SequenceNumbersService;
import org.elasticsearch.index.shard.ShardId;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TranslogWriter extends TranslogReader implements Closeable {

    public static final String TRANSLOG_CODEC = "translog";
    public static final int VERSION_CHECKSUMS = 1;
    public static final int VERSION_CHECKPOINTS = 2; // since 2.0 we have checkpoints?
    public static final int VERSION = VERSION_CHECKPOINTS;

    protected final ShardId shardId;
    protected final ReleasableLock readLock;
    protected final ReleasableLock writeLock;
    /* the offset in bytes that was written when the file was last synced*/
    protected volatile long lastSyncedOffset;
    /* the number of translog operations written to this file */
    protected volatile int operationCounter;
    /* the offset in bytes written to the file */
    protected volatile long writtenOffset;
    /* if we hit an exception that we can't recover from we assign it to this var and ship it with every AlreadyClosedException we throw */
    private volatile Throwable tragedy;

    protected final AtomicBoolean closed = new AtomicBoolean(false);

    protected final long minSeqNo;
    protected volatile long maxSeqNo;
    final LocalCheckpointService checkpointSeqNoServiceService;


    public TranslogWriter(ShardId shardId, Checkpoint checkpoint, ChannelReference channelReference, int checkpointArraySize) throws IOException {
        super(checkpoint.generation, channelReference, channelReference.getChannel().position());
        this.shardId = shardId;
        ReadWriteLock rwl = new ReentrantReadWriteLock();
        readLock = new ReleasableLock(rwl.readLock());
        writeLock = new ReleasableLock(rwl.writeLock());
        this.writtenOffset = channelReference.getChannel().position();
        this.lastSyncedOffset = channelReference.getChannel().position();
        this.operationCounter = checkpoint.numOps;
        this.minSeqNo = checkpoint.minSeqNo;
        this.maxSeqNo = checkpoint.maxSeqNo;
        this.checkpointSeqNoServiceService = new LocalCheckpointService(shardId, minSeqNo, checkpoint.localCheckpoint, checkpointArraySize);
    }

    public TranslogWriter(ShardId shardId, ImmutableTranslogReader reader, int checkpointArraySize) throws IOException {
        super(reader.getGeneration(), reader.channelReference, reader.getFirstOperationOffset());
        this.shardId = shardId;
        ReadWriteLock rwl = new ReentrantReadWriteLock();
        readLock = new ReleasableLock(rwl.readLock());
        writeLock = new ReleasableLock(rwl.writeLock());
        Checkpoint checkpoint = reader.getInfo();
        channel.position(checkpoint.offset);
        this.writtenOffset = checkpoint.offset;
        this.lastSyncedOffset = checkpoint.offset;
        this.operationCounter = checkpoint.numOps;
        this.minSeqNo = checkpoint.minSeqNo;
        this.maxSeqNo = checkpoint.maxSeqNo;
        this.checkpointSeqNoServiceService = new LocalCheckpointService(shardId, minSeqNo, checkpoint.localCheckpoint, checkpointArraySize);
    }

    public static TranslogWriter create(Type type, ShardId shardId, String translogUUID, long fileGeneration, long minSeqNo,
                                        Path file, int bufferSize, int checkpointArraySize,
                                        ChannelFactory channelFactory) throws IOException {
        final BytesRef ref = new BytesRef(translogUUID);
        final int headerLength = CodecUtil.headerLength(TRANSLOG_CODEC) + ref.length + RamUsageEstimator.NUM_BYTES_INT;
        final FileChannel channel = channelFactory.create(file);
        try {
            // This OutputStreamDataOutput is intentionally not closed because
            // closing it will close the FileChannel
            final OutputStreamDataOutput out = new OutputStreamDataOutput(java.nio.channels.Channels.newOutputStream(channel));
            CodecUtil.writeHeader(out, TRANSLOG_CODEC, VERSION);
            out.writeInt(ref.length);
            out.writeBytes(ref.bytes, ref.offset, ref.length);
            channel.force(false);
            Checkpoint checkpoint = new Checkpoint(headerLength, 0, fileGeneration, minSeqNo, SequenceNumbersService.NO_OPS_PERFORMED, minSeqNo - 1);
            writeCheckpoint(checkpoint, file.getParent(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            final TranslogWriter writer = type.create(shardId, checkpoint, new ChannelReference(file, fileGeneration, channel), bufferSize, checkpointArraySize);
            return writer;
        } catch (Throwable throwable) {
            IOUtils.closeWhileHandlingException(channel);
            try {
                Files.delete(file); // remove the file as well
            } catch (IOException ex) {
                throwable.addSuppressed(ex);
            }
            throw throwable;
        }
    }

    public static TranslogWriter open(Type type, ShardId shardId, Checkpoint checkpoint, Path file, String translogUUID,
                                      int bufferSize, int checkpointArraySize, ChannelFactory channelFactory) throws IOException {
        final FileChannel channel = channelFactory.open(file);
        try {
            // check that the file is in the right format (TODO: find a cleaner option)
            ImmutableTranslogReader reader = ImmutableTranslogReader.open(new ChannelReference(file, checkpoint.generation, channel), checkpoint, translogUUID);
            final TranslogWriter writer = type.open(shardId, reader, bufferSize, checkpointArraySize);
            return writer;
        } catch (Throwable throwable) {
            IOUtils.closeWhileHandlingException(channel);
            throw throwable;
        }
    }


    /**
     * If this {@code TranslogWriter} was closed as a side-effect of a tragic exception,
     * e.g. disk full while flushing a new segment, this returns the root cause exception.
     * Otherwise (no tragic exception has occurred) it returns null.
     */
    public Throwable getTragicException() {
        return tragedy;
    }


    public boolean pendingWrites() {
        return maxSeqNo != getLocalCheckpoint();
    }

    public enum Type {

        SIMPLE() {
            @Override
            public TranslogWriter create(ShardId shardId, Checkpoint checkpoint, ChannelReference channelReference, int bufferSize, int checkpointArraySize) throws IOException {
                return new TranslogWriter(shardId, checkpoint, channelReference, checkpointArraySize);
            }

            @Override
            public TranslogWriter open(ShardId shardId, ImmutableTranslogReader reader, int bufferSize, int checkpointArraySize) throws IOException {
                return new TranslogWriter(shardId, reader, checkpointArraySize);
            }
        },
        BUFFERED() {
            @Override
            public TranslogWriter create(ShardId shardId, Checkpoint checkpoint, ChannelReference channelReference, int bufferSize, int checkpointArraySize) throws IOException {
                return new BufferingTranslogWriter(shardId, checkpoint, channelReference, bufferSize, checkpointArraySize);
            }

            @Override
            public TranslogWriter open(ShardId shardId, ImmutableTranslogReader reader, int bufferSize, int checkpointArraySize) throws IOException {
                return new BufferingTranslogWriter(shardId, reader, bufferSize, checkpointArraySize);
            }
        };

        public abstract TranslogWriter create(ShardId shardId, Checkpoint checkpoint, ChannelReference raf, int bufferSize, int checkpointArraySize) throws IOException;

        public static Type fromString(String type) {
            if (SIMPLE.name().equalsIgnoreCase(type)) {
                return SIMPLE;
            } else if (BUFFERED.name().equalsIgnoreCase(type)) {
                return BUFFERED;
            }
            throw new IllegalArgumentException("No translog fs type [" + type + "]");
        }

        public abstract TranslogWriter open(ShardId shardId, ImmutableTranslogReader reader, int bufferSize, int checkpointArraySize) throws IOException;
    }

    protected final void closeWithTragicEvent(Throwable throwable) throws IOException {
        try (ReleasableLock lock = writeLock.acquire()) {
            if (tragedy == null) {
                tragedy = throwable;
            } else {
                tragedy.addSuppressed(throwable);
            }
            close();
        }
    }

    /**
     * add the given bytes to the translog and return the location they were written at
     */
    public Translog.Location add(long seqNo, BytesReference data) throws IOException {
        final long position;
        try (ReleasableLock lock = writeLock.acquire()) {
            ensureOpen();
            position = writtenOffset;
            try {
                data.writeTo(channel);
            } catch (Throwable e) {
                closeWithTragicEvent(e);
                throw e;
            }
            checkpointSeqNoServiceService.markSeqNoAsCompleted(seqNo);
            maxSeqNo = Math.max(maxSeqNo, seqNo);
            writtenOffset = writtenOffset + data.length();
            operationCounter++;
        }
        return new Translog.Location(getGeneration(), position, data.length());
    }

    /**
     * change the size of the internal buffer if relevant
     */
    public void updateBufferSize(int bufferSize) throws TranslogException {
    }

    @Override
    public Translog.Snapshot newSnapshot() {
        try (ReleasableLock lock = writeLock.acquire()) {
            // must make sure things stay consistent & are available via the channel
            flush();
            return super.newSnapshot();
        } catch (IOException e) {
            try {
                closeWithTragicEvent(e);
            } catch (IOException closeException) {
                e.addSuppressed(closeException);
            }
            throw new TranslogException(shardId, "exception while creating an immutable reader", e);
        }
    }
    /**
     * write all buffered ops to disk and fsync file
     */
    public synchronized void sync() throws IOException { // synchronized to ensure only one sync happens a time
        // check if we really need to sync here...
        if (syncNeeded()) {
            try (ReleasableLock lock = writeLock.acquire()) {
                ensureOpen();
                checkpoint(writtenOffset, operationCounter, minSeqNo, maxSeqNo, checkpointSeqNoServiceService.getCheckpoint(), channelReference);
                lastSyncedOffset = writtenOffset;
            }
        }
    }

    /**
     * returns true if there are buffered ops
     */
    public boolean syncNeeded() {
        return writtenOffset != lastSyncedOffset; // by default nothing is buffered
    }

    @Override
    public int totalOperations() {
        return operationCounter;
    }

    @Override
    public long getMinSeqNo() {
        return minSeqNo;
    }

    @Override
    public long getMaxSeqNo() {
        return maxSeqNo;
    }

    @Override
    public long getLocalCheckpoint() {
        return checkpointSeqNoServiceService.getCheckpoint();
    }

    @Override
    public long sizeInBytes() {
        return writtenOffset;
    }


    /**
     * Flushes the buffer if the translog is buffered.
     */
    protected void flush() throws IOException {
    }

    public ImmutableTranslogReader closeIntoReader() throws TranslogException {
        try (ReleasableLock ignore = writeLock.acquire()) {
            ensureOpen();
            sync();
            closed.set(true);
            Checkpoint checkpoint = new Checkpoint(writtenOffset, operationCounter, getGeneration(), minSeqNo, maxSeqNo, getLocalCheckpoint());
            ImmutableTranslogReader reader = new ImmutableTranslogReader(checkpoint, channelReference, getFirstOperationOffset());
            return reader;
        } catch (Exception e) {
            throw new TranslogException(shardId, "exception while creating an immutable reader", e);
        }
    }

    boolean assertBytesAtLocation(Translog.Location location, BytesReference expectedBytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(location.size);
        readBytes(buffer, location.translogLocation);
        return new BytesArray(buffer.array()).equals(expectedBytes);
    }

    /**
     * Syncs the translog up to at least the given offset unless already synced
     *
     * @return <code>true</code> if this call caused an actual sync operation
     */
    public boolean syncUpTo(long offset) throws IOException {
        if (lastSyncedOffset < offset) {
            sync();
            return true;
        }
        return false;
    }

    @Override
    protected void readBytes(ByteBuffer buffer, long position) throws IOException {
        try (ReleasableLock lock = readLock.acquire()) {
            Channels.readFromFileChannelWithEofException(channel, position, buffer);
        }
    }

    protected synchronized void checkpoint(long lastSyncPosition, int operationCounter, long minSeqNo, long maxSeqNo, long localCheckpoint, ChannelReference channelReference) throws IOException {
        channelReference.getChannel().force(false);
        writeCheckpoint(lastSyncPosition, operationCounter, channelReference.getPath().getParent(), channelReference.getGeneration(), minSeqNo, maxSeqNo, localCheckpoint, StandardOpenOption.WRITE);
    }

    private static void writeCheckpoint(long syncPosition, int numOperations, Path translogFile, long generation, long minSeqNo, long maxSeqNo, long localCheckpoint, OpenOption... options) throws IOException {
        Checkpoint checkpoint = new Checkpoint(syncPosition, numOperations, generation, minSeqNo, maxSeqNo, localCheckpoint);
        writeCheckpoint(checkpoint, translogFile, options);
    }

    private static void writeCheckpoint(Checkpoint checkpoint, Path translogFile, OpenOption... options) throws IOException {
        final Path checkpointFile = translogFile.resolve(Translog.getCommitCheckpointFileName(checkpoint.generation));
        Checkpoint.write(checkpointFile, checkpoint, options);
    }

    static class ChannelFactory {

        static final ChannelFactory DEFAULT = new ChannelFactory();

        // only for testing until we have a disk-full FileSystemt
        public FileChannel create(Path file) throws IOException {
            return FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE_NEW);
        }

        public FileChannel open(Path file) throws IOException {
            return FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ);
        }
    }

    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            channelReference.decRef();
        }
    }

    protected final boolean isClosed() {
        return closed.get();
    }

    protected final void ensureOpen() {
        if (isClosed()) {
            throw new AlreadyClosedException("translog [" + getGeneration() + "] is already closed", tragedy);
        }
    }
}
