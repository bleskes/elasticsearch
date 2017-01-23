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

import org.elasticsearch.xpack.ml.action.RevertModelSnapshotAction.Response;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

public class RevertModelSnapshotActionResponseTests extends AbstractStreamableTestCase<RevertModelSnapshotAction.Response> {

    @Override
    protected Response createTestInstance() {
        ModelSnapshot modelSnapshot = new ModelSnapshot(randomAsciiOfLengthBetween(1, 20));
        modelSnapshot.setDescription(randomAsciiOfLengthBetween(1, 20));
        return new RevertModelSnapshotAction.Response(modelSnapshot);
    }

    @Override
    protected Response createBlankInstance() {
        return new RevertModelSnapshotAction.Response();
    }

}
