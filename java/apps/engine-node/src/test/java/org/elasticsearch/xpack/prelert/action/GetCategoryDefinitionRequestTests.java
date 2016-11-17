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

public class GetCategoryDefinitionRequestTests extends AbstractStreamableTestCase<GetCategoryDefinitionAction.Request> {

    @Override
    protected GetCategoryDefinitionAction.Request createTestInstance() {
        String jobId = randomAsciiOfLength(10);
        GetCategoryDefinitionAction.Request request = new GetCategoryDefinitionAction.Request(jobId);
        if (randomBoolean()) {
            request.setCategoryId(randomAsciiOfLength(10));
        } else {
            int from = randomInt(PageParams.MAX_FROM_SIZE_SUM);
            int maxSize = PageParams.MAX_FROM_SIZE_SUM - from;
            int size = randomInt(maxSize);
            request.setPageParams(new PageParams(from, size));
        }
        return request;
    }

    @Override
    protected GetCategoryDefinitionAction.Request createBlankInstance() {
        return new GetCategoryDefinitionAction.Request();
    }
}
