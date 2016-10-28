package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.RevertModelSnapshotAction.Response;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class RevertModelSnapshotActionResponseTests extends AbstractStreamableTestCase<RevertModelSnapshotAction.Response> {

    @Override
    protected Response createTestInstance() {
        if (randomBoolean()) {
            return new Response();
        } else {
            ModelSnapshot modelSnapshot = new ModelSnapshot();
            modelSnapshot.setDescription(randomAsciiOfLengthBetween(1, 20));
            return new RevertModelSnapshotAction.Response(modelSnapshot);
        }
    }

    @Override
    protected Response createBlankInstance() {
        return new RevertModelSnapshotAction.Response();
    }

}
