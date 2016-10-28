package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.GetRecordsAction.Response;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.List;

public class GetRecordsActionResponseTests extends AbstractStreamableTestCase<GetRecordsAction.Response> {

    @Override
    protected Response createTestInstance() {
        int listSize = randomInt(10);
        List<AnomalyRecord> hits = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            AnomalyRecord record = new AnomalyRecord();
            record.setId(randomAsciiOfLengthBetween(1, 20));
            hits.add(record);
        }
        QueryPage<AnomalyRecord> snapshots = new QueryPage<>(hits, listSize);
        return new Response(snapshots);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
