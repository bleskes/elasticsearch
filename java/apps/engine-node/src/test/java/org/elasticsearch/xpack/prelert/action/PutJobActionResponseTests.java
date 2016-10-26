package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.PutJobAction.Response;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class PutJobActionResponseTests extends AbstractStreamableTestCase<Response> {

    @Override
    protected Response createTestInstance() {
        JobConfiguration jobConfiguration = new JobConfiguration(randomAsciiOfLength(10));
        jobConfiguration.setIgnoreDowntime(IgnoreDowntime.NEVER);
        return new Response(jobConfiguration.build());
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
