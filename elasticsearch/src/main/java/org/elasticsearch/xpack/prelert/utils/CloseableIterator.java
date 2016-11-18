/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.utils;

import java.io.Closeable;
import java.util.Iterator;

/**
 * An interface for iterators that can have resources that will be automatically cleaned up
 * if iterator is created in a try-with-resources block.
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

}
