package org.elasticsearch.xpack.prelert.modelsnapshots;

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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.action.GetModelSnapshotsAction;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.PageParams;

import java.util.Collections;

import static org.elasticsearch.mock.orig.Mockito.when;
import static org.mockito.Mockito.mock;

public class GetModelSnapshotsTest extends ESTestCase {

    public void testModelSnapshots_GivenNegativeSkip() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class,
                () -> new GetModelSnapshotsAction.Request("foo").setPageParams(new PageParams(-5, 10)));
        assertEquals("Parameter [skip] cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_SKIP_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testModelSnapshots_GivenNegativeTake() {
        ElasticsearchException e = expectThrows(ElasticsearchException.class,
                () -> new GetModelSnapshotsAction.Request("foo").setPageParams(new PageParams(10, -5)));
        assertEquals("Parameter [take] cannot be < 0", e.getMessage());
        assertEquals(ErrorCodes.INVALID_TAKE_PARAM.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testModelSnapshots_GivenNoStartOrEndParams() throws JobException {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Collections.singletonList(modelSnapshot), 300);

        JobProvider jobProvider = mock(JobProvider.class);
        when(jobProvider.modelSnapshots("foo", 0, 100, null, null, null, true, null, null)).thenReturn(queryResult);

        GetModelSnapshotsAction.Request request = new GetModelSnapshotsAction.Request("foo");
        request.setPageParams(new PageParams(0, 100));
        request.setDescOrder(true);

        QueryPage<ModelSnapshot> page = GetModelSnapshotsAction.TransportAction.doGetPage(jobProvider, request);
        assertEquals(300, page.hitCount());
    }

    public void testModelSnapshots_GivenEpochStartAndEpochEndParams() throws JobException {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Collections.singletonList(modelSnapshot), 300);

        JobProvider jobProvider = mock(JobProvider.class);
        when(jobProvider.modelSnapshots("foo", 0, 100, "1", "2", null, true, null, null)).thenReturn(queryResult);

        GetModelSnapshotsAction.Request request = new GetModelSnapshotsAction.Request("foo");
        request.setPageParams(new PageParams(0, 100));
        request.setStart("1");
        request.setEnd("2");
        request.setDescOrder(true);

        QueryPage<ModelSnapshot> page = GetModelSnapshotsAction.TransportAction.doGetPage(jobProvider, request);
        assertEquals(300, page.hitCount());
    }

    public void testModelSnapshots_GivenIsoWithMillisStartAndEpochEndParams() throws JobException {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Collections.singletonList(modelSnapshot), 300);

        JobProvider jobProvider = mock(JobProvider.class);
        when(jobProvider.modelSnapshots("foo", 0, 100, "2015-01-01T12:00:00.042Z", "2015-01-01T13:00:00.142+00:00", null, true, null, null))
        .thenReturn(queryResult);

        GetModelSnapshotsAction.Request request = new GetModelSnapshotsAction.Request("foo");
        request.setPageParams(new PageParams(0, 100));
        request.setStart("2015-01-01T12:00:00.042Z");
        request.setEnd("2015-01-01T13:00:00.142+00:00");
        request.setDescOrder(true);

        QueryPage<ModelSnapshot> page = GetModelSnapshotsAction.TransportAction.doGetPage(jobProvider, request);
        assertEquals(300, page.hitCount());
    }

    public void testModelSnapshots_GivenIsoWithoutMillisStartAndEpochEndParams() throws JobException {
        ModelSnapshot modelSnapshot = new ModelSnapshot();
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Collections.singletonList(modelSnapshot), 300);

        JobProvider jobProvider = mock(JobProvider.class);
        when(jobProvider.modelSnapshots("foo", 0, 100, "2015-01-01T12:00:00Z", "2015-01-01T13:00:00Z", null, true, null, null))
        .thenReturn(queryResult);

        GetModelSnapshotsAction.Request request = new GetModelSnapshotsAction.Request("foo");
        request.setPageParams(new PageParams(0, 100));
        request.setStart("2015-01-01T12:00:00Z");
        request.setEnd("2015-01-01T13:00:00Z");
        request.setDescOrder(true);

        QueryPage<ModelSnapshot> page = GetModelSnapshotsAction.TransportAction.doGetPage(jobProvider, request);
        assertEquals(300, page.hitCount());
    }
}
