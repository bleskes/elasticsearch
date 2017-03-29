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

package org.elasticsearch.xpack.monitoring;

import org.elasticsearch.client.Client;
import org.elasticsearch.license.TribeTransportTestCase;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkAction;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkRequest;

import java.util.Collections;
import java.util.List;

public class MonitoringTribeTests extends TribeTransportTestCase {

    @Override
    protected List<String> enabledFeatures() {
        return Collections.singletonList(Monitoring.NAME);
    }

    @Override
    protected void verifyActionOnClientNode(Client client) throws Exception {
        assertMonitoringTransportActionsWorks(client);
    }

    @Override
    protected void verifyActionOnMasterNode(Client masterClient) throws Exception {
        assertMonitoringTransportActionsWorks(masterClient);
    }

    @Override
    protected void verifyActionOnDataNode(Client dataNodeClient) throws Exception {
        assertMonitoringTransportActionsWorks(dataNodeClient);
    }

    private static void assertMonitoringTransportActionsWorks(Client client) throws Exception {
        client.execute(MonitoringBulkAction.INSTANCE, new MonitoringBulkRequest());
    }

    @Override
    protected void verifyActionOnTribeNode(Client tribeClient) {
        failAction(tribeClient, MonitoringBulkAction.INSTANCE);
    }
}
