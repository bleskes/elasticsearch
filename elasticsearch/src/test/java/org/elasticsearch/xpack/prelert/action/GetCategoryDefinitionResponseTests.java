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

import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import java.util.Collections;

public class GetCategoryDefinitionResponseTests extends AbstractStreamableTestCase<GetCategoriesDefinitionAction.Response> {

    @Override
    protected GetCategoriesDefinitionAction.Response createTestInstance() {
        CategoryDefinition definition = new CategoryDefinition(randomAsciiOfLength(10));
        QueryPage<CategoryDefinition> queryPage =
                new QueryPage<>(Collections.singletonList(definition), 1L, CategoryDefinition.RESULTS_FIELD);
        return new GetCategoriesDefinitionAction.Response(queryPage);
    }

    @Override
    protected GetCategoriesDefinitionAction.Response createBlankInstance() {
        return new GetCategoriesDefinitionAction.Response();
    }
}
