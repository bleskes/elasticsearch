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
