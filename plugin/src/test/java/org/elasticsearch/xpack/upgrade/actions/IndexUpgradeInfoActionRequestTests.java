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

import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.test.AbstractStreamableTestCase;
import org.elasticsearch.xpack.upgrade.actions.IndexUpgradeInfoAction.Request;

import java.util.Collections;

public class IndexUpgradeInfoActionRequestTests extends AbstractStreamableTestCase<Request> {
    @Override
    protected Request createTestInstance() {
        int indexCount = randomInt(4);
        String[] indices = new String[indexCount];
        for (int i = 0; i < indexCount; i++) {
            indices[i] = randomAlphaOfLength(10);
        }
        Request request = new Request(indices);
        if (randomBoolean()) {
            request.extraParams(Collections.singletonMap(randomAlphaOfLength(10), randomAlphaOfLength(20)));
        }
        if (randomBoolean()) {
            request.indicesOptions(IndicesOptions.fromOptions(randomBoolean(), randomBoolean(), randomBoolean(), randomBoolean()));
        }
        return request;
    }

    @Override
    protected Request createBlankInstance() {
        return new Request();
    }
}
