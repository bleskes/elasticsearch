/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.upgrade.actions;

import org.elasticsearch.test.AbstractStreamableTestCase;
import org.elasticsearch.xpack.upgrade.actions.IndexUpgradeAction.Request;

import java.util.Collections;

public class IndexUpgradeActionRequestTests extends AbstractStreamableTestCase<Request> {
    @Override
    protected Request createTestInstance() {
        Request request = new Request(randomAlphaOfLength(10));
        if (randomBoolean()) {
            request.extraParams(Collections.singletonMap(randomAlphaOfLength(10), randomAlphaOfLength(20)));
        }
        return request;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }
}
