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
package org.elasticsearch.xpack.prelert.job.scheduler.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Encapsulates the HTTP response stream and the status code.
 *
 * <p><b>Important note</b>: The stream has to be consumed thoroughly.
 * Java is keeping connections alive thus reusing them and any
 * streams with dangling data can lead to problems.
 */
class HttpResponse {

    public static final int OK_STATUS = 200;

    private static final String NEW_LINE = "\n";

    private final InputStream stream;
    private final int responseCode;

    public HttpResponse(InputStream responseStream, int responseCode) {
        stream = responseStream;
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public InputStream getStream() {
        return stream;
    }

    public String getResponseAsString() throws IOException {
        return getStreamAsString(stream);
    }

    public static String getStreamAsString(InputStream stream) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return buffer.lines().collect(Collectors.joining(NEW_LINE));
        }
    }
}
