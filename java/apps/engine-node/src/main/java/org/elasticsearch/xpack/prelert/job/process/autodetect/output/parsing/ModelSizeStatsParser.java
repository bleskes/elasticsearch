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
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

// NORELEASE remove this class in favour of ModelSizeStats.PARSER when we remove Jackson
final class ModelSizeStatsParser extends FieldNameParser<ModelSizeStats.Builder> {
    private static final Logger LOGGER = Loggers.getLogger(ModelSizeStats.class);

    public ModelSizeStatsParser(JsonParser jsonParser) {
        super("ModelSizeStats", jsonParser, LOGGER);
    }

    @Override
    protected ModelSizeStats.Builder supply() {
        return new ModelSizeStats.Builder();
    }

    @Override
    protected void handleFieldName(String fieldName, ModelSizeStats.Builder modelSizeStats)
            throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
        case "modelBytes":
            modelSizeStats.setModelBytes(parseAsLongOrZero(fieldName));
            break;
        case "totalByFieldCount":
            modelSizeStats.setTotalByFieldCount(parseAsLongOrZero(fieldName));
            break;
        case "totalOverFieldCount":
            modelSizeStats.setTotalOverFieldCount(parseAsLongOrZero(fieldName));
            break;
        case "totalPartitionFieldCount":
            modelSizeStats.setTotalPartitionFieldCount(parseAsLongOrZero(fieldName));
            break;
        case "bucketAllocationFailuresCount":
            modelSizeStats.setBucketAllocationFailuresCount(parseAsLongOrZero(fieldName));
            break;
        case "memoryStatus":
            int status = parseAsIntOrZero(fieldName);
            modelSizeStats.setMemoryStatus(ModelSizeStats.MemoryStatus.values()[status]);
            break;
        case "bucketTime":
            modelSizeStats.setTimestamp(parseTimestamp(token));
            break;
        default:
            LOGGER.warn(String.format(Locale.ROOT, "Parse error unknown field in ModelSizeStats %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }

    private Date parseTimestamp(JsonToken token) throws IOException {
        long val = 0;
        if (token == JsonToken.VALUE_NUMBER_INT) {
            // convert seconds to ms
            val = parser.getLongValue() * 1000;
        } else {
            LOGGER.warn("Cannot parse " + token.asString() + " as a long");
        }
        return new Date(val);
    }

}
