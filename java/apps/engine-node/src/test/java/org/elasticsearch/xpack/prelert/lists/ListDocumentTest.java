package org.elasticsearch.xpack.prelert.lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

public class ListDocumentTest extends AbstractSerializingTestCase<ListDocument> {

    @Override
    protected ListDocument createTestInstance() {
        int size = randomInt(10);
        List<String> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(randomAsciiOfLengthBetween(1, 20));
        }
        return new ListDocument(randomAsciiOfLengthBetween(1, 20), items);
    }

    @Override
    protected Reader<ListDocument> instanceReader() {
        return ListDocument::new;
    }

    @Override
    protected ListDocument parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return ListDocument.PARSER.apply(parser, () -> matcher);
    }

    public void testNullId() {
        NullPointerException ex = expectThrows(NullPointerException.class, () -> new ListDocument(null, Collections.emptyList()));
        assertEquals(ListDocument.ID.getPreferredName() + " must not be null", ex.getMessage());
    }

    public void testNullItems() {
        NullPointerException ex = expectThrows(NullPointerException.class, () -> new ListDocument(randomAsciiOfLengthBetween(1, 20), null));
        assertEquals(ListDocument.ITEMS.getPreferredName() + " must not be null", ex.getMessage());
    }

}
