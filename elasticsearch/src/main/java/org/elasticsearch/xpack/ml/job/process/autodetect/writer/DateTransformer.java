/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.process.autodetect.writer;

/**
 * An interface for transforming a String timestamp into epoch_millis.
 */
public interface DateTransformer {
    /**
     *
     * @param timestamp A String representing a timestamp
     * @return Milliseconds since the epoch that the timestamp corresponds to
     * @throws CannotParseTimestampException If the timestamp cannot be parsed
     */
    long transform(String timestamp) throws CannotParseTimestampException;
}
