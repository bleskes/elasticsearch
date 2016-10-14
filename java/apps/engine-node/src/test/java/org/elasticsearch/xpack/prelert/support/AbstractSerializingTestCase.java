package org.elasticsearch.xpack.prelert.support;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.test.ESTestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import static org.hamcrest.Matchers.equalTo;

public abstract class AbstractSerializingTestCase<T extends ToXContent & Writeable> extends ESTestCase {
    private static final int NUMBER_OF_TESTQUERIES = 20;

    protected abstract T createTestInstance();

    protected abstract Reader<T> instanceReader();

    public void testEqualsAndHashcode() throws IOException {
        for (int runs = 0; runs < NUMBER_OF_TESTQUERIES; runs++) {
            T firstInstance = createTestInstance();
            assertFalse("query is equal to null", firstInstance.equals(null));
            assertFalse("query is equal to incompatible type", firstInstance.equals(""));
            assertTrue("query is not equal to self", firstInstance.equals(firstInstance));
            assertThat("same query's hashcode returns different values if called multiple times", firstInstance.hashCode(),
                    equalTo(firstInstance.hashCode()));

            T secondInstance = copyInstance(firstInstance);
            assertTrue("query is not equal to self", secondInstance.equals(secondInstance));
            assertTrue("query is not equal to its copy", firstInstance.equals(secondInstance));
            assertTrue("equals is not symmetric", secondInstance.equals(firstInstance));
            assertThat("query copy's hashcode is different from original hashcode", secondInstance.hashCode(),
                    equalTo(firstInstance.hashCode()));

            T thirdInstance = copyInstance(secondInstance);
            assertTrue("query is not equal to self", thirdInstance.equals(thirdInstance));
            assertTrue("query is not equal to its copy", secondInstance.equals(thirdInstance));
            assertThat("query copy's hashcode is different from original hashcode", secondInstance.hashCode(),
                    equalTo(thirdInstance.hashCode()));
            assertTrue("equals is not transitive", firstInstance.equals(thirdInstance));
            assertThat("query copy's hashcode is different from original hashcode", firstInstance.hashCode(),
                    equalTo(thirdInstance.hashCode()));
            assertTrue("equals is not symmetric", thirdInstance.equals(secondInstance));
            assertTrue("equals is not symmetric", thirdInstance.equals(firstInstance));
        }
    }

    /**
     * Generic test that creates new instance from the test instance and checks
     * both for equality and asserts equality on the two queries.
     */
    public void testFromXContent() throws IOException {
        for (int runs = 0; runs < NUMBER_OF_TESTQUERIES; runs++) {
            T testInstance = createTestInstance();
            XContentBuilder builder = toXContent(testInstance, randomFrom(XContentType.values()));
            XContentBuilder shuffled = shuffleXContent(builder, shuffleProtectedFields());
            assertParsedInstance(shuffled.bytes(), testInstance);
            for (Map.Entry<String, T> alternateVersion : getAlternateVersions().entrySet()) {
                String instanceAsString = alternateVersion.getKey();
                assertParsedInstance(new BytesArray(instanceAsString), alternateVersion.getValue());
            }
        }
    }

    /**
     * Test serialization and deserialization of the test query.
     */
    public void testSerialization() throws IOException {
        for (int runs = 0; runs < NUMBER_OF_TESTQUERIES; runs++) {
            T testInstance = createTestInstance();
            assertSerialization(testInstance);
        }
    }

    // NORELEASE remove this test method when Jackson is gone
    public void testJacksonSerialisation() throws Exception {
        T testInstance = createTestInstance();
        ObjectMapper objectMapper = new ObjectMapper();
        String instanceStr = objectMapper.writeValueAsString(testInstance);
        ObjectReader objectReader = objectMapper.readerFor(testInstance.getClass());
        T deserializedInstance = objectReader.readValue(instanceStr);
        assertEquals(testInstance, deserializedInstance);
        assertEquals(testInstance.hashCode(), deserializedInstance.hashCode());
        assertNotSame(testInstance, deserializedInstance);
    }

    /**
     * Serialize the given query builder and asserts that both are equal
     */
    protected T assertSerialization(T testInstance) throws IOException {
        T deserializedInstance = copyInstance(testInstance);
        assertEquals(testInstance, deserializedInstance);
        assertEquals(testInstance.hashCode(), deserializedInstance.hashCode());
        assertNotSame(testInstance, deserializedInstance);
        return deserializedInstance;
    }

    private void assertParsedInstance(BytesReference queryAsBytes, T expectedInstance)
            throws IOException {
        XContentParser parser = XContentFactory.xContent(queryAsBytes).createParser(queryAsBytes);
        T newInstance = parseQuery(parser, ParseFieldMatcher.STRICT);
        assertNotSame(newInstance, expectedInstance);
        assertEquals(expectedInstance, newInstance);
        assertEquals(expectedInstance.hashCode(), newInstance.hashCode());
    }

    private T parseQuery(XContentParser parser, ParseFieldMatcher matcher) throws IOException {
        T parsedInstance = parseInstance(parser, matcher);
        assertNull(parser.nextToken());
        return parsedInstance;
    }

    protected abstract T parseInstance(XContentParser parser, ParseFieldMatcher matcher);

    /**
     * Subclasses can override this method and return an array of fieldnames
     * which should be protected from recursive random shuffling in the
     * {@link #testFromXContent()} test case
     */
    protected String[] shuffleProtectedFields() {
        return Strings.EMPTY_ARRAY;
    }

    protected static <T extends ToXContent & Writeable> XContentBuilder toXContent(T instance, XContentType contentType)
            throws IOException {
        XContentBuilder builder = XContentFactory.contentBuilder(contentType);
        if (randomBoolean()) {
            builder.prettyPrint();
        }
        instance.toXContent(builder, ToXContent.EMPTY_PARAMS);
        return builder;
    }

    /**
     * Returns alternate string representation of the query that need to be
     * tested as they are never used as output of
     * {@link QueryBuilder#toXContent(XContentBuilder, ToXContent.Params)}. By
     * default there are no alternate versions.
     */
    protected Map<String, T> getAlternateVersions() {
        return Collections.emptyMap();
    }

    private T copyInstance(T instance) throws IOException {
        try (BytesStreamOutput output = new BytesStreamOutput()) {
            instance.writeTo(output);
            try (StreamInput in = new NamedWriteableAwareStreamInput(output.bytes().streamInput(),
                    new NamedWriteableRegistry(Collections.emptyList()))) {
                return instanceReader().read(in);
            }
        }
    }
}
