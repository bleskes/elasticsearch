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
package org.elasticsearch.xpack.ml.modelsnapshots;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.action.GetModelSnapshotsAction;
import org.elasticsearch.xpack.ml.job.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.persistence.QueryPage;
import org.elasticsearch.xpack.ml.job.quantiles.Quantiles;
import org.elasticsearch.xpack.ml.job.results.PageParams;

import java.util.Arrays;
import java.util.Date;

public class GetModelSnapshotsTests extends ESTestCase {

    public void testModelSnapshots_GivenNegativeFrom() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> new GetModelSnapshotsAction.Request("foo", null).setPageParams(new PageParams(-5, 10)));
        assertEquals("Parameter [from] cannot be < 0", e.getMessage());
    }

    public void testModelSnapshots_GivenNegativeSize() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> new GetModelSnapshotsAction.Request("foo", null).setPageParams(new PageParams(10, -5)));
        assertEquals("Parameter [size] cannot be < 0", e.getMessage());
    }

    public void testModelSnapshots_clearQuantiles() {
        ModelSnapshot m1 = new ModelSnapshot("jobId");
        m1.setQuantiles(new Quantiles("jobId", new Date(), "quantileState"));
        ModelSnapshot m2 = new ModelSnapshot("jobId");

        QueryPage<ModelSnapshot> page = new QueryPage<>(Arrays.asList(m1, m2), 2, new ParseField("field"));
        GetModelSnapshotsAction.TransportAction.clearQuantiles(page);
        assertEquals(2, page.results().size());
        for (ModelSnapshot modelSnapshot : page.results()) {
            assertNull(modelSnapshot.getQuantiles());
        }
    }
}
