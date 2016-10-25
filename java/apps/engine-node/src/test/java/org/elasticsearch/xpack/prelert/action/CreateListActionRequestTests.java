package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.action.CreateListAction.Request;
import org.elasticsearch.xpack.prelert.lists.ListDocument;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableXContentTestCase;

import java.util.ArrayList;
import java.util.List;

public class CreateListActionRequestTests extends AbstractStreamableXContentTestCase<CreateListAction.Request> {

    @Override
    protected Request createTestInstance() {
        int size = randomInt(10);
        List<String> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(randomAsciiOfLengthBetween(1, 20));
        }
        ListDocument listDocument = new ListDocument(randomAsciiOfLengthBetween(1, 20), items);
        return new CreateListAction.Request(listDocument);
    }

    @Override
    protected Request createBlankInstance() {
        return new CreateListAction.Request();
    }

    @Override
    protected Request parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return CreateListAction.Request.parseRequest(parser, () -> matcher);
    }

}
