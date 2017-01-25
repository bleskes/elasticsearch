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

import java.util.Locale;

/**
 * A transformer that attempts to parse a String timestamp
 * as a double and convert that to a long that represents
 * an epoch. If m_IsMillisecond is true, it will convert to seconds.
 */
public class DoubleDateTransformer implements DateTransformer {

    private static final long MS_IN_SECOND = 1000;

    private final boolean isMillisecond;

    public DoubleDateTransformer(boolean isMillisecond) {
        this.isMillisecond = isMillisecond;
    }

    @Override
    public long transform(String timestamp) throws CannotParseTimestampException {
        try {
            long longValue = Double.valueOf(timestamp).longValue();
            return isMillisecond ? longValue : longValue * MS_IN_SECOND;
        } catch (NumberFormatException e) {
            String message = String.format(Locale.ROOT, "Cannot parse timestamp '%s' as epoch value", timestamp);
            throw new CannotParseTimestampException(message, e);
        }
    }
}
