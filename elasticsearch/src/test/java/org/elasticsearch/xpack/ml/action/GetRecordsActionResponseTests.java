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

import org.elasticsearch.xpack.ml.action.GetRecordsAction.Response;
import org.elasticsearch.xpack.ml.job.persistence.QueryPage;
import org.elasticsearch.xpack.ml.job.results.AnomalyRecord;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GetRecordsActionResponseTests extends AbstractStreamableTestCase<GetRecordsAction.Response> {

    @Override
    protected Response createTestInstance() {
        int listSize = randomInt(10);
        List<AnomalyRecord> hits = new ArrayList<>(listSize);
        String jobId = randomAsciiOfLengthBetween(1, 20);
        for (int j = 0; j < listSize; j++) {
            AnomalyRecord record = new AnomalyRecord(jobId, new Date(), 600, j + 1);
            hits.add(record);
        }
        QueryPage<AnomalyRecord> snapshots = new QueryPage<>(hits, listSize, AnomalyRecord.RESULTS_FIELD);
        return new Response(snapshots);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
