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

package org.elasticsearch.shield.action;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.search.ClearScrollAction;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.transport.KnownActionsTests;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;

public class ShieldActionMapperTests extends ElasticsearchTestCase {

    @Test
    public void testThatAllOrdinaryActionsRemainTheSame() {
        List<String> actions = new ArrayList<>();
        actions.addAll(KnownActionsTests.loadKnownActions());
        actions.addAll(KnownActionsTests.loadKnownHandlers());

        ShieldActionMapper shieldActionMapper = new ShieldActionMapper();
        int iterations = randomIntBetween(10, 100);
        for (int i = 0; i < iterations; i++) {
            String randomAction;
            do {
                if (randomBoolean()) {
                    randomAction = randomFrom(actions);
                } else {
                    randomAction = randomAsciiOfLength(randomIntBetween(1, 30));
                }
            } while (randomAction.equals(ClearScrollAction.NAME) ||
                    randomAction.equals(AnalyzeAction.NAME));

            assertThat(shieldActionMapper.action(randomAction, null), equalTo(randomAction));
        }
    }

    @Test
    public void testClearScroll() {
        ShieldActionMapper shieldActionMapper = new ShieldActionMapper();
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        int scrollIds = randomIntBetween(1, 10);
        for (int i = 0; i < scrollIds; i++) {
            clearScrollRequest.addScrollId(randomAsciiOfLength(randomIntBetween(1, 30)));
        }
        assertThat(shieldActionMapper.action(ClearScrollAction.NAME, clearScrollRequest), equalTo(ClearScrollAction.NAME));
    }

    @Test
    public void testClearScrollAll() {
        ShieldActionMapper shieldActionMapper = new ShieldActionMapper();
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        int scrollIds = randomIntBetween(0, 10);
        for (int i = 0; i < scrollIds; i++) {
            clearScrollRequest.addScrollId(randomAsciiOfLength(randomIntBetween(1, 30)));
        }
        clearScrollRequest.addScrollId("_all");
        //make sure that wherever the _all is among the scroll ids the action name gets translated
        Collections.shuffle(clearScrollRequest.getScrollIds(), getRandom());

        assertThat(shieldActionMapper.action(ClearScrollAction.NAME, clearScrollRequest), equalTo(ShieldActionMapper.CLUSTER_PERMISSION_SCROLL_CLEAR_ALL_NAME));
    }

    @Test
    public void testIndicesAnalyze() {
        ShieldActionMapper shieldActionMapper = new ShieldActionMapper();
        AnalyzeRequest analyzeRequest;
        if (randomBoolean()) {
            analyzeRequest = new AnalyzeRequest(randomAsciiOfLength(randomIntBetween(1, 30)), "text");
        } else {
            analyzeRequest = new AnalyzeRequest("text");
            analyzeRequest.index(randomAsciiOfLength(randomIntBetween(1, 30)));
        }
        assertThat(shieldActionMapper.action(AnalyzeAction.NAME, analyzeRequest), equalTo(AnalyzeAction.NAME));
    }

    @Test
    public void testClusterAnalyze() {
        ShieldActionMapper shieldActionMapper = new ShieldActionMapper();
        AnalyzeRequest analyzeRequest = new AnalyzeRequest("text");
        assertThat(shieldActionMapper.action(AnalyzeAction.NAME, analyzeRequest), equalTo(ShieldActionMapper.CLUSTER_PERMISSION_ANALYZE));
    }
}
