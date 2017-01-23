/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.job.results.CategoryDefinition;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.util.Collections;

public class GetCategoriesResponseTests extends AbstractStreamableTestCase<GetCategoriesAction.Response> {

    @Override
    protected GetCategoriesAction.Response createTestInstance() {
        CategoryDefinition definition = new CategoryDefinition(randomAsciiOfLength(10));
        QueryPage<CategoryDefinition> queryPage =
                new QueryPage<>(Collections.singletonList(definition), 1L, CategoryDefinition.RESULTS_FIELD);
        return new GetCategoriesAction.Response(queryPage);
    }

    @Override
    protected GetCategoriesAction.Response createBlankInstance() {
        return new GetCategoriesAction.Response();
    }
}
