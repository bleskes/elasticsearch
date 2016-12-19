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
package org.elasticsearch.xpack.prelert.job.process.autodetect.output;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.CompositeBytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the autodetect persisted state and writes the results via the {@linkplain JobResultsPersister} passed in the constructor.
 */
public class StateProcessor extends AbstractComponent {

    private static final int READ_BUF_SIZE = 8192;
    private final JobResultsPersister persister;

    public StateProcessor(Settings settings, JobResultsPersister persister) {
        super(settings);
        this.persister = persister;
    }

    public void process(String jobId, InputStream in) {
        try {
            BytesReference bytesRef = null;
            byte[] readBuf = new byte[READ_BUF_SIZE];
            for (int bytesRead = in.read(readBuf); bytesRead != -1; bytesRead = in.read(readBuf)) {
                if (bytesRef == null) {
                    bytesRef = new BytesArray(readBuf, 0, bytesRead);
                } else {
                    bytesRef = new CompositeBytesReference(bytesRef, new BytesArray(readBuf, 0, bytesRead));
                }
                bytesRef = splitAndPersist(jobId, bytesRef);
                readBuf = new byte[READ_BUF_SIZE];
            }
        } catch (IOException e) {
            logger.info(new ParameterizedMessage("[{}] Error reading autodetect state output", jobId), e);
        }
        logger.info("[{}] State output finished", jobId);
    }

    /**
     * Splits bulk data streamed from the C++ process on '\0' characters.  The
     * data is expected to be a series of Elasticsearch bulk requests in UTF-8 JSON
     * (as would be uploaded to the public REST API) separated by zero bytes ('\0').
     */
    private BytesReference splitAndPersist(String jobId, BytesReference bytesRef) {
        int from = 0;
        while (true) {
            int nextZeroByte = findNextZeroByte(bytesRef, from);
            if (nextZeroByte == -1) {
                // No more zero bytes in this block
                break;
            }
            persister.persistBulkState(jobId, bytesRef.slice(from, nextZeroByte - from));
            from = nextZeroByte + 1;
        }
        if (from >= bytesRef.length()) {
            return null;
        }
        return bytesRef.slice(from, bytesRef.length() - from);
    }

    private static int findNextZeroByte(BytesReference bytesRef, int from) {
        for (int i = from; i < bytesRef.length(); ++i) {
            if (bytesRef.get(i) == 0) {
                return i;
            }
        }
        return -1;
    }
}

