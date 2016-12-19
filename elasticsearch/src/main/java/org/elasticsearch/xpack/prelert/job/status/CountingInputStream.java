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
package org.elasticsearch.xpack.prelert.job.status;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple wrapper around an inputstream instance that counts
 * all the bytes passing through it reporting that number to
 * the {@link StatusReporter}
 * <p>
 * Overrides the read methods counting the number of bytes read.
 */
public class CountingInputStream extends FilterInputStream {
    private StatusReporter statusReporter;

    /**
     * @param in
     *            input stream
     * @param statusReporter
     *            Write number of records, bytes etc.
     */
    public CountingInputStream(InputStream in, StatusReporter statusReporter) {
        super(in);
        this.statusReporter = statusReporter;
    }

    /**
     * Report 1 byte read
     */
    @Override
    public int read() throws IOException {
        int read = in.read();
        statusReporter.reportBytesRead(read < 0 ? 0 : 1);

        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int read = in.read(b);

        statusReporter.reportBytesRead(read < 0 ? 0 : read);

        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);

        statusReporter.reportBytesRead(read < 0 ? 0 : read);
        return read;
    }
}
