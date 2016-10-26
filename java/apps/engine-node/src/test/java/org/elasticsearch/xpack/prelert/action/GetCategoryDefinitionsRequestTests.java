package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class GetCategoryDefinitionsRequestTests extends AbstractStreamableTestCase<GetCategoryDefinitionsAction.Request> {

    @Override
    protected GetCategoryDefinitionsAction.Request createTestInstance() {
        GetCategoryDefinitionsAction.Request request = new GetCategoryDefinitionsAction.Request(randomAsciiOfLength(10));
        if (randomBoolean()) {
            request.setPagination(randomIntBetween(0, 128), randomIntBetween(128, 256));
        }
        return request;
    }

    @Override
    protected GetCategoryDefinitionsAction.Request createBlankInstance() {
        return new GetCategoryDefinitionsAction.Request();
    }
}
