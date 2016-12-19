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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.PutModelSnapshotDescriptionAction.Response;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class PutModelSnapshotDescriptionActionResponseTests extends AbstractStreamableTestCase<PutModelSnapshotDescriptionAction.Response> {

    @Override
    protected Response createTestInstance() {
        ModelSnapshot snapshot = new ModelSnapshot(randomAsciiOfLengthBetween(1, 20));
        snapshot.setDescription(randomAsciiOfLengthBetween(1, 20));
        return new Response(snapshot);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
