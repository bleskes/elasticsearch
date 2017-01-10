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
package org.elasticsearch.xpack.ml.job.process.normalizer.output;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.CompositeBytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.ml.job.process.normalizer.NormalizerResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads normalizer output.
 */
public class NormalizerResultHandler extends AbstractComponent {

    private static final int READ_BUF_SIZE = 1024;

    private final InputStream inputStream;
    private final List<NormalizerResult> normalizedResults;

    public NormalizerResultHandler(Settings settings, InputStream inputStream) {
        super(settings);
        this.inputStream = inputStream;
        normalizedResults = new ArrayList<>();
    }

    public List<NormalizerResult> getNormalizedResults() {
        return normalizedResults;
    }

    public void process() throws IOException {
        XContent xContent = XContentFactory.xContent(XContentType.JSON);
        BytesReference bytesRef = null;
        byte[] readBuf = new byte[READ_BUF_SIZE];
        for (int bytesRead = inputStream.read(readBuf); bytesRead != -1; bytesRead = inputStream.read(readBuf)) {
            if (bytesRef == null) {
                bytesRef = new BytesArray(readBuf, 0, bytesRead);
            } else {
                bytesRef = new CompositeBytesReference(bytesRef, new BytesArray(readBuf, 0, bytesRead));
            }
            bytesRef = parseResults(xContent, bytesRef);
            readBuf = new byte[READ_BUF_SIZE];
        }
    }

    private BytesReference parseResults(XContent xContent, BytesReference bytesRef) throws IOException {
        byte marker = xContent.streamSeparator();
        int from = 0;
        while (true) {
            int nextMarker = findNextMarker(marker, bytesRef, from);
            if (nextMarker == -1) {
                // No more markers in this block
                break;
            }
            // Ignore blank lines
            if (nextMarker > from) {
                parseResult(xContent, bytesRef.slice(from, nextMarker - from));
            }
            from = nextMarker + 1;
        }
        if (from >= bytesRef.length()) {
            return null;
        }
        return bytesRef.slice(from, bytesRef.length() - from);
    }

    private void parseResult(XContent xContent, BytesReference bytesRef) throws IOException {
        XContentParser parser = xContent.createParser(NamedXContentRegistry.EMPTY, bytesRef);
        NormalizerResult result = NormalizerResult.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT);
        normalizedResults.add(result);
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

