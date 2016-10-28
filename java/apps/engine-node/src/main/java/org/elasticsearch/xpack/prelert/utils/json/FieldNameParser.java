
package org.elasticsearch.xpack.prelert.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base class that allows parsing of simple JSON objects given a JsonParser
 * that either points to the start object, or right after it. The class defines
 * an abstract method that allows clients to specify how the fieldNames should be parsed,
 * without having to duplicate the rest of the parsing process. It also provide helper
 * methods for parsing the next token as various data types.
 */
public abstract class FieldNameParser<T> {

    protected interface ElementParser<T> {
        T parse() throws ElasticsearchParseException, IOException;
    }

    private final String objectName;
    protected final JsonParser parser;
    protected final Logger logger;

    public FieldNameParser(String fieldName, JsonParser jsonParser, Logger logger) {
        objectName = fieldName;
        parser = jsonParser;
        this.logger = logger;
    }

    /**
     * Creates a new object T and populates it from the JSON parser. The parser
     * must be pointing at the start of the object then all the object's fields
     * are read and if they match the property names the appropriate members are
     * set.
     *
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     */
    public T parseJson() throws ElasticsearchParseException {
        T result = supply();
        try {
            parse(result);
        } catch (IOException e) {
            throw new ElasticsearchParseException(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Creates a new object T and populates it from the JSON parser.
     * The parser must be pointing at the first token inside the object.  It
     * is assumed that prior code has validated that the previous token was
     * the start of an object.  Then all the object's fields are read and if
     * they match the property names the appropriate members are set.
     * <p>
     * Does not validate that all the properties (or any) have been set but if
     * parsing fails an exception will be thrown.
     */
    public T parseJsonAfterStartObject() throws ElasticsearchParseException {
        T result = supply();
        try {
            parseAfterStartObject(result);
        } catch (IOException e) {
            throw new ElasticsearchParseException(e.getMessage(), e);
        }
        return result;
    }

    private void parse(T data) throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (JsonToken.START_OBJECT != token) {
            String msg = String.format(
                    "Cannot parse %s. First token '%s', is not the start object token",
                    objectName, parser.getText());
            logger.error(msg);
            throw new ElasticsearchParseException(msg);
        }
        parser.nextToken();
        parseAfterStartObject(data);
    }

    private void parseAfterStartObject(T data) throws IOException {
        JsonToken token = parser.getCurrentToken();
        while (token != JsonToken.END_OBJECT) {
            switch (token) {
            case START_OBJECT:
                logger.error(String.format("Start object parsed in %s", objectName));
                break;
            case END_OBJECT:
                logger.error(String.format("End object parsed in %s", objectName));
                break;
            case FIELD_NAME:
                String fieldName = parser.getCurrentName();
                handleFieldName(fieldName, data);
                break;
            default:
                logger.warn(String.format(
                        "Parsing error: Only simple fields expected in %s not %s",
                        objectName, token));
                break;
            }

            token = parser.nextToken();
        }
    }

    /**
     * Supply a new object to be populated from the parser
     */
    protected abstract T supply();

    protected abstract void handleFieldName(String fieldName, T data) throws IOException;

    protected int parseAsIntOrZero(String fieldName) throws IOException {
        if (parser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue();
        }
        logger.warn("Cannot parse " + fieldName + " : " + parser.getText() + " as an int");
        return 0;
    }

    protected long parseAsLongOrZero(String fieldName) throws IOException {
        if (parser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
            return parser.getLongValue();
        }
        logger.warn("Cannot parse " + fieldName + " : " + parser.getText() + " as a long");
        return 0;
    }

    protected double parseAsDoubleOrZero(String fieldName) throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT) {
            return parser.getDoubleValue();
        }
        logger.warn("Cannot parse " + fieldName + " : " + parser.getText() + " as a double");
        return 0.0;
    }

    protected String parseAsStringOrNull(String fieldName) throws IOException {
        if (parser.getCurrentToken() == JsonToken.VALUE_STRING) {
            return parser.getText();
        }
        logger.warn("Cannot parse " + fieldName + " : " + parser.getText() + " as a string");
        return null;
    }

    protected Boolean parseAsBooleanOrNull(String fieldName) throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token == JsonToken.VALUE_TRUE) {
            return true;
        }
        if (token == JsonToken.VALUE_FALSE) {
            return false;
        }
        logger.warn("Cannot parse " + fieldName + " : " + parser.getText() + " as a boolean");
        return null;
    }

    protected <E> void parseArray(String fieldName, ElementParser<E> elementParser,
            Collection<E> result) throws ElasticsearchParseException, IOException {
        JsonToken token = parser.getCurrentToken();
        if (token != JsonToken.START_ARRAY) {
            String msg = "Invalid value Expecting an array of " + fieldName;
            logger.warn(msg);
            throw new ElasticsearchParseException(msg);
        }

        token = parser.nextToken();
        while (token != JsonToken.END_ARRAY) {
            result.add(elementParser.parse());
            token = parser.nextToken();
        }
    }

    protected double[] parsePrimitiveDoubleArray(String fieldName) throws IOException {
        List<Double> list = new ArrayList<>();
        parseArray(fieldName, () -> parseAsDoubleOrZero(fieldName), list);
        return list.stream().mapToDouble(d -> d).toArray();
    }

    protected List<String> parseStringArray(String fieldName)
            throws IOException {
        List<String> parsedArray = new ArrayList<>();
        parseArray(fieldName, () -> parseAsStringOrNull(fieldName), parsedArray);
        return parsedArray;
    }
}
