package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

public class GetCategoryDefinitionResponseTests extends AbstractStreamableTestCase<GetCategoryDefinitionAction.Response> {

    @Override
    protected GetCategoryDefinitionAction.Response createTestInstance() {
        SingleDocument<CategoryDefinition> document;
        if (randomBoolean()) {
            document = SingleDocument.empty(CategoryDefinition.TYPE.getPreferredName());
        } else {
            CategoryDefinition categoryDefinition = new CategoryDefinition();
            categoryDefinition.setRegex(randomAsciiOfLength(10));
            categoryDefinition.setTerms(randomAsciiOfLength(10));
            categoryDefinition.setCategoryId(randomLong());
            document = new SingleDocument<>(CategoryDefinition.TYPE.getPreferredName(), categoryDefinition);
        }
        return new GetCategoryDefinitionAction.Response(document);
    }

    @Override
    protected GetCategoryDefinitionAction.Response createBlankInstance() {
        return new GetCategoryDefinitionAction.Response();
    }
}
