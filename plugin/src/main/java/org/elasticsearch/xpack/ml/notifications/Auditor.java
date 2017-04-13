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
package org.elasticsearch.xpack.ml.notifications;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Auditor {

    public static final String NOTIFICATIONS_INDEX = ".ml-notifications";
    private static final Logger LOGGER = Loggers.getLogger(Auditor.class);

    private final Client client;
    private final ClusterService clusterService;

    public Auditor(Client client, ClusterService clusterService) {
        this.client = Objects.requireNonNull(client);
        this.clusterService = clusterService;
    }

    public void info(String jobId, String message) {
        indexDoc(AuditMessage.TYPE.getPreferredName(), AuditMessage.newInfo(jobId, message, clusterService.localNode().getName()));
    }

    public void warning(String jobId, String message) {
        indexDoc(AuditMessage.TYPE.getPreferredName(), AuditMessage.newWarning(jobId, message, clusterService.localNode().getName()));
    }

    public void error(String jobId, String message) {
        indexDoc(AuditMessage.TYPE.getPreferredName(), AuditMessage.newError(jobId, message, clusterService.localNode().getName()));
    }

    private void indexDoc(String type, ToXContent toXContent) {
        client.prepareIndex(NOTIFICATIONS_INDEX, type)
                .setSource(toXContentBuilder(toXContent))
                .execute(new ActionListener<IndexResponse>() {
                    @Override
                    public void onResponse(IndexResponse indexResponse) {
                        LOGGER.trace("Successfully persisted {}", type);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LOGGER.debug(new ParameterizedMessage("Error writing {}", new Object[]{type}, e));
                    }
                });
    }

    private XContentBuilder toXContentBuilder(ToXContent toXContent) {
        try {
            return toXContent.toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
