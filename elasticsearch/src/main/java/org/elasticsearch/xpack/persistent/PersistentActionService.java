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
package org.elasticsearch.xpack.persistent;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.node.tasks.cancel.CancelTasksRequest;
import org.elasticsearch.action.admin.cluster.node.tasks.cancel.CancelTasksResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.tasks.TaskId;

/**
 * Service responsible for executing restartable actions that can survive disappearance of a coordinating and executor nodes.
 */
public class PersistentActionService extends AbstractComponent {

    private final Client client;
    private final ClusterService clusterService;

    public PersistentActionService(Settings settings, ClusterService clusterService, Client client) {
        super(settings);
        this.client = client;
        this.clusterService = clusterService;
    }

    public <Request extends PersistentActionRequest> void sendRequest(String action, Request request,
                                                                      ActionListener<PersistentActionResponse> listener) {
        StartPersistentTaskAction.Request startRequest = new StartPersistentTaskAction.Request(action, request);
        try {
            client.execute(StartPersistentTaskAction.INSTANCE, startRequest, listener);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    public void sendCompletionNotification(long taskId, Exception failure,
                                           ActionListener<CompletionPersistentTaskAction.Response> listener) {
        CompletionPersistentTaskAction.Request restartRequest = new CompletionPersistentTaskAction.Request(taskId, failure);
        try {
            client.execute(CompletionPersistentTaskAction.INSTANCE, restartRequest, listener);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    public void sendCancellation(long taskId, ActionListener<CancelTasksResponse> listener) {
        DiscoveryNode localNode = clusterService.localNode();
        CancelTasksRequest cancelTasksRequest = new CancelTasksRequest();
        cancelTasksRequest.setTaskId(new TaskId(localNode.getId(), taskId));
        cancelTasksRequest.setReason("persistent action was removed");
        try {
            client.admin().cluster().cancelTasks(cancelTasksRequest, listener);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}