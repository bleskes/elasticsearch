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

package org.elasticsearch.marvel.agent.resolver.bulk;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.marvel.action.MonitoringBulkDoc;
import org.elasticsearch.marvel.agent.resolver.MonitoringIndexNameResolver;

import java.io.IOException;

public class MonitoringBulkDataResolver extends MonitoringIndexNameResolver.Data<MonitoringBulkDoc> {

    @Override
    public String type(MonitoringBulkDoc document) {
        return document.getType();
    }

    @Override
    public String id(MonitoringBulkDoc document) {
        return document.getId();
    }

    @Override
    protected void buildXContent(MonitoringBulkDoc document, XContentBuilder builder, ToXContent.Params params) throws IOException {
        BytesReference source = document.getSource();
        if (source != null && source.length() > 0) {
            builder.rawField(type(document), source);
        }
    }
}