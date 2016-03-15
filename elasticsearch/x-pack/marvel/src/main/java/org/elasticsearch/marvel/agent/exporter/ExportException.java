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

package org.elasticsearch.marvel.agent.exporter;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExportException extends ElasticsearchException implements Iterable<ExportException> {

    private final List<ExportException> exceptions = new ArrayList<>();

    public ExportException(Throwable throwable) {
        super(throwable);
    }

    public ExportException(String msg, Object... args) {
        super(msg, args);
    }

    public ExportException(String msg, Throwable throwable, Object... args) {
        super(msg, throwable, args);
    }

    public ExportException(StreamInput in) throws IOException {
        super(in);
        for (int i = in.readVInt(); i > 0; i--) {
            exceptions.add(new ExportException(in));
        }
    }

    public boolean addExportException(ExportException e) {
        return exceptions.add(e);
    }

    public boolean hasExportExceptions() {
        return exceptions.size() > 0;
    }

    @Override
    public Iterator<ExportException> iterator() {
        return exceptions.iterator();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(exceptions.size());
        for (ExportException e : exceptions) {
            e.writeTo(out);
        }
    }

    @Override
    protected void innerToXContent(XContentBuilder builder, Params params) throws IOException {
        super.innerToXContent(builder, params);
        if (hasExportExceptions()) {
            builder.startArray("exceptions");
            for (ExportException exception : exceptions) {
                builder.startObject();
                exception.toXContent(builder, params);
                builder.endObject();
            }
            builder.endArray();
        }
    }
}
