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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.xpack.ml.action.GetInfluencersAction.Response;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.job.results.Influencer;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GetInfluencersActionResponseTests extends AbstractStreamableTestCase<GetInfluencersAction.Response> {

    @Override
    protected Response createTestInstance() {
        int listSize = randomInt(10);
        List<Influencer> hits = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            Influencer influencer = new Influencer(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20),
                    randomAsciiOfLengthBetween(1, 20), new Date(randomPositiveLong()), randomPositiveLong(), j + 1);
            influencer.setAnomalyScore(randomDouble());
            influencer.setInitialAnomalyScore(randomDouble());
            influencer.setProbability(randomDouble());
            influencer.setInterim(randomBoolean());
            hits.add(influencer);
        }
        QueryPage<Influencer> buckets = new QueryPage<>(hits, listSize, Influencer.RESULTS_FIELD);
        return new Response(buckets);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
