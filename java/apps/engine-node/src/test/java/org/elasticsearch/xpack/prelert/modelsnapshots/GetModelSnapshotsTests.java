/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.modelsnapshots;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.action.GetModelSnapshotsAction;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.PageParams;

import java.util.Collections;

import static org.elasticsearch.mock.orig.Mockito.when;
import static org.mockito.Mockito.mock;

public class GetModelSnapshotsTests extends ESTestCase {

    public void testModelSnapshots_GivenNegativeSkip() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> new GetModelSnapshotsAction.Request("foo").setPageParams(new PageParams(-5, 10)));
        assertEquals("Parameter [skip] cannot be < 0", e.getMessage());
    }

    public void testModelSnapshots_GivenNegativeTake() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> new GetModelSnapshotsAction.Request("foo").setPageParams(new PageParams(10, -5)));
        assertEquals("Parameter [take] cannot be < 0", e.getMessage());
    }

    public void testModelSnapshots_GivenNoStartOrEndParams() {
        ModelSnapshot modelSnapshot = new ModelSnapshot(randomAsciiOfLengthBetween(1, 20));
        QueryPage<ModelSnapshot> queryResult = new QueryPage<>(Collections.singletonList(modelSnapshot), 300);

        JobProvider jobProvider = mock(JobProvider.class);
        when(jobProvider.modelSnapshots("foo", 0, 100, null, null, null, true, null, null)).thenReturn(queryResult);

        GetModelSnapshotsAction.Request request = new GetModelSnapshotsAction.Request("foo");
        request.setPageParams(new PageParams(0, 100));
        request.setDescOrder(true);

        QueryPage<ModelSnapshot> page = GetModelSnapshotsAction.TransportAction.doGetPage(jobProvider, request);
        assertEquals(300, page.hitCount());
    }

    public void testModelSnapshots_GivenEpochStartAndEpochEndParams() {
        ModelSnapshot modelSnapshot = new ModelSnapshot(randomAsciiOfLengthBetween(1, 20));
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

    public void testModelSnapshots_GivenIsoWithMillisStartAndEpochEndParams() {
        ModelSnapshot modelSnapshot = new ModelSnapshot(randomAsciiOfLengthBetween(1, 20));
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

    public void testModelSnapshots_GivenIsoWithoutMillisStartAndEpochEndParams() {
        ModelSnapshot modelSnapshot = new ModelSnapshot(randomAsciiOfLengthBetween(1, 20));
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
