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

import com.carrotsearch.randomizedtesting.generators.RandomPicks;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.mockfile.FilterFileChannel;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.MockDirectoryWrapper;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.LineFileDocs;
import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.IndexSettingsModule;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 *
 */
@LuceneTestCase.SuppressFileSystems("ExtrasFS")
public class TranslogTests extends ESTestCase {

    protected final ShardId shardId = new ShardId(new Index("index"), 1);

    protected Translog translog;
    protected Path translogDir;
    protected Translog.View uncommittedView;

    @Override
    protected void afterIfSuccessful() throws Exception {
        super.afterIfSuccessful();

        if (translog.isOpen()) {
            final long currentGen = translog.currentFileGeneration();
            if (currentGen > 1) {
                uncommittedView.incMinGeneration(currentGen);
                assertFileDeleted(translog, currentGen - 1);
            }
            translog.close();
        }
        assertFileIsPresent(translog, translog.currentFileGeneration());
        IOUtils.rm(translog.location()); // delete all the locations

    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // if a previous test failed we clean up things here
        translogDir = createTempDir();
        translog = create(translogDir);
        uncommittedView = translog.newView();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        try {
            if (translog.isOpen()) {
                assertEquals("there are still open views (other than the uncommitted view)", 1, translog.getNumOpenViews());
                translog.close();
            }
        } finally {
            super.tearDown();
        }
    }

    private Translog create(Path path) throws IOException {
        return new Translog(getTranslogConfig(path));
    }

    protected TranslogConfig getTranslogConfig(Path path) {
        Settings build = Settings.settingsBuilder()
                .put(TranslogConfig.INDEX_TRANSLOG_FS_TYPE, TranslogWriter.Type.SIMPLE.name())
                .put(IndexMetaData.SETTING_VERSION_CREATED, org.elasticsearch.Version.CURRENT)
                .build();
        return new TranslogConfig(shardId, path, IndexSettingsModule.newIndexSettings(shardId.index(), build), Translog.Durabilty.REQUEST, BigArrays.NON_RECYCLING_INSTANCE, null);
    }

    protected void commit() throws IOException {
        commit(translog, uncommittedView);
    }

    protected void commit(Translog translog, Translog.View uncommittedView) throws IOException {
        long newGen = translog.startNewGeneration();
        uncommittedView.incMinGeneration(newGen);
    }

    protected Checkpoint readLastCheckpoint() throws IOException {
        List<Checkpoint> checkpoints = translog.readOrderedCheckpoints();
        assertThat(checkpoints.size(), greaterThan(0));
        return checkpoints.get(checkpoints.size() - 1);
    }

    protected void addToTranslogAndList(Translog translog, ArrayList<Translog.Operation> list, Translog.Operation op) throws IOException {
        list.add(op);
        translog.add(op);
    }

    public void testIdParsingFromFile() {
        long id = randomIntBetween(0, Integer.MAX_VALUE);
        Path file = translogDir.resolve(Translog.TRANSLOG_FILE_PREFIX + id + ".tlog");
        assertThat(Translog.parseIdFromFileName(file), equalTo(id));

        id = randomIntBetween(0, Integer.MAX_VALUE);
        file = translogDir.resolve(Translog.TRANSLOG_FILE_PREFIX + id);
        try {
            Translog.parseIdFromFileName(file);
            fail("invalid pattern");
        } catch (IllegalArgumentException ex) {
            // all good
        }

        file = translogDir.resolve(Translog.TRANSLOG_FILE_PREFIX + id + ".recovering");
        try {
            Translog.parseIdFromFileName(file);
            fail("invalid pattern");
        } catch (IllegalArgumentException ex) {
            // all good
        }

        file = translogDir.resolve(Translog.TRANSLOG_FILE_PREFIX + randomNonTranslogPatternString(1, 10) + id);
        try {
            Translog.parseIdFromFileName(file);
            fail("invalid pattern");
        } catch (IllegalArgumentException ex) {
            // all good
        }
        file = translogDir.resolve(randomNonTranslogPatternString(1, Translog.TRANSLOG_FILE_PREFIX.length() - 1));
        try {
            Translog.parseIdFromFileName(file);
            fail("invalid pattern");
        } catch (IllegalArgumentException ex) {
            // all good
        }
    }

    private String randomNonTranslogPatternString(int min, int max) {
        String string;
        boolean validPathString;
        do {
            validPathString = false;
            string = randomRealisticUnicodeOfCodepointLength(randomIntBetween(min, max));
            try {
                final Path resolved = translogDir.resolve(string);
                // some strings (like '/' , '..') do not refer to a file, which we this method should return
                validPathString = resolved.getFileName() != null;
            } catch (InvalidPathException ex) {
                // some FS don't like our random file names -- let's just skip these random choices
            }
        } while (Translog.PARSE_STRICT_ID_PATTERN.matcher(string).matches() || validPathString == false);
        return string;
    }

    public void testRead() throws IOException {
        Translog.Location loc1 = translog.add(new Translog.Index("test", "1", 0, new byte[]{1}));
        Translog.Location loc2 = translog.add(new Translog.Index("test", "2", 1, new byte[]{2}));
        assertThat(translog.read(loc1).getSource().source.toBytesArray(), equalTo(new BytesArray(new byte[]{1})));
        assertThat(translog.read(loc2).getSource().source.toBytesArray(), equalTo(new BytesArray(new byte[]{2})));
        translog.sync();
        assertThat(translog.read(loc1).getSource().source.toBytesArray(), equalTo(new BytesArray(new byte[]{1})));
        assertThat(translog.read(loc2).getSource().source.toBytesArray(), equalTo(new BytesArray(new byte[]{2})));
        Translog.Location loc3 = translog.add(new Translog.Index("test", "2", 2, new byte[]{3}));
        assertThat(translog.read(loc3).getSource().source.toBytesArray(), equalTo(new BytesArray(new byte[]{3})));
        translog.sync();
        assertThat(translog.read(loc3).getSource().source.toBytesArray(), equalTo(new BytesArray(new byte[]{3})));
        translog.startNewGeneration();
        assertThat(translog.read(loc3).getSource().source.toBytesArray(), equalTo(new BytesArray(new byte[]{3})));
        commit();
        assertNull(translog.read(loc1));
        assertNull(translog.read(loc2));
        assertNull(translog.read(loc3));
        try {
            translog.read(new Translog.Location(translog.currentFileGeneration() + 1, 17, 35));
            fail("generation is greater than the current");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    public void testSimpleOperations() throws IOException {
        ArrayList<Translog.Operation> ops = new ArrayList<>();
        Translog.Snapshot snapshot = uncommittedView.snapshot();
        assertThat(snapshot, SnapshotMatchers.size(0));

        addToTranslogAndList(translog, ops, new Translog.Index("test", "1", 0, new byte[]{1}));
        snapshot = uncommittedView.snapshot();
        assertThat(snapshot, SnapshotMatchers.equalsTo(ops));
        assertThat(snapshot.estimatedTotalOperations(), equalTo(ops.size()));

        addToTranslogAndList(translog, ops, new Translog.Delete(newUid("2"), 1));
        snapshot = uncommittedView.snapshot();
        assertThat(snapshot, SnapshotMatchers.equalsTo(ops));
        assertThat(snapshot.estimatedTotalOperations(), equalTo(ops.size()));

        snapshot = uncommittedView.snapshot();

        Translog.Index index = (Translog.Index) snapshot.next();
        assertThat(index != null, equalTo(true));
        assertThat(index.source().toBytes(), equalTo(new byte[]{1}));

        Translog.Delete delete = (Translog.Delete) snapshot.next();
        assertThat(delete != null, equalTo(true));
        assertThat(delete.uid(), equalTo(newUid("2")));

        assertThat(snapshot.next(), equalTo(null));

        long firstId = translog.currentFileGeneration();
        translog.startNewGeneration();
        assertThat(translog.currentFileGeneration(), Matchers.not(equalTo(firstId)));

        snapshot = uncommittedView.snapshot();
        assertThat(snapshot, SnapshotMatchers.equalsTo(ops));
        assertThat(snapshot.estimatedTotalOperations(), equalTo(ops.size()));

        commit();
        snapshot = uncommittedView.snapshot();
        assertThat(snapshot, SnapshotMatchers.size(0));
        assertThat(snapshot.estimatedTotalOperations(), equalTo(0));
    }

    protected TranslogStats stats() throws IOException {
        // force flushing and updating of stats
        translog.sync();
        TranslogStats stats = translog.stats();
        if (randomBoolean()) {
            BytesStreamOutput out = new BytesStreamOutput();
            stats.writeTo(out);
            StreamInput in = StreamInput.wrap(out.bytes());
            stats = new TranslogStats();
            stats.readFrom(in);
        }
        return stats;
    }

    public void testStats() throws IOException {
        TranslogStats stats = stats();
        assertThat(stats.numberOfOperations(), equalTo(0l));
        final long emptySize = stats.getTranslogSizeInBytes();
        assertThat((int) emptySize, greaterThan(CodecUtil.headerLength(TranslogWriter.TRANSLOG_CODEC)));
        TranslogStats total = new TranslogStats();
        translog.add(new Translog.Index("test", "1", 0, new byte[]{1}));
        stats = stats();
        total.add(stats);
        assertThat(stats.numberOfOperations(), equalTo(1l));
        assertThat(stats.getTranslogSizeInBytes(), greaterThan(emptySize));
        long lastSize = stats.getTranslogSizeInBytes();

        translog.add(new Translog.Delete(newUid("2"), 1));
        stats = stats();
        total.add(stats);
        assertThat(stats.numberOfOperations(), equalTo(2l));
        assertThat(stats.getTranslogSizeInBytes(), greaterThan(lastSize));
        lastSize = stats.getTranslogSizeInBytes();

        translog.add(new Translog.Delete(newUid("3"), 2));
        long newGen = translog.startNewGeneration();
        stats = stats();
        total.add(stats);
        assertThat(stats.numberOfOperations(), equalTo(3l));
        assertThat(stats.getTranslogSizeInBytes(), greaterThan(lastSize));

        uncommittedView.incMinGeneration(newGen);
        stats = stats();
        total.add(stats);
        assertThat(stats.numberOfOperations(), equalTo(0l));
        assertThat(stats.getTranslogSizeInBytes(), equalTo(emptySize));
        assertEquals(6, total.numberOfOperations());
        assertEquals(437, total.getTranslogSizeInBytes());

        BytesStreamOutput out = new BytesStreamOutput();
        total.writeTo(out);
        TranslogStats copy = new TranslogStats();
        copy.readFrom(StreamInput.wrap(out.bytes()));

        assertEquals(6, copy.numberOfOperations());
        assertEquals(437, copy.getTranslogSizeInBytes());
        assertEquals("\"translog\"{\n" +
                "  \"operations\" : 6,\n" +
                "  \"size_in_bytes\" : 437\n" +
                "}", copy.toString().trim());

        try {
            new TranslogStats(1, -1, 0, 0);
            fail("must be positive");
        } catch (IllegalArgumentException ex) {
            //all well
        }
        try {
            new TranslogStats(-1, 1, 0, 0);
            fail("must be positive");
        } catch (IllegalArgumentException ex) {
            //all well
        }
        try {
            new TranslogStats(1, 1, -3, 0);
            fail("must be positive");
        } catch (IllegalArgumentException ex) {
            //all well
        }
        try {
            new TranslogStats(1, 1, 0, -3);
            fail("must be positive");
        } catch (IllegalArgumentException ex) {
            //all well
        }
    }

    public void testSnapshot() throws IOException {
        ArrayList<Translog.Operation> ops = new ArrayList<>();
        Translog.Snapshot snapshot = uncommittedView.snapshot();
        assertThat(snapshot, SnapshotMatchers.size(0));

        addToTranslogAndList(translog, ops, new Translog.Index("test", "1", 0, new byte[]{1}));

        snapshot = uncommittedView.snapshot();
        assertThat(snapshot, SnapshotMatchers.equalsTo(ops));
        assertThat(snapshot.estimatedTotalOperations(), equalTo(1));

        // snapshot while another is open
        snapshot = uncommittedView.snapshot();
        Translog.Snapshot snapshot1 = translog.newSnapshot();
        assertThat(snapshot, SnapshotMatchers.equalsTo(ops));
        assertThat(snapshot.estimatedTotalOperations(), equalTo(1));

        assertThat(snapshot1, SnapshotMatchers.size(1));
        assertThat(snapshot1.estimatedTotalOperations(), equalTo(1));
    }

    public void testSnapshotWithNewTranslog() throws IOException {
        ArrayList<Translog.Operation> ops = new ArrayList<>();
        Translog.Snapshot snapshot = uncommittedView.snapshot();
        assertThat(snapshot, SnapshotMatchers.size(0));

        addToTranslogAndList(translog, ops, new Translog.Index("test", "1", 0, new byte[]{1}));
        Translog.Snapshot snapshot1 = uncommittedView.snapshot();

        addToTranslogAndList(translog, ops, new Translog.Index("test", "2", 1, new byte[]{2}));

        long gen = translog.startNewGeneration();
        addToTranslogAndList(translog, ops, new Translog.Index("test", "3", 2, new byte[]{3}));

        Translog.View view2 = translog.newView();
        uncommittedView.incMinGeneration(gen);
        Translog.Snapshot snapshot2 = view2.snapshot();
        assertThat(snapshot2, SnapshotMatchers.equalsTo(ops));
        assertThat(snapshot2.estimatedTotalOperations(), equalTo(ops.size()));


        assertThat(snapshot1, SnapshotMatchers.equalsTo(ops.get(0)));
        view2.close();
    }

    public void testSnapshotOnClosedTranslog() throws IOException {
        assertTrue(Files.exists(translogDir.resolve(Translog.getFilename(1))));
        translog.add(new Translog.Index("test", "1", 0, new byte[]{1}));
        translog.close();
        try {
            Translog.Snapshot snapshot = translog.newSnapshot();
            fail("translog is closed");
        } catch (AlreadyClosedException ex) {
            assertEquals(ex.getMessage(), "translog is already closed");
        }
    }

    public void testDeleteOnSnapshotRelease() throws Exception {
        ArrayList<Translog.Operation> firstOps = new ArrayList<>();
        addToTranslogAndList(translog, firstOps, new Translog.Index("test", "1", 0, new byte[]{1}));

        Translog.View firstView = translog.newView();
        Translog.Snapshot firstSnapshot = firstView.snapshot();
        assertThat(firstView.totalOperations(), equalTo(1));
        assertThat(firstSnapshot.estimatedTotalOperations(), equalTo(1));
        commit();
        assertFileIsPresent(translog, 1);


        ArrayList<Translog.Operation> secOps = new ArrayList<>();
        addToTranslogAndList(translog, secOps, new Translog.Index("test", "2", 1, new byte[]{2}));
        assertThat(firstSnapshot.estimatedTotalOperations(), equalTo(1));

        Translog.View secondView = translog.newView();
        secondView.incMinGeneration(2);
        Translog.Snapshot secondSnapshot = secondView.snapshot();
        translog.add(new Translog.Index("test", "3", 2, new byte[]{3}));
        assertThat(secondSnapshot, SnapshotMatchers.equalsTo(secOps));
        assertThat(secondSnapshot.estimatedTotalOperations(), equalTo(1));
        assertFileIsPresent(translog, 1);
        assertFileIsPresent(translog, 2);

        firstView.close();
        assertFileDeleted(translog, 1);
        assertFileIsPresent(translog, 2);
        secondView.close();
        assertFileIsPresent(translog, 2); // it's the current nothing should be deleted
        commit();
        assertFileIsPresent(translog, 3); // it's the current nothing should be deleted
        assertFileDeleted(translog, 2);

    }


    public void assertFileIsPresent(Translog translog, long id) {
        if (Files.exists(translogDir.resolve(Translog.getFilename(id)))) {
            return;
        }
        fail(Translog.getFilename(id) + " is not present in any location: " + translog.location());
    }

    public void assertFileDeleted(Translog translog, long id) {
        assertFalse("translog [" + id + "] still exists", Files.exists(translog.location().resolve(Translog.getFilename(id))));
    }

    public void testConcurrentWritesWithVaryingSize() throws Throwable {
        final int opsPerThread = randomIntBetween(10, 200);
        int threadCount = 2 + randomInt(5);

        logger.info("testing with [{}] threads, each doing [{}] ops", threadCount, opsPerThread);
        final BlockingQueue<Translog.Operation> writtenOperations = new ArrayBlockingQueue<>(threadCount * opsPerThread);
        Path tempDir = createTempDir();
        TranslogConfig config = getTranslogConfig(tempDir);
        Translog translog = new Translog(config);

        Thread[] threads = new Thread[threadCount];
        final Throwable[] threadExceptions = new Throwable[threadCount];
        final CountDownLatch downLatch = new CountDownLatch(1);
        final AtomicLong seqNoGenerator = new AtomicLong();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new TranslogThread(translog, downLatch, opsPerThread, threadId, writtenOperations, threadExceptions, seqNoGenerator);
            threads[i].setDaemon(true);
            threads[i].start();
        }

        downLatch.countDown();

        for (int i = 0; i < threadCount; i++) {
            if (threadExceptions[i] != null) {
                throw threadExceptions[i];
            }
            threads[i].join(60 * 1000);
        }

        for (Translog.Operation expectedOp : writtenOperations) {
            Translog.Operation op = translog.read(expectedOp.location());
            assertEquals(expectedOp.opType(), op.opType());
            switch (op.opType()) {
                case INDEX:
                    Translog.Index indexOp = (Translog.Index) op;
                    Translog.Index expIndexOp = (Translog.Index) expectedOp;
                    assertEquals(expIndexOp.id(), indexOp.id());
                    assertEquals(expIndexOp.routing(), indexOp.routing());
                    assertEquals(expIndexOp.type(), indexOp.type());
                    assertEquals(expIndexOp.source(), indexOp.source());
                    assertEquals(expIndexOp.version(), indexOp.version());
                    assertEquals(expIndexOp.versionType(), indexOp.versionType());
                    break;
                case DELETE:
                    Translog.Delete delOp = (Translog.Delete) op;
                    Translog.Delete expDelOp = (Translog.Delete) expectedOp;
                    assertEquals(expDelOp.uid(), delOp.uid());
                    assertEquals(expDelOp.version(), delOp.version());
                    assertEquals(expDelOp.versionType(), delOp.versionType());
                    break;
                case FAILURE:
                    Translog.Failure failOp = (Translog.Failure) op;
                    Translog.Failure expFailOp = (Translog.Failure) expectedOp;
                    assertEquals(expFailOp.seqNo(), failOp.seqNo());
                    assertEquals(expFailOp.description(), failOp.description());
                    break;
                default:
                    throw new ElasticsearchException("unsupported opType");
            }

        }
        translog.close();
    }

    public void testTranslogChecksums() throws Exception {
        List<Translog.Location> locations = new ArrayList<>();

        int translogOperations = randomIntBetween(10, 100);
        for (int op = 0; op < translogOperations; op++) {
            String ascii = randomAsciiOfLengthBetween(1, 50);
            locations.add(translog.add(new Translog.Index("test", "" + op, op, ascii.getBytes("UTF-8"))));
        }
        translog.sync();

        corruptTranslogs(translogDir);

        AtomicInteger corruptionsCaught = new AtomicInteger(0);
        for (Translog.Location location : locations) {
            try {
                translog.read(location);
            } catch (TranslogCorruptedException e) {
                corruptionsCaught.incrementAndGet();
            }
        }
        assertThat("at least one corruption was caused and caught", corruptionsCaught.get(), greaterThanOrEqualTo(1));
    }

    public void testTruncatedTranslogs() throws Exception {
        List<Translog.Location> locations = new ArrayList<>();

        int translogOperations = randomIntBetween(10, 100);
        for (int op = 0; op < translogOperations; op++) {
            String ascii = randomAsciiOfLengthBetween(1, 50);
            locations.add(translog.add(new Translog.Index("test", "" + op, op, ascii.getBytes("UTF-8"))));
        }
        translog.sync();

        truncateTranslogs(translogDir);

        AtomicInteger truncations = new AtomicInteger(0);
        for (Translog.Location location : locations) {
            try {
                translog.read(location);
            } catch (ElasticsearchException e) {
                if (e.getCause() instanceof EOFException) {
                    truncations.incrementAndGet();
                } else {
                    throw e;
                }
            }
        }
        assertThat("at least one truncation was caused and caught", truncations.get(), greaterThanOrEqualTo(1));
    }

    /**
     * Randomly truncate some bytes in the translog files
     */
    private void truncateTranslogs(Path directory) throws Exception {
        Path[] files = FileSystemUtils.files(directory, "translog-*");
        for (Path file : files) {
            try (FileChannel f = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                long prevSize = f.size();
                long newSize = prevSize - randomIntBetween(1, (int) prevSize / 2);
                logger.info("--> truncating {}, prev: {}, now: {}", file, prevSize, newSize);
                f.truncate(newSize);
            }
        }
    }


    /**
     * Randomly overwrite some bytes in the translog files
     */
    private void corruptTranslogs(Path directory) throws Exception {
        Path[] files = FileSystemUtils.files(directory, "translog-*");
        for (Path file : files) {
            logger.info("--> corrupting {}...", file);
            FileChannel f = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
            int corruptions = scaledRandomIntBetween(10, 50);
            for (int i = 0; i < corruptions; i++) {
                // note: with the current logic, this will sometimes be a no-op
                long pos = randomIntBetween(0, (int) f.size());
                ByteBuffer junk = ByteBuffer.wrap(new byte[]{randomByte()});
                f.write(junk, pos);
            }
            f.close();
        }
    }

    private Term newUid(String id) {
        return new Term("_uid", id);
    }

    public void testVerifyTranslogIsNotDeleted() throws IOException {
        assertFileIsPresent(translog, 1);
        translog.add(new Translog.Index("test", "1", 0, new byte[]{1}));
        Translog.View view = translog.newView();
        assertThat(view.snapshot(), SnapshotMatchers.size(1));
        assertFileIsPresent(translog, 1);
        assertThat(view.totalOperations(), equalTo(1));
        if (randomBoolean()) {
            translog.close();
            view.close();
        } else {
            view.close();
            translog.close();
        }

        assertFileIsPresent(translog, 1);
    }

    /** Tests that concurrent readers and writes maintain view and snapshot semantics */
    public void testConcurrentWriteViewsAndSnapshot() throws Throwable {
        final Thread[] writers = new Thread[randomIntBetween(1, 10)];
        final Thread[] readers = new Thread[randomIntBetween(1, 10)];
        final int flushEveryOps = randomIntBetween(5, 100);
        // used to notify main thread that so many operations have been written so it can simulate a flush
        final AtomicReference<CountDownLatch> writtenOpsLatch = new AtomicReference<>(new CountDownLatch(0));
        final AtomicLong idGenerator = new AtomicLong();
        final CyclicBarrier barrier = new CyclicBarrier(writers.length + readers.length + 1);

        // a map of all written ops and their returned location.
        final Map<Translog.Operation, Translog.Location> writtenOps = ConcurrentCollections.newConcurrentMap();

        // a signal for all threads to stop
        final AtomicBoolean run = new AtomicBoolean(true);

        // any errors on threads
        final List<Throwable> errors = new CopyOnWriteArrayList<>();
        logger.debug("using [{}] readers. [{}] writers. flushing every ~[{}] ops.", readers.length, writers.length, flushEveryOps);
        final AtomicLong seqNoGenerator = new AtomicLong();
        for (int i = 0; i < writers.length; i++) {
            final String threadId = "writer_" + i;
            writers[i] = new Thread(new AbstractRunnable() {
                @Override
                public void doRun() throws BrokenBarrierException, InterruptedException, IOException {
                    barrier.await();
                    int counter = 0;
                    while (run.get()) {
                        long id = idGenerator.incrementAndGet();
                        final Translog.Operation op;
                        switch (Translog.Operation.Type.values()[((int) (id % Translog.Operation.Type.values().length))]) {
                            case CREATE:
                            case INDEX:
                                op = new Translog.Index("type", "" + id, seqNoGenerator.getAndIncrement(), new byte[]{(byte) id});
                                break;
                            case DELETE:
                                op = new Translog.Delete(newUid("" + id), seqNoGenerator.getAndIncrement());
                                break;
                            case FAILURE:
                                op = new Translog.Failure(seqNoGenerator.getAndIncrement(), "_FAKE_FAILURE");
                                break;
                            default:
                                throw new ElasticsearchException("unknown type");
                        }
                        Translog.Location location = translog.add(op);
                        Translog.Location existing = writtenOps.put(op, location);
                        if (existing != null) {
                            fail("duplicate op [" + op + "], old entry at " + location);
                        }
                        writtenOpsLatch.get().countDown();
                        counter++;
                    }
                    logger.debug("--> [{}] done. wrote [{}] ops.", threadId, counter);
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("--> writer [{}] had an error", t, threadId);
                    errors.add(t);
                }
            }, threadId);
            writers[i].start();
        }

        for (int i = 0; i < readers.length; i++) {
            final String threadId = "reader_" + i;
            readers[i] = new Thread(new AbstractRunnable() {
                Translog.View view = null;
                Set<Translog.Operation> writtenOpsAtView;

                @Override
                public void onFailure(Throwable t) {
                    logger.error("--> reader [{}] had an error", t, threadId);
                    errors.add(t);
                    closeView();
                }

                void closeView() {
                    if (view != null) {
                        view.close();
                    }
                }

                void newView() {
                    closeView();
                    view = translog.newView();
                    // captures the currently written ops so we know what to expect from the view
                    writtenOpsAtView = new HashSet<>(writtenOps.keySet());
                    logger.debug("--> [{}] opened view from [{}]", threadId, view.minTranslogGeneration());
                }

                @Override
                protected void doRun() throws Exception {
                    barrier.await();
                    int iter = 0;
                    while (run.get()) {
                        if (iter++ % 10 == 0) {
                            newView();
                        }

                        // captures al views that are written since the view was created (with a small caveat see bellow)
                        // these are what we expect the snapshot to return (and potentially some more).
                        Set<Translog.Operation> expectedOps = new HashSet<>(writtenOps.keySet());
                        expectedOps.removeAll(writtenOpsAtView);
                        Translog.Snapshot snapshot = view.snapshot();
                        Translog.Operation op;
                        while ((op = snapshot.next()) != null) {
                            expectedOps.remove(op);
                        }
                        if (expectedOps.isEmpty() == false) {
                            StringBuilder missed = new StringBuilder("missed ").append(expectedOps.size()).append(" operations");
                            boolean failed = false;
                            for (Translog.Operation expectedOp : expectedOps) {
                                final Translog.Location loc = writtenOps.get(expectedOp);
                                if (loc.generation < view.minTranslogGeneration()) {
                                    // writtenOps is only updated after the op was written to the translog. This mean
                                    // that ops written to the translog before the view was taken (and will be missing from the view)
                                    // may yet be available in writtenOpsAtView, meaning we will erroneously expect them
                                    continue;
                                }
                                failed = true;
                                missed.append("\n --> [").append(expectedOp).append("] written at ").append(loc);
                            }
                            if (failed) {
                                fail(missed.toString());
                            }
                        }
                        // slow down things a bit and spread out testing..
                        writtenOpsLatch.get().await(200, TimeUnit.MILLISECONDS);
                    }
                    closeView();
                    logger.debug("--> [{}] done. tested [{}] snapshots", threadId, iter);
                }
            }, threadId);
            readers[i].start();
        }

        barrier.await();
        try {
            for (int iterations = scaledRandomIntBetween(10, 200); iterations > 0 && errors.isEmpty(); iterations--) {
                writtenOpsLatch.set(new CountDownLatch(flushEveryOps));
                while (writtenOpsLatch.get().await(200, TimeUnit.MILLISECONDS) == false) {
                    if (errors.size() > 0) {
                        break;
                    }
                }
                commit();
            }
        } finally {
            run.set(false);
            logger.debug("--> waiting for threads to stop");
            for (Thread thread : writers) {
                thread.join();
            }
            for (Thread thread : readers) {
                thread.join();
            }
            if (errors.size() > 0) {
                Throwable e = errors.get(0);
                for (Throwable suppress : errors.subList(1, errors.size())) {
                    e.addSuppressed(suppress);
                }
                throw e;
            }
            logger.info("--> test done. total ops written [{}]", writtenOps.size());
        }
    }


    public void testSyncUpTo() throws IOException {
        int translogOperations = randomIntBetween(10, 100);
        int count = 0;
        for (int op = 0; op < translogOperations; op++) {
            count++;
            final Translog.Location location = translog.add(new Translog.Index("test", "" + op, count - 1, Integer.toString(count).getBytes(Charset.forName("UTF-8"))));
            if (randomBoolean()) {
                assertTrue("at least one operation pending", translog.syncNeeded());
                assertTrue("this operation has not been synced", translog.ensureSynced(location));
                assertFalse("the last call to ensureSycned synced all previous ops", translog.syncNeeded()); // we are the last location so everything should be synced
                count++;
                translog.add(new Translog.Index("test", "" + op, count - 1, Integer.toString(count).getBytes(Charset.forName("UTF-8"))));
                assertTrue("one pending operation", translog.syncNeeded());
                assertFalse("this op has been synced before", translog.ensureSynced(location)); // not syncing now
                assertTrue("we only synced a previous operation yet", translog.syncNeeded());
            }
            if (rarely()) {
                commit();
                assertFalse("location is from a previous translog - already synced", translog.ensureSynced(location)); // not syncing now
                assertFalse("no sync needed since no operations in current translog", translog.syncNeeded());
            }

            if (randomBoolean()) {
                translog.sync();
                assertFalse("translog has been synced already", translog.ensureSynced(location));
            }
        }
    }

    public void testLocationComparison() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        int translogOperations = randomIntBetween(10, 100);
        int count = 0;
        for (int op = 0; op < translogOperations; op++) {
            locations.add(translog.add(new Translog.Index("test", "" + op, op, Integer.toString(++count).getBytes(Charset.forName("UTF-8")))));
            if (rarely() && translogOperations > op + 1) {
                commit();
            }
        }
        Collections.shuffle(locations, random());
        Translog.Location max = locations.get(0);
        for (Translog.Location location : locations) {
            max = max(max, location);
        }

        assertEquals(max.generation, translog.currentFileGeneration());
        final Translog.Operation read = translog.read(max);
        assertEquals(read.getSource().source.toUtf8(), Integer.toString(count));
    }

    public static Translog.Location max(Translog.Location a, Translog.Location b) {
        if (a.compareTo(b) > 0) {
            return a;
        }
        return b;
    }


    public void testBasicCheckpoint() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        int translogOperations = randomIntBetween(10, 100);
        int lastSynced = -1;
        for (int op = 0; op < translogOperations; op++) {
            locations.add(translog.add(new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")))));
            if (frequently()) {
                translog.sync();
                lastSynced = op;
            }
        }
        assertEquals(translogOperations, translog.stats().numberOfOperations());
        final Translog.Location lastLocation = translog.add(new Translog.Index("test", "" + translogOperations, translogOperations, Integer.toString(translogOperations).getBytes(Charset.forName("UTF-8"))));

        final Checkpoint checkpoint = readLastCheckpoint();
        try (final ImmutableTranslogReader reader = translog.openReader(translog.location().resolve(Translog.getFilename(translog.currentFileGeneration())), checkpoint)) {
            assertEquals(lastSynced + 1, reader.totalOperations());
            for (int op = 0; op < translogOperations; op++) {
                Translog.Location location = locations.get(op);
                if (op <= lastSynced) {
                    final Translog.Operation read = reader.read(location);
                    assertEquals(Integer.toString(op), read.getSource().source.toUtf8());
                } else {
                    try {
                        reader.read(location);
                        fail("read past checkpoint");
                    } catch (EOFException ex) {

                    }
                }
            }
            try {
                reader.read(lastLocation);
                fail("read past checkpoint");
            } catch (EOFException ex) {
            }
        }
        assertEquals(translogOperations + 1, translog.stats().numberOfOperations());
        translog.close();
    }

    public void testTranslogWriter() throws IOException {
        final TranslogWriter writer = translog.createWriter(2, 0); // the translog starts on gen 1, use 2 to have a fresh file
        final int numOps = randomIntBetween(10, 100);
        byte[] bytes = new byte[4];
        ByteArrayDataOutput out = new ByteArrayDataOutput(bytes);
        for (int i = 0; i < numOps; i++) {
            out.reset(bytes);
            out.writeInt(i);
            writer.add(i, new BytesArray(bytes));
        }
        writer.sync();

        final TranslogReader reader = randomBoolean() ? writer : translog.openReader(writer.path(), readLastCheckpoint());
        for (int i = 0; i < numOps; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            reader.readBytes(buffer, reader.getFirstOperationOffset() + 4 * i);
            buffer.flip();
            final int value = buffer.getInt();
            assertEquals(i, value);
        }

        out.reset(bytes);
        out.writeInt(2048);
        writer.add(numOps, new BytesArray(bytes));

        if (reader instanceof ImmutableTranslogReader) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            try {
                reader.readBytes(buffer, reader.getFirstOperationOffset() + 4 * numOps);
                fail("read past EOF?");
            } catch (EOFException ex) {
                // expected
            }
        } else {
            // live reader!
            ByteBuffer buffer = ByteBuffer.allocate(4);
            final long pos = reader.getFirstOperationOffset() + 4 * numOps;
            reader.readBytes(buffer, pos);
            buffer.flip();
            final int value = buffer.getInt();
            assertEquals(2048, value);
        }
        IOUtils.close(writer);
        if (reader instanceof Closeable) {
            IOUtils.close((Closeable) reader);
        }
    }

    public void testBasicRecovery() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        int translogOperations = randomIntBetween(10, 100);
        Translog.TranslogGeneration translogGeneration = null;
        int minUncommittedOp = -1;
        final boolean commitOften = randomBoolean();
        for (int op = 0; op < translogOperations; op++) {
            locations.add(translog.add(new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")))));
            final boolean commit = commitOften ? frequently() : rarely();
            if (commit && op < translogOperations - 1) {
                commit();
                minUncommittedOp = op + 1;
                translogGeneration = translog.getGeneration();
            }
        }
        translog.sync();
        TranslogConfig config = translog.getConfig();

        config.setTranslogGeneration(translogGeneration);
        IOUtils.close(translog);
        translog = new Translog(config);
        uncommittedView = translog.newView();
        if (translogGeneration == null) {
            assertEquals(0, translog.stats().numberOfOperations());
            assertEquals(1, translog.currentFileGeneration());
            assertFalse(translog.syncNeeded());
            Translog.Snapshot snapshot = uncommittedView.snapshot();
            assertNull(snapshot.next());
        } else {
            assertEquals("lastCommitted must be 1 less than current", translogGeneration.translogFileGeneration + 1, translog.currentFileGeneration());
            assertFalse(translog.syncNeeded());
            Translog.Snapshot snapshot = uncommittedView.snapshot();
            for (int i = minUncommittedOp; i < translogOperations; i++) {
                assertEquals("expected operation" + i + " to be in the previous translog but wasn't", translog.currentFileGeneration() - 1, locations.get(i).generation);
                Translog.Operation next = snapshot.next();
                assertNotNull("operation " + i + " must be non-null", next);
                assertEquals(i, Integer.parseInt(next.getSource().source.toUtf8()));
            }
        }
    }

    public void testRecoveryUncommitted() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        int translogOperations = randomIntBetween(10, 100);
        final int prepareOp = randomIntBetween(0, translogOperations - 1);
        Translog.TranslogGeneration translogGeneration = null;
        final boolean sync = randomBoolean();
        for (int op = 0; op < translogOperations; op++) {
            locations.add(translog.add(new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")))));
            if (op == prepareOp) {
                translogGeneration = translog.getGeneration();
                translog.startNewGeneration();
                assertEquals("expected this to be the first commit", 1l, translogGeneration.translogFileGeneration);
                assertNotNull(translogGeneration.translogUUID);
            }
        }
        if (sync) {
            translog.sync();
        }
        // we intentionally don't close the tlog that is in the prepareCommit stage since we try to recovery the uncommitted
        // translog here as well.
        TranslogConfig config = translog.getConfig();
        config.setTranslogGeneration(translogGeneration);
        try (Translog translog = new Translog(config)) {
            assertNotNull(translogGeneration);
            assertEquals("lastCommitted must be 2 less than current - we never finished the commit", translogGeneration.translogFileGeneration + 2, translog.currentFileGeneration());
            assertFalse(translog.syncNeeded());
            try (Translog.View uncommittedView = translog.newView()) {
                Translog.Snapshot snapshot = uncommittedView.snapshot();
                int upTo = sync ? translogOperations : prepareOp;
                for (int i = 0; i < upTo; i++) {
                    Translog.Operation next = snapshot.next();
                    assertNotNull("operation " + i + " must be non-null synced: " + sync, next);
                    assertEquals("payload missmatch, synced: " + sync, i, Integer.parseInt(next.getSource().source.toUtf8()));
                }
            }
        }
        if (randomBoolean()) { // recover twice
            try (Translog translog = new Translog(config)) {
                assertNotNull(translogGeneration);
                assertEquals("lastCommitted must be 3 less than current - we never finished the commit and run recovery twice", translogGeneration.translogFileGeneration + 3, translog.currentFileGeneration());
                assertFalse(translog.syncNeeded());
                try (Translog.View uncommittedView = translog.newView()) {
                    Translog.Snapshot snapshot = uncommittedView.snapshot();
                    int upTo = sync ? translogOperations : prepareOp;
                    for (int i = 0; i < upTo; i++) {
                        Translog.Operation next = snapshot.next();
                        assertNotNull("operation " + i + " must be non-null synced: " + sync, next);
                        assertEquals("payload missmatch, synced: " + sync, i, Integer.parseInt(next.getSource().source.toUtf8()));
                    }
                }
            }
        }
    }


    public void testRecoveryUncommittedFileExists() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        int translogOperations = randomIntBetween(10, 100);
        final int prepareOp = randomIntBetween(0, translogOperations - 1);
        Translog.TranslogGeneration translogGeneration = null;
        final boolean sync = randomBoolean();
        for (int op = 0; op < translogOperations; op++) {
            locations.add(translog.add(new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")))));
            if (op == prepareOp) {
                translogGeneration = translog.getGeneration();
                translog.startNewGeneration();
                assertEquals("expected this to be the first commit", 1l, translogGeneration.translogFileGeneration);
                assertNotNull(translogGeneration.translogUUID);
            }
        }
        if (sync) {
            translog.sync();
        }
        // we intentionally don't close the tlog that is in the prepareCommit stage since we try to recovery the uncommitted
        // translog here as well.
        TranslogConfig config = translog.getConfig();
        config.setTranslogGeneration(translogGeneration);

        try (Translog translog = new Translog(config)) {
            assertNotNull(translogGeneration);
            assertEquals("lastCommitted must be 2 less than current - we never finished the commit", translogGeneration.translogFileGeneration + 2, translog.currentFileGeneration());
            assertFalse(translog.syncNeeded());
            try (Translog.View uncommittedView = translog.newView()) {
                Translog.Snapshot snapshot = uncommittedView.snapshot();
                int upTo = sync ? translogOperations : prepareOp;
                for (int i = 0; i < upTo; i++) {
                    Translog.Operation next = snapshot.next();
                    assertNotNull("operation " + i + " must be non-null synced: " + sync, next);
                    assertEquals("payload missmatch, synced: " + sync, i, Integer.parseInt(next.getSource().source.toUtf8()));
                }
            }
        }

        if (randomBoolean()) { // recover twice
            try (Translog translog = new Translog(config)) {
                assertNotNull(translogGeneration);
                assertEquals("lastCommitted must be 3 less than current - we never finished the commit and run recovery twice", translogGeneration.translogFileGeneration + 3, translog.currentFileGeneration());
                assertFalse(translog.syncNeeded());
                try (Translog.View uncommittedView = translog.newView()) {
                    Translog.Snapshot snapshot = uncommittedView.snapshot();
                    int upTo = sync ? translogOperations : prepareOp;
                    for (int i = 0; i < upTo; i++) {
                        Translog.Operation next = snapshot.next();
                        assertNotNull("operation " + i + " must be non-null synced: " + sync, next);
                        assertEquals("payload missmatch, synced: " + sync, i, Integer.parseInt(next.getSource().source.toUtf8()));
                    }
                }
            }
        }
    }

    public void testRecoveryUncommittedCorruptedCheckpoint() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        int translogOperations = 100;
        final int prepareOp = 44;
        Translog.TranslogGeneration translogGeneration = null;
        final boolean sync = randomBoolean();
        for (int op = 0; op < translogOperations; op++) {
            locations.add(translog.add(new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")))));
            if (op == prepareOp) {
                translogGeneration = translog.getGeneration();
                translog.startNewGeneration();
                assertEquals("expected this to be the first commit", 1l, translogGeneration.translogFileGeneration);
                assertNotNull(translogGeneration.translogUUID);
            }
        }
        translog.sync();
        // we intentionally don't close the tlog that is in the prepareCommit stage since we try to recovery the uncommitted
        // translog here as well.
        TranslogConfig config = translog.getConfig();
        config.setTranslogGeneration(translogGeneration);
        Checkpoint read = readLastCheckpoint();
        Checkpoint corrupted = new Checkpoint(0, 0, 0, 0, 0, 0);
        Checkpoint.write(config.getTranslogPath().resolve(Translog.getCommitCheckpointFileName(read.generation)), corrupted, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        try (Translog translog = new Translog(config)) {
            fail("corrupted");
        } catch (IllegalStateException ex) {
            assertThat(ex.getMessage(), containsString("generation mismatch"));
        }
        Checkpoint.write(config.getTranslogPath().resolve(Translog.getCommitCheckpointFileName(read.generation)), read, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        try (Translog translog = new Translog(config)) {
            assertNotNull(translogGeneration);
            assertEquals("lastCommitted must be 2 less than current - we never finished the commit", translogGeneration.translogFileGeneration + 2, translog.currentFileGeneration());
            assertFalse(translog.syncNeeded());
            try (Translog.View uncommittedView = translog.newView()) {
                Translog.Snapshot snapshot = uncommittedView.snapshot();
                int upTo = sync ? translogOperations : prepareOp;
                for (int i = 0; i < upTo; i++) {
                    Translog.Operation next = snapshot.next();
                    assertNotNull("operation " + i + " must be non-null synced: " + sync, next);
                    assertEquals("payload missmatch, synced: " + sync, i, Integer.parseInt(next.getSource().source.toUtf8()));
                }
            }
        }
    }

    public void testSnapshotFromStreamInput() throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        List<Translog.Operation> ops = new ArrayList<>();
        int translogOperations = randomIntBetween(10, 100);
        for (int op = 0; op < translogOperations; op++) {
            Translog.Index test = new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")));
            ops.add(test);
        }
        Translog.writeOperations(out, ops);
        final List<Translog.Operation> readOperations = Translog.readOperations(StreamInput.wrap(out.bytes()));
        assertEquals(ops.size(), readOperations.size());
        assertEquals(ops, readOperations);
    }

    public void testLocationHashCodeEquals() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        List<Translog.Location> locations2 = new ArrayList<>();
        int translogOperations = randomIntBetween(10, 100);
        try (Translog translog2 = create(createTempDir())) {
            for (int op = 0; op < translogOperations; op++) {
                locations.add(translog.add(new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")))));
                locations2.add(translog2.add(new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")))));
            }
            int iters = randomIntBetween(10, 100);
            for (int i = 0; i < iters; i++) {
                Translog.Location location = RandomPicks.randomFrom(random(), locations);
                for (Translog.Location loc : locations) {
                    if (loc == location) {
                        assertTrue(loc.equals(location));
                        assertEquals(loc.hashCode(), location.hashCode());
                    } else {
                        assertFalse(loc.equals(location));
                    }
                }
                for (int j = 0; j < translogOperations; j++) {
                    assertTrue(locations.get(j).equals(locations2.get(j)));
                    assertEquals(locations.get(j).hashCode(), locations2.get(j).hashCode());
                }
            }
        }
    }

    public void testOpenForeignTranslog() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        int translogOperations = randomIntBetween(1, 10);
        int firstUncommitted = 0;
        for (int op = 0; op < translogOperations; op++) {
            locations.add(translog.add(new Translog.Index("test", "" + op, op, Integer.toString(op).getBytes(Charset.forName("UTF-8")))));
            if (randomBoolean()) {
                commit();
                firstUncommitted = op + 1;
            }
        }
        TranslogConfig config = translog.getConfig();
        Translog.TranslogGeneration translogGeneration = translog.getGeneration();
        translog.close();

        config.setTranslogGeneration(new Translog.TranslogGeneration(randomRealisticUnicodeOfCodepointLengthBetween(1, translogGeneration.translogUUID.length()), translogGeneration.translogFileGeneration));
        try {
            new Translog(config);
            fail("translog doesn't belong to this UUID");
        } catch (TranslogCorruptedException ex) {

        }
        config.setTranslogGeneration(translogGeneration);
        this.translog = new Translog(config);
        this.uncommittedView = translog.newView();
        Translog.Snapshot snapshot = this.uncommittedView.snapshot();
        for (int i = firstUncommitted; i < translogOperations; i++) {
            Translog.Operation next = snapshot.next();
            assertNotNull("" + i, next);
            assertEquals(Integer.parseInt(next.getSource().source.toUtf8()), i);
        }
        assertNull(snapshot.next());
    }

    public void testFailOnClosedWrite() throws IOException {
        translog.add(new Translog.Index("test", "1", 0, Integer.toString(1).getBytes(Charset.forName("UTF-8"))));
        translog.close();
        try {
            translog.add(new Translog.Index("test", "1", 1, Integer.toString(1).getBytes(Charset.forName("UTF-8"))));
            fail("closed");
        } catch (AlreadyClosedException ex) {
            // all is welll
        }
    }

    public void testCloseConcurrently() throws Throwable {
        final int opsPerThread = randomIntBetween(10, 200);
        int threadCount = 2 + randomInt(5);

        logger.info("testing with [{}] threads, each doing [{}] ops", threadCount, opsPerThread);
        final BlockingQueue<Translog.Operation> writtenOperations = new ArrayBlockingQueue<>(threadCount * opsPerThread);

        Thread[] threads = new Thread[threadCount];
        final Throwable[] threadExceptions = new Throwable[threadCount];
        final CountDownLatch downLatch = new CountDownLatch(1);
        final AtomicLong seqNoGenerator = new AtomicLong();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new TranslogThread(translog, downLatch, opsPerThread, threadId, writtenOperations, threadExceptions, seqNoGenerator);
            threads[i].setDaemon(true);
            threads[i].start();
        }

        downLatch.countDown();
        translog.close();

        for (int i = 0; i < threadCount; i++) {
            if (threadExceptions[i] != null) {
                if ((threadExceptions[i] instanceof AlreadyClosedException) == false) {
                    throw threadExceptions[i];
                }
            }
            threads[i].join(60 * 1000);
        }
    }

    private static class TranslogThread extends Thread {
        private final CountDownLatch downLatch;
        private final int opsPerThread;
        private final int threadId;
        private final Collection<Translog.Operation> writtenOperations;
        private final Throwable[] threadExceptions;
        private final Translog translog;
        private final AtomicLong seqNoGenerator;


        public TranslogThread(Translog translog, CountDownLatch downLatch, int opsPerThread, int threadId, Collection<Translog.Operation> writtenOperations, Throwable[] threadExceptions, AtomicLong seqNoGenerator) {
            this.translog = translog;
            this.downLatch = downLatch;
            this.opsPerThread = opsPerThread;
            this.threadId = threadId;
            this.threadExceptions = threadExceptions;
            this.seqNoGenerator = seqNoGenerator;
            this.writtenOperations = writtenOperations;
        }

        @Override
        public void run() {
            try {
                downLatch.await();
                for (int opCount = 0; opCount < opsPerThread; opCount++) {
                    Translog.Operation op;
                    switch (randomFrom(Translog.Operation.Type.values())) {
                        case CREATE:
                        case INDEX:
                            op = new Translog.Index("test", threadId + "_" + opCount, seqNoGenerator.getAndIncrement(),
                                    randomUnicodeOfLengthBetween(1, 20 * 1024).getBytes("UTF-8"));
                            break;
                        case DELETE:
                            op = new Translog.Delete(new Term("_uid", threadId + "_" + opCount),
                                    seqNoGenerator.getAndIncrement(), 1 + randomInt(100000),
                                    randomFrom(VersionType.values()));
                            break;
                        case FAILURE:
                            op = new Translog.Failure(seqNoGenerator.getAndIncrement(), randomAsciiOfLength(10));
                            break;
                        default:
                            throw new ElasticsearchException("not supported op type");
                    }
                    try {
                        add(op);
                    } finally {
                        if (op.location() != null) {
                            writtenOperations.add(op);
                        }
                    }
                    afterAdd();
                }
            } catch (Throwable t) {
                threadExceptions[threadId] = t;
            }
        }

        protected Translog.Location add(Translog.Operation op) throws IOException {
            return translog.add(op);
        }

        protected void afterAdd() throws IOException {
        }
    }

    public void testFailFlush() throws IOException {
        Path tempDir = createTempDir();
        final AtomicBoolean fail = new AtomicBoolean();
        TranslogConfig config = getTranslogConfig(tempDir);
        Translog translog = getFailableTranslog(fail, config);
        Translog.View uncommittedView = translog.newView();

        List<Translog.Location> locations = new ArrayList<>();
        int opsSynced = 0;
        boolean failed = false;
        while (failed == false) {
            try {
                locations.add(translog.add(new Translog.Index("test", "" + opsSynced, opsSynced, Integer.toString(opsSynced).getBytes(Charset.forName("UTF-8")))));
                translog.sync();
                opsSynced++;
            } catch (MockDirectoryWrapper.FakeIOException ex) {
                failed = true;
                assertFalse(translog.isOpen());
            } catch (IOException ex) {
                failed = true;
                assertFalse(translog.isOpen());
                assertEquals("__FAKE__ no space left on device", ex.getMessage());
            }
            fail.set(randomBoolean());
        }
        fail.set(false);
        if (randomBoolean()) {
            try {
                locations.add(translog.add(new Translog.Index("test", "" + opsSynced, opsSynced, Integer.toString(opsSynced).getBytes(Charset.forName("UTF-8")))));
                fail("we are already closed");
            } catch (AlreadyClosedException ex) {
                assertNotNull(ex.getCause());
                if (ex.getCause() instanceof MockDirectoryWrapper.FakeIOException) {
                    assertNull(ex.getCause().getMessage());
                } else {
                    assertEquals(ex.getCause().getMessage(), "__FAKE__ no space left on device");
                }
            }

        }
        Translog.TranslogGeneration translogGeneration = translog.getGeneration();
        try {
            uncommittedView.snapshot();
            fail("already closed");
        } catch (AlreadyClosedException ex) {
            // all is well
            assertNotNull(ex.getCause());
            assertSame(translog.getTragicException(), ex.getCause());
        }
        try {
            translog.newView();
            fail("already closed");
        } catch (AlreadyClosedException ex) {
            // all is well
            assertNotNull(ex.getCause());
            assertSame(translog.getTragicException(), ex.getCause());
        }


        try {
            commit(translog, uncommittedView);
            fail("already closed");
        } catch (AlreadyClosedException ex) {
            assertNotNull(ex.getCause());
            assertSame(translog.getTragicException(), ex.getCause());
        }

        assertFalse(translog.isOpen());
        translog.close(); // we are closed
        config.setTranslogGeneration(translogGeneration);
        try (Translog tlog = new Translog(config)) {
            assertEquals("lastCommitted must be 1 less than current", translogGeneration.translogFileGeneration + 1, tlog.currentFileGeneration());
            assertFalse(tlog.syncNeeded());
            try (Translog.View view = tlog.newView()) {
                Translog.Snapshot snapshot = view.snapshot();
                assertEquals(opsSynced, snapshot.estimatedTotalOperations());
                for (int i = 0; i < opsSynced; i++) {
                    assertEquals("expected operation" + i + " to be in the previous translog but wasn't", tlog.currentFileGeneration() - 1, locations.get(i).generation);
                    Translog.Operation next = snapshot.next();
                    assertNotNull("operation " + i + " must be non-null", next);
                    assertEquals(i, Integer.parseInt(next.getSource().source.toUtf8()));
                }
            }
        }
    }

    public void testTranslogOpsCountIsCorrect() throws IOException {
        List<Translog.Location> locations = new ArrayList<>();
        int numOps = randomIntBetween(100, 200);
        LineFileDocs lineFileDocs = new LineFileDocs(random()); // writes pretty big docs so we cross buffer boarders regularly
        for (int opsAdded = 0; opsAdded < numOps; opsAdded++) {
            locations.add(translog.add(new Translog.Index("test", "" + opsAdded, opsAdded, lineFileDocs.nextDoc().toString().getBytes(Charset.forName("UTF-8")))));
            Translog.Snapshot snapshot = uncommittedView.snapshot();
            assertEquals(opsAdded + 1, snapshot.estimatedTotalOperations());
            for (int i = 0; i < opsAdded; i++) {
                assertEquals("expected operation" + i + " to be in the current translog but wasn't", translog.currentFileGeneration(), locations.get(i).generation);
                Translog.Operation next = snapshot.next();
                assertNotNull("operation " + i + " must be non-null", next);
            }
        }
    }

    public void testTragicEventCanBeAnyException() throws IOException {
        Path tempDir = createTempDir();
        final AtomicBoolean fail = new AtomicBoolean();
        TranslogConfig config = getTranslogConfig(tempDir);
        assumeFalse("this won't work if we sync on any op", config.isSyncOnEachOperation());
        Translog translog = getFailableTranslog(fail, config, false, true);
        LineFileDocs lineFileDocs = new LineFileDocs(random()); // writes pretty big docs so we cross buffer boarders regularly
        translog.add(new Translog.Index("test", "1", 0, lineFileDocs.nextDoc().toString().getBytes(Charset.forName("UTF-8"))));
        fail.set(true);
        try {
            Translog.Location location = translog.add(new Translog.Index("test", "2", 1, lineFileDocs.nextDoc().toString().getBytes(Charset.forName("UTF-8"))));
            if (config.getType() == TranslogWriter.Type.BUFFERED) { // the buffered case will fail on the add if we exceed the buffer or will fail on the flush once we sync
                if (randomBoolean()) {
                    translog.ensureSynced(location);
                } else {
                    translog.sync();
                }
            }
            //TODO once we have a mock FS that can simulate we can also fail on plain sync
            fail("WTF");
        } catch (UnknownException ex) {
            // w00t
        } catch (TranslogException ex) {
            assertTrue(ex.getCause() instanceof UnknownException);
        }
        assertFalse(translog.isOpen());
        assertTrue(translog.getTragicException() instanceof UnknownException);
    }

    public void testFatalIOExceptionsWhileWritingConcurrently() throws IOException, InterruptedException {
        Path tempDir = createTempDir();
        final AtomicBoolean fail = new AtomicBoolean(false);

        TranslogConfig config = getTranslogConfig(tempDir);
        Translog translog = getFailableTranslog(fail, config);

        final int threadCount = randomIntBetween(1, 5);
        Thread[] threads = new Thread[threadCount];
        final Throwable[] threadExceptions = new Throwable[threadCount];
        final CountDownLatch downLatch = new CountDownLatch(1);
        final CountDownLatch added = new CountDownLatch(randomIntBetween(10, 100));
        List<Translog.Operation> writtenOperations = Collections.synchronizedList(new ArrayList<>());
        final AtomicLong seqNoGenerator = new AtomicLong();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new TranslogThread(translog, downLatch, 200, threadId, writtenOperations, threadExceptions, seqNoGenerator) {
                @Override
                protected Translog.Location add(Translog.Operation op) throws IOException {
                    Translog.Location add = super.add(op);
                    added.countDown();
                    return add;
                }

                @Override
                protected void afterAdd() throws IOException {
                    if (randomBoolean()) {
                        translog.sync();
                    }
                }
            };
            threads[i].setDaemon(true);
            threads[i].start();
        }
        downLatch.countDown();
        added.await();
        try (Translog.View view = translog.newView()) {
            // this holds a reference to the current tlog channel such that it's not closed
            // if we hit a tragic event. this is important to ensure that asserts inside the Translog#add doesn't trip
            // otherwise our assertions here are off by one sometimes.
            fail.set(true);
            for (int i = 0; i < threadCount; i++) {
                threads[i].join();
            }
            boolean atLeastOneFailed = false;
            for (Throwable ex : threadExceptions) {
                assertTrue(ex.toString(), ex instanceof IOException || ex instanceof AlreadyClosedException);
                if (ex != null) {
                    atLeastOneFailed = true;
                }
            }
            if (atLeastOneFailed == false) {
                try {
                    boolean syncNeeded = translog.syncNeeded();
                    translog.close();
                    assertFalse("should have failed if sync was needed", syncNeeded);
                } catch (IOException ex) {
                    // boom now we failed
                }
            }
            Collections.sort(writtenOperations, (a, b) -> a.location().compareTo(b.location()));
            assertFalse(translog.isOpen());
            final List<Checkpoint> checkpoints = translog.readOrderedCheckpoints();
            Iterator<Translog.Operation> iterator = writtenOperations.iterator();
            while (iterator.hasNext()) {
                Translog.Operation next = iterator.next();
                for (Checkpoint checkpoint : checkpoints) {
                    if (next.location().generation == checkpoint.generation) {
                        if (checkpoint.offset < (next.location().translogLocation + next.location().size)) {
                            // drop all that haven't been synced
                            logger.info("--> dropping [{}], checkpoint offset [{}], need [{}]", next, checkpoint.offset, next.location().translogLocation + next.location().size);
                            iterator.remove();
                        }
                    }
                }
            }
            config.setTranslogGeneration(translog.getGeneration());
            try (Translog tlog = new Translog(config)) {
                try (Translog.View uncommittedView = tlog.newView()) {
                    Translog.Snapshot snapshot = uncommittedView.snapshot();
                    if (writtenOperations.size() != snapshot.estimatedTotalOperations()) {
                        for (int i = 0; i < threadCount; i++) {
                            if (threadExceptions[i] != null) {
                                threadExceptions[i].printStackTrace();
                            }
                        }
                    }
                    logger.info("--> checking for [{}] operations in snapshot", writtenOperations.size());
                    for (int i = 0; i < writtenOperations.size(); i++) {
                        final Translog.Operation expected = writtenOperations.get(i);
                        Translog.Operation next = snapshot.next();
                        assertNotNull("operation " + i + " must be non-null", next);
                        assertEquals("next: " + next + "\nexpected: " + expected + "\nsnapshot: " + snapshot, next, expected);
                    }
                    assertEquals(writtenOperations.size(), snapshot.estimatedTotalOperations());
                }
            }
        }
    }

    private Translog getFailableTranslog(final AtomicBoolean fail, final TranslogConfig config) throws IOException {
        return getFailableTranslog(fail, config, randomBoolean(), false);
    }

    private Translog getFailableTranslog(final AtomicBoolean fail, final TranslogConfig config, final boolean paritalWrites, final boolean throwUnknownException) throws IOException {
        return new Translog(config) {
            @Override
            TranslogWriter.ChannelFactory getChannelFactory() {
                final TranslogWriter.ChannelFactory factory = super.getChannelFactory();

                return new TranslogWriter.ChannelFactory() {
                    @Override
                    public FileChannel create(Path file) throws IOException {
                        FileChannel channel = factory.create(file);
                        return new ThrowingFileChannel(fail, paritalWrites, throwUnknownException, channel);
                    }

                    @Override
                    public FileChannel open(Path file) throws IOException {
                        FileChannel channel = factory.open(file);
                        return new ThrowingFileChannel(fail, paritalWrites, throwUnknownException, channel);
                    }
                };
            }
        };
    }

    public static class ThrowingFileChannel extends FilterFileChannel {
        private final AtomicBoolean fail;
        private final boolean partialWrite;
        private final boolean throwUnknownException;

        public ThrowingFileChannel(AtomicBoolean fail, boolean partialWrite, boolean throwUnknownException, FileChannel delegate) {
            super(delegate);
            this.fail = fail;
            this.partialWrite = partialWrite;
            this.throwUnknownException = throwUnknownException;
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException {
            throw new UnsupportedOperationException();
        }


        public int write(ByteBuffer src) throws IOException {
            if (fail.get()) {
                if (partialWrite) {
                    if (src.hasRemaining()) {
                        final int pos = src.position();
                        final int limit = src.limit();
                        src.limit(randomIntBetween(pos, limit));
                        super.write(src);
                        src.limit(limit);
                        src.position(pos);
                        throw new IOException("__FAKE__ no space left on device");
                    }
                }
                if (throwUnknownException) {
                    throw new UnknownException();
                } else {
                    throw new MockDirectoryWrapper.FakeIOException();
                }
            }
            return super.write(src);
        }
    }

    private static final class UnknownException extends RuntimeException {

    }
}
