package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.job.results.PageParams;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class GetCategoryDefinitionsRequestTests extends AbstractStreamableTestCase<GetCategoryDefinitionsAction.Request> {

    @Override
    protected GetCategoryDefinitionsAction.Request createTestInstance() {
        GetCategoryDefinitionsAction.Request request = new GetCategoryDefinitionsAction.Request(randomAsciiOfLength(10));
        if (randomBoolean()) {
            int skip = randomInt(PageParams.MAX_SKIP_TAKE_SUM);
            int maxTake = PageParams.MAX_SKIP_TAKE_SUM - skip;
            int take = randomInt(maxTake);
            request.setPageParams(new PageParams(skip, take));
        }
        return request;
    }

    @Override
    protected GetCategoryDefinitionsAction.Request createBlankInstance() {
        return new GetCategoryDefinitionsAction.Request();
    }
}
