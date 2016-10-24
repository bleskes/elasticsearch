
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.DataDescription.DataFormat;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;;

public class DataDescriptionTest extends AbstractSerializingTestCase<DataDescription> {


    public void testTransform_GivenJson() {
        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.JSON);
        assertTrue(dd.transform());
    }


    public void testTransform_GivenDelimitedAndEpoch() {
        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setTimeFormat("epoch");
        assertFalse(dd.transform());
    }


    public void testTransform_GivenDelimitedAndEpochMs() {
        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setTimeFormat("epoch_ms");
        assertTrue(dd.transform());
    }


    public void testIsTransformTime_GivenTimeFormatIsNull() {
        DataDescription dd = new DataDescription();
        dd.setTimeFormat(null);
        assertFalse(dd.isTransformTime());
    }


    public void testIsTransformTime_GivenTimeFormatIsEpoch() {
        DataDescription dd = new DataDescription();
        dd.setTimeFormat("epoch");
        assertFalse(dd.isTransformTime());
    }


    public void testIsTransformTime_GivenTimeFormatIsEpochMs() {
        DataDescription dd = new DataDescription();
        dd.setTimeFormat("epoch_ms");
        assertTrue(dd.isTransformTime());
    }


    public void testIsTransformTime_GivenTimeFormatPattern() {
        DataDescription dd = new DataDescription();
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        assertTrue(dd.isTransformTime());
    }


    public void testEquals_GivenEqual() {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('"');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(',');

        assertTrue(description1.equals(description2));
        assertTrue(description2.equals(description1));
    }


    public void testEquals_GivenDifferentDateFormat() {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.DELIMITED);
        description2.setQuoteCharacter('"');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(',');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }


    public void testEquals_GivenDifferentQuoteCharacter() {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('\'');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(',');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }


    public void testEquals_GivenDifferentTimeField() {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('"');
        description2.setTimeField("time");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(',');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }


    public void testEquals_GivenDifferentTimeFormat() {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('"');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch_ms");
        description2.setFieldDelimiter(',');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }


    public void testEquals_GivenDifferentFieldDelimiter() {
        DataDescription description1 = new DataDescription();
        description1.setFormat(DataFormat.JSON);
        description1.setQuoteCharacter('"');
        description1.setTimeField("timestamp");
        description1.setTimeFormat("epoch");
        description1.setFieldDelimiter(',');

        DataDescription description2 = new DataDescription();
        description2.setFormat(DataFormat.JSON);
        description2.setQuoteCharacter('"');
        description2.setTimeField("timestamp");
        description2.setTimeFormat("epoch");
        description2.setFieldDelimiter(';');

        assertFalse(description1.equals(description2));
        assertFalse(description2.equals(description1));
    }


    public void testHashCode_GivenEqual() {
        DataDescription dataDescription1 = new DataDescription();
        dataDescription1.setFormat(DataFormat.JSON);
        dataDescription1.setTimeField("timestamp");
        dataDescription1.setQuoteCharacter('\'');
        dataDescription1.setTimeFormat("timeFormat");
        dataDescription1.setFieldDelimiter(',');

        DataDescription dataDescription2 = new DataDescription();
        dataDescription2.setFormat(DataFormat.JSON);
        dataDescription2.setTimeField("timestamp");
        dataDescription2.setQuoteCharacter('\'');
        dataDescription2.setTimeFormat("timeFormat");
        dataDescription2.setFieldDelimiter(',');

        assertEquals(dataDescription1.hashCode(), dataDescription2.hashCode());
    }

    public void testInvalidDataFormat() throws Exception {
        String json = "{ \"format\":\"INEXISTENT_FORMAT\" }";
        XContentParser parser = XContentFactory.xContent(json.getBytes()).createParser(json.getBytes());
        ParsingException ex = expectThrows(ParsingException.class,
                () -> DataDescription.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT));
        assertThat(ex.getMessage(), containsString("[dataDescription] failed to parse field [format]"));
        Throwable cause = ex.getCause();
        assertNotNull(cause);
        assertThat(cause, instanceOf(IllegalArgumentException.class));
        assertThat(cause.getMessage(),
                containsString("No enum constant org.elasticsearch.xpack.prelert.job.DataDescription.DataFormat.INEXISTENT_FORMAT"));
    }

    public void testInvalidFieldDelimiter() throws Exception {
        String json = "{ \"fieldDelimiter\":\",,\" }";
        XContentParser parser = XContentFactory.xContent(json.getBytes()).createParser(json.getBytes());
        ParsingException ex = expectThrows(ParsingException.class,
                () -> DataDescription.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT));
        assertThat(ex.getMessage(), containsString("[dataDescription] failed to parse field [fieldDelimiter]"));
        Throwable cause = ex.getCause();
        assertNotNull(cause);
        assertThat(cause, instanceOf(IllegalArgumentException.class));
        assertThat(cause.getMessage(),
                containsString("String must be a single character, found [,,]"));
    }

    public void testInvalidQuoteCharacter() throws Exception {
        String json = "{ \"quoteCharacter\":\"''\" }";
        XContentParser parser = XContentFactory.xContent(json.getBytes()).createParser(json.getBytes());
        ParsingException ex = expectThrows(ParsingException.class,
                () -> DataDescription.PARSER.apply(parser, () -> ParseFieldMatcher.STRICT));
        assertThat(ex.getMessage(), containsString("[dataDescription] failed to parse field [quoteCharacter]"));
        Throwable cause = ex.getCause();
        assertNotNull(cause);
        assertThat(cause, instanceOf(IllegalArgumentException.class));
        assertThat(cause.getMessage(), containsString("String must be a single character, found ['']"));
    }

    @Override
    protected DataDescription createTestInstance() {
        DataDescription dataDescription = new DataDescription();
        if (randomBoolean()) {
            dataDescription.setFormat(randomFrom(DataFormat.values()));
        }
        if (randomBoolean()) {
            dataDescription.setTimeField(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            dataDescription.setTimeFormat(randomAsciiOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            dataDescription.setFieldDelimiter(randomAsciiOfLength(1).charAt(0));
        }
        if (randomBoolean()) {
            dataDescription.setQuoteCharacter(randomAsciiOfLength(1).charAt(0));
        }
        return dataDescription;
    }

    @Override
    protected Reader<DataDescription> instanceReader() {
        return DataDescription::new;
    }

    @Override
    protected DataDescription parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return DataDescription.PARSER.apply(parser, () -> matcher);
    }
}
