/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
            CategoryDefinition categoryDefinition = new CategoryDefinition(randomAsciiOfLength(10));
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
