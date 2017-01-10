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

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.ml.action.PutListAction.Request;
import org.elasticsearch.xpack.ml.lists.ListDocument;
import org.elasticsearch.xpack.ml.support.AbstractStreamableXContentTestCase;

import java.util.ArrayList;
import java.util.List;

public class CreateListActionRequestTests extends AbstractStreamableXContentTestCase<PutListAction.Request> {

    @Override
    protected Request createTestInstance() {
        int size = randomInt(10);
        List<String> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(randomAsciiOfLengthBetween(1, 20));
        }
        ListDocument listDocument = new ListDocument(randomAsciiOfLengthBetween(1, 20), items);
        return new PutListAction.Request(listDocument);
    }

    @Override
    protected Request createBlankInstance() {
        return new PutListAction.Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return PutListAction.Request.parseRequest(parser, () -> matcher);
    }

}
