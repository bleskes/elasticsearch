package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.PutModelSnapshotDescriptionAction.Response;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class PutModelSnapshotDescriptionActionResponseTests extends AbstractStreamableTestCase<PutModelSnapshotDescriptionAction.Response> {

    @Override
    protected Response createTestInstance() {
        ModelSnapshot snapshot = new ModelSnapshot();
        snapshot.setDescription(randomAsciiOfLengthBetween(1, 20));
        return new Response(snapshot);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
