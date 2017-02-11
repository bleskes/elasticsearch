/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.support;

import org.elasticsearch.cluster.Diff;
import org.elasticsearch.cluster.Diffable;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.io.stream.Writeable.Reader;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Utilities that simplify testing of diffable classes
 */
public final class DiffableTestUtils {
    protected static final int NUMBER_OF_DIFF_TEST_RUNS = 20;

    private DiffableTestUtils() {

    }

    /**
     * Asserts that changes are applied correctly, i.e. that applying diffs to localInstance produces that object
     * equal but not the same as the remoteChanges instance.
     */
    public static <T extends Diffable<T>> T assertDiffApplication(T remoteChanges, T localInstance, Diff<T> diffs) {
        T localChanges = diffs.apply(localInstance);
        assertEquals(remoteChanges, localChanges);
        assertEquals(remoteChanges.hashCode(), localChanges.hashCode());
        assertNotSame(remoteChanges, localChanges);
        return localChanges;
    }

    /**
     * Simulates sending diffs over the wire
     */
    public static <T extends Writeable> T copyInstance(T diffs, NamedWriteableRegistry namedWriteableRegistry,
                                                       Reader<T> reader) throws IOException {
        try (BytesStreamOutput output = new BytesStreamOutput()) {
            diffs.writeTo(output);
            try (StreamInput in = new NamedWriteableAwareStreamInput(output.bytes().streamInput(), namedWriteableRegistry)) {
                return reader.read(in);
            }
        }
    }

    public static <T> Diff<T> copyInstance(Diff<T> diffs, NamedWriteableRegistry namedWriteableRegistry,
                                           Reader<Diff<T>> reader) throws IOException {
        try (BytesStreamOutput output = new BytesStreamOutput()) {
            diffs.writeTo(output);
            try (StreamInput in = new NamedWriteableAwareStreamInput(output.bytes().streamInput(), namedWriteableRegistry)) {
                return reader.read(in);
            }
        }
    }


    /**
     * Tests making random changes to an object, calculating diffs for these changes, sending this
     * diffs over the wire and appling these diffs on the other side.
     */
    public static <T extends Diffable<T>> void testDiffableSerialization(Supplier<T> testInstance,
                                                                         Function<T, T> modifier,
                                                                         NamedWriteableRegistry namedWriteableRegistry,
                                                                         Reader<T> reader,
                                                                         Reader<Diff<T>> diffReader) throws IOException {
        T remoteInstance = testInstance.get();
        T localInstance = assertSerialization(remoteInstance, namedWriteableRegistry, reader);
        for (int runs = 0; runs < NUMBER_OF_DIFF_TEST_RUNS; runs++) {
            T remoteChanges = modifier.apply(remoteInstance);
            Diff<T> remoteDiffs = remoteChanges.diff(remoteInstance);
            Diff<T> localDiffs = copyInstance(remoteDiffs, namedWriteableRegistry, diffReader);
            localInstance = assertDiffApplication(remoteChanges, localInstance, localDiffs);
            remoteInstance = remoteChanges;
        }
    }

    /**
     * Asserts that testInstance can be correctly.
     */
    public static  <T extends Writeable> T assertSerialization(T testInstance, NamedWriteableRegistry namedWriteableRegistry,
                                                               Reader<T> reader) throws IOException {
        T deserializedInstance = copyInstance(testInstance, namedWriteableRegistry, reader);
        assertEquals(testInstance, deserializedInstance);
        assertEquals(testInstance.hashCode(), deserializedInstance.hashCode());
        assertNotSame(testInstance, deserializedInstance);
        return deserializedInstance;
    }

}