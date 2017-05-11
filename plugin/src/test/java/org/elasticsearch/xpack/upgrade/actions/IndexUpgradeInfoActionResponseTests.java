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
import org.elasticsearch.xpack.upgrade.UpgradeActionRequired;
import org.elasticsearch.xpack.upgrade.actions.IndexUpgradeInfoAction.Response;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class IndexUpgradeInfoActionResponseTests extends AbstractStreamableTestCase<Response> {


    @Override
    protected Response createTestInstance() {
        int actionsCount = randomIntBetween(0, 5);
        Map<String, UpgradeActionRequired> actions = new HashMap<>(actionsCount);
        for (int i = 0; i < actionsCount; i++) {
            actions.put(randomAlphaOfLength(10), randomFrom(EnumSet.allOf(UpgradeActionRequired.class)));
        }
        return new Response(actions);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }
}
