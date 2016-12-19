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

abstract class AbstractJsonRecordReader implements JsonRecordReader {
    static final int PARSE_ERRORS_LIMIT = 100;

    // NORELEASE - Remove direct dependency on Jackson
    protected final JsonParser parser;
    protected final Map<String, Integer> fieldMap;
    protected final String recordHoldingField;
    protected final Logger logger;
    protected int nestedLevel;
    protected long fieldCount;
    protected int errorCounter;

    /**
     * Create a reader that parses the mapped fields from JSON.
     *
     * @param parser
     *            The JSON parser
     * @param fieldMap
     *            Map to field name to record array index position
     * @param recordHoldingField
     *            record holding field
     * @param logger
     *            the logger
     */
    AbstractJsonRecordReader(JsonParser parser, Map<String, Integer> fieldMap, String recordHoldingField, Logger logger) {
        this.parser = Objects.requireNonNull(parser);
        this.fieldMap = Objects.requireNonNull(fieldMap);
        this.recordHoldingField = Objects.requireNonNull(recordHoldingField);
        this.logger = Objects.requireNonNull(logger);
    }

    protected void consumeToField(String field) throws IOException {
        if (field == null || field.isEmpty()) {
            return;
        }
        JsonToken token = null;
        while ((token = tryNextTokenOrReadToEndOnError()) != null) {
            if (token == JsonToken.FIELD_NAME
                    && parser.getCurrentName().equals(field)) {
                tryNextTokenOrReadToEndOnError();
                return;
            }
        }
    }

    protected void consumeToRecordHoldingField() throws IOException {
        consumeToField(recordHoldingField);
    }

    protected void initArrays(String[] record, boolean[] gotFields) {
        Arrays.fill(gotFields, false);
        Arrays.fill(record, "");
    }

    /**
     * Returns null at the EOF or the next token
     */
    protected JsonToken tryNextTokenOrReadToEndOnError() throws IOException {
        try {
            return parser.nextToken();
        } catch (JsonParseException e) {
            logger.warn("Attempting to recover from malformed JSON data.", e);
            for (int i = 0; i <= nestedLevel; ++i) {
                readToEndOfObject();
            }
            clearNestedLevel();
        }

        return parser.getCurrentToken();
    }

    protected abstract void clearNestedLevel();

    /**
     * In some cases the parser doesn't recognise the '}' of a badly formed
     * JSON document and so may skip to the end of the second document. In this
     * case we lose an extra record.
     */
    protected void readToEndOfObject() throws IOException {
        JsonToken token = null;
        do {
            try {
                token = parser.nextToken();
            } catch (JsonParseException e) {
                ++errorCounter;
                if (errorCounter >= PARSE_ERRORS_LIMIT) {
                    logger.error("Failed to recover from malformed JSON data.", e);
                    throw new ElasticsearchParseException("The input JSON data is malformed.");
                }
            }
        }
        while (token != JsonToken.END_OBJECT);
    }
}
