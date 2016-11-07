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
package org.elasticsearch.xpack.prelert.job.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.CompositeBytesReference;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.Objects;

/**
 * Handle a stream of C++ log messages that arrive via a named pipe in JSON format.
 * Retains the last few error messages so that they can be passed back in a REST response
 * if the C++ process dies.
 */
public class CppLogMessageHandler implements Closeable {

    private static final int DEFAULT_READBUF_SIZE = 1024;
    private static final int DEFAULT_ERROR_STORE_SIZE = 5;

    private final Logger logger;
    private final InputStream inputStream;
    private final int readBufSize;
    private final int errorStoreSize;
    private final Deque<String> errorStore;

    /**
     * @param jobId May be null or empty if the logs are from a process not associated with a job.
     * @param inputStream May not be null.
     */
    public CppLogMessageHandler(String jobId, InputStream inputStream) {
        this(inputStream, Strings.isNullOrEmpty(jobId) ? Loggers.getLogger(CppLogMessageHandler.class) : Loggers.getLogger(jobId),
                DEFAULT_READBUF_SIZE, DEFAULT_ERROR_STORE_SIZE);
    }

    /**
     * For testing - allows meddling with the logger, read buffer size and error store size.
     */
    CppLogMessageHandler(InputStream inputStream, Logger logger, int readBufSize, int errorStoreSize) {
        this.logger = Objects.requireNonNull(logger);
        this.inputStream = Objects.requireNonNull(inputStream);
        this.readBufSize = readBufSize;
        this.errorStoreSize = errorStoreSize;
        this.errorStore = ConcurrentCollections.newDeque();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    /**
     * Tail the InputStream provided to the constructor, handling each complete log document as it arrives.
     * This method will not return until either end-of-file is detected on the InputStream or the
     * InputStream throws an exception.
     */
    public void tailStream() throws IOException {
        XContent xContent = XContentFactory.xContent(XContentType.JSON);
        BytesReference bytesRef = null;
        byte[] readBuf = new byte[readBufSize];
        for (int bytesRead = inputStream.read(readBuf); bytesRead != -1; bytesRead = inputStream.read(readBuf)) {
            if (bytesRef == null) {
                bytesRef = new BytesArray(readBuf, 0, bytesRead);
            } else {
                bytesRef = new CompositeBytesReference(bytesRef, new BytesArray(readBuf, 0, bytesRead));
            }
            bytesRef = parseMessages(xContent, bytesRef);
            readBuf = new byte[readBufSize];
        }
    }

    /**
     * Expected to be called very infrequently.
     */
    public String getErrors() {
        String[] errorSnapshot = errorStore.toArray(new String[0]);
        StringBuilder errors = new StringBuilder();
        for (String error : errorSnapshot) {
            errors.append(error).append('\n');
        }
        return errors.toString();
    }

    private BytesReference parseMessages(XContent xContent, BytesReference bytesRef) {
        byte marker = xContent.streamSeparator();
        int from = 0;
        while (true) {
            int nextMarker = findNextMarker(marker, bytesRef, from);
            if (nextMarker == -1) {
                // No more markers in this block
                break;
            }
            parseMessage(xContent, bytesRef.slice(from, nextMarker - from));
            from = nextMarker + 1;
        }
        return bytesRef.slice(from, bytesRef.length() - from);
    }

    private void parseMessage(XContent xContent, BytesReference bytesRef) {
        try {
            XContentParser parser = xContent.createParser(bytesRef);
            CppLogMessage msg = CppLogMessage.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT);
            Level level = Level.getLevel(msg.getLevel());
            if (level == null) {
                // This isn't expected to ever happen
                level = Level.WARN;
            } else if (level.isMoreSpecificThan(Level.ERROR)) {
                // Keep the last few error messages to report if the process dies
                storeError(msg.getMessage());
            }
            // TODO: Is there a way to preserve the original timestamp when re-logging?
            logger.log(level, "{}/{} {}@{} {}", msg.getLogger(), msg.getPid(), msg.getFile(), msg.getLine(), msg.getMessage());
            // TODO: Could send the message for indexing instead of or as well as logging it
        } catch (IOException e) {
            logger.warn("Failed to parse C++ log message: " + bytesRef.utf8ToString(), e);
        }
    }

    private void storeError(String error) {
        if (Strings.isNullOrEmpty(error) || errorStoreSize <= 0) {
            return;
        }
        if (errorStore.size() >= errorStoreSize) {
            errorStore.removeFirst();
        }
        errorStore.offerLast(error);
    }

    private static int findNextMarker(byte marker, BytesReference bytesRef, int from) {
        for (int i = from; i < bytesRef.length(); ++i) {
            if (bytesRef.get(i) == marker) {
                return i;
            }
        }
        return -1;
    }
}
