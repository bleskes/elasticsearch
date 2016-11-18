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

import org.elasticsearch.xpack.prelert.action.GetListAction.Response;
import org.elasticsearch.xpack.prelert.lists.ListDocument;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.util.Collections;

public class GetListActionResponseTests extends AbstractStreamableTestCase<GetListAction.Response> {

    @Override
    protected Response createTestInstance() {
        final SingleDocument<ListDocument> result;
        if (randomBoolean()) {
            result = SingleDocument.empty(ListDocument.TYPE.getPreferredName());
        } else {
            result = new SingleDocument<>(ListDocument.TYPE.getPreferredName(),
                    new ListDocument(randomAsciiOfLengthBetween(1, 20), Collections.singletonList(randomAsciiOfLengthBetween(1, 20))));
        }
        return new Response(result);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
