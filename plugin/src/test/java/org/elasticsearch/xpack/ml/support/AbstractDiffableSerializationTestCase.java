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
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.ToXContent;

import java.io.IOException;

/**
 * An abstract test case to ensure correct behavior of Diffable.
 *
 * This class can be used as a based class for tests of MetaData.Custom classes and other classes that support,
 * Writable serialization, XContent-based serialization and is diffable.
 */
public abstract class AbstractDiffableSerializationTestCase<T extends Diffable<T> & ToXContent> extends AbstractSerializingTestCase<T> {

    /**
     *  Introduces random changes into the test object
     */
    protected abstract T makeTestChanges(T testInstance);

    protected abstract Reader<Diff<T>> diffReader();

    public void testDiffableSerialization() throws IOException {
        DiffableTestUtils.testDiffableSerialization(this::createTestInstance, this::makeTestChanges, getNamedWriteableRegistry(),
                instanceReader(), diffReader());
    }

}