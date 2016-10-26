package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class GetCategoryDefinitionRequestTests extends AbstractStreamableTestCase<GetCategoryDefinitionAction.Request> {

    @Override
    protected GetCategoryDefinitionAction.Request createTestInstance() {
        String jobId = randomAsciiOfLength(10);
        String categoryId = randomAsciiOfLength(10);
        return new GetCategoryDefinitionAction.Request(jobId, categoryId);
    }

    @Override
    protected GetCategoryDefinitionAction.Request createBlankInstance() {
        return new GetCategoryDefinitionAction.Request();
    }
}
