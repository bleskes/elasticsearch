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

public class GetCategoryDefinitionsResponseTests extends AbstractStreamableTestCase<GetCategoryDefinitionsAction.Response> {

    @Override
    protected GetCategoryDefinitionsAction.Response createTestInstance() {
        QueryPage<CategoryDefinition> queryPage =
                new QueryPage<>(Collections.singletonList(new CategoryDefinition(randomAsciiOfLength(10))), 1L);
        return new GetCategoryDefinitionsAction.Response(queryPage);
    }

    @Override
    protected GetCategoryDefinitionsAction.Response createBlankInstance() {
        return new GetCategoryDefinitionsAction.Response();
    }
}
