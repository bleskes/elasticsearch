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

import org.elasticsearch.xpack.ml.action.GetFiltersAction.Response;
import org.elasticsearch.xpack.ml.action.util.QueryPage;
import org.elasticsearch.xpack.ml.job.config.MlFilter;
import org.elasticsearch.xpack.ml.support.AbstractStreamableTestCase;

import java.util.Collections;

public class GetFiltersActionResponseTests extends AbstractStreamableTestCase<GetFiltersAction.Response> {

    @Override
    protected Response createTestInstance() {
        final QueryPage<MlFilter> result;

        MlFilter doc = new MlFilter(
                randomAlphaOfLengthBetween(1, 20), Collections.singletonList(randomAlphaOfLengthBetween(1, 20)));
        result = new QueryPage<>(Collections.singletonList(doc), 1, MlFilter.RESULTS_FIELD);
        return new Response(result);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
