package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import java.util.Collections;

public class GetCategoryDefinitionsResponseTests extends AbstractStreamableTestCase<GetCategoryDefinitionsAction.Response> {

    @Override
    protected GetCategoryDefinitionsAction.Response createTestInstance() {
        QueryPage<CategoryDefinition> queryPage = new QueryPage<>(Collections.singletonList(new CategoryDefinition()), 1L);
        return new GetCategoryDefinitionsAction.Response(queryPage);
    }

    @Override
    protected GetCategoryDefinitionsAction.Response createBlankInstance() {
        return new GetCategoryDefinitionsAction.Response();
    }
}
