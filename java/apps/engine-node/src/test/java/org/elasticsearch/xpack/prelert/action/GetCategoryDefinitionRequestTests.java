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

import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

public class GetCategoryDefinitionRequestTests extends AbstractStreamableTestCase<GetCategoryDefinitionAction.Request> {

    @Override
    protected GetCategoryDefinitionAction.Request createTestInstance() {
        String jobId = randomAsciiOfLength(10);
        String categoryId = randomAsciiOfLength(10);
        return new GetCategoryDefinitionAction.Request(jobId, categoryId);
    }

    @Override
    protected GetCategoryDefinitionAction.Request createBlankInstance() {
        return new GetCategoryDefinitionAction.Request();
    }
}
