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

package org.elasticsearch.xpack.monitoring.action;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;

import java.io.IOException;

public class MonitoringBulkDoc extends MonitoringDoc {

    private MonitoringIndex index = MonitoringIndex.TIMESTAMPED;
    private String type;
    private String id;
    private BytesReference source;

    public MonitoringBulkDoc(String monitoringId, String monitoringVersion) {
        super(monitoringId, monitoringVersion);
    }

    public MonitoringBulkDoc(String monitoringId, String monitoringVersion,
                             MonitoringIndex index, String type, String id,
                             BytesReference source) {
        super(monitoringId, monitoringVersion);
        this.index = index;
        this.type = type;
        this.id = id;
        this.source = source;
    }

    /**
     * Read from a stream.
     */
    public MonitoringBulkDoc(StreamInput in) throws IOException {
        super(in);
        index = MonitoringIndex.readFrom(in);
        type = in.readOptionalString();
        id = in.readOptionalString();
        source = in.readBytesReference();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        index.writeTo(out);
        out.writeOptionalString(type);
        out.writeOptionalString(id);
        out.writeBytesReference(source);
    }

    public MonitoringIndex getIndex() {
        return index;
    }

    public void setIndex(MonitoringIndex index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BytesReference getSource() {
        return source;
    }

    public void setSource(BytesReference source) {
        this.source = source;
    }

}
