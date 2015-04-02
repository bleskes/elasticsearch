/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.watcher.support.http;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.io.ByteStreams;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class HttpResponse implements Closeable {

    private int status;
    private InputStream inputStream;
    private byte[] body;

    public HttpResponse(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }

    public byte[] body() {
        if (body == null && inputStream != null) {
            try {
                body = ByteStreams.toByteArray(inputStream);
                inputStream.close();
            } catch (IOException e) {
                throw ExceptionsHelper.convertToElastic(e);
            }
        }
        return body;
    }

    public void inputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}