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

import org.elasticsearch.xpack.ml.action.GetDatafeedsStatsAction.Response;
import org.elasticsearch.xpack.ml.job.persistence.QueryPage;
import org.elasticsearch.xpack.ml.datafeed.Datafeed;
import org.elasticsearch.xpack.ml.datafeed.DatafeedStatus;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.List;

public class GetDatafeedsStatsActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        final Response result;

        int listSize = randomInt(10);
        List<Response.DatafeedStats> datafeedStatsList = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            String datafeedId = randomAsciiOfLength(10);
            DatafeedStatus datafeedStatus = randomFrom(DatafeedStatus.values());

            Response.DatafeedStats datafeedStats = new Response.DatafeedStats(datafeedId, datafeedStatus);
            datafeedStatsList.add(datafeedStats);
        }

        result = new Response(new QueryPage<>(datafeedStatsList, datafeedStatsList.size(), Datafeed.RESULTS_FIELD));

        return result;
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
