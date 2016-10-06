
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.Date;

final class ModelSnapshotParser extends FieldNameParser<ModelSnapshot> {
    private static final Logger LOGGER = Loggers.getLogger(ModelSnapshotParser.class);

    public ModelSnapshotParser(JsonParser jsonParser) {
        super("ModelSnapshot", jsonParser, LOGGER);
    }

    @Override
    protected ModelSnapshot supply() {
        return new ModelSnapshot();
    }

    @Override
    protected void handleFieldName(String fieldName, ModelSnapshot modelSnapshot) throws IOException {
        JsonToken token = parser.nextToken();
        switch (fieldName) {
            case ModelSnapshot.TIMESTAMP:
                modelSnapshot.setTimestamp(new Date(parseAsLongOrZero(fieldName)));
                break;
            case ModelSnapshot.DESCRIPTION:
                modelSnapshot.setDescription(parseAsStringOrNull(fieldName));
                break;
            case ModelSnapshot.RESTORE_PRIORITY:
                modelSnapshot.setRestorePriority(parseAsLongOrZero(fieldName));
                break;
            case ModelSnapshot.SNAPSHOT_ID:
                modelSnapshot.setSnapshotId(parseAsStringOrNull(fieldName));
                break;
            case ModelSnapshot.SNAPSHOT_DOC_COUNT:
                modelSnapshot.setSnapshotDocCount(parseAsIntOrZero(fieldName));
                break;
            case ModelSizeStats.TYPE:
                modelSnapshot.setModelSizeStats(new ModelSizeStatsParser(parser).parseJson());
                break;
            case Quantiles.TYPE:
                modelSnapshot.setQuantiles(new QuantilesParser(parser).parseJson());
                break;
            case ModelSnapshot.LATEST_RECORD_TIME:
                modelSnapshot.setLatestRecordTimeStamp(new Date(parseAsLongOrZero(fieldName)));
                break;
            case ModelSnapshot.LATEST_RESULT_TIME:
                modelSnapshot.setLatestResultTimeStamp(new Date(parseAsLongOrZero(fieldName)));
                break;
            default:
                LOGGER.warn(String.format("Parse error unknown field in ModelSnapshot %s:%s",
                        fieldName, token.asString()));
                break;
        }
    }
}
