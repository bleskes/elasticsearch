
package org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.utils.json.FieldNameParser;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

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
        case "timestamp":
            modelSnapshot.setTimestamp(new Date(parseAsLongOrZero(fieldName)));
            break;
        case "description":
            modelSnapshot.setDescription(parseAsStringOrNull(fieldName));
            break;
        case "restorePriority":
            modelSnapshot.setRestorePriority(parseAsLongOrZero(fieldName));
            break;
        case "snapshotId":
            modelSnapshot.setSnapshotId(parseAsStringOrNull(fieldName));
            break;
        case "snapshotDocCount":
            modelSnapshot.setSnapshotDocCount(parseAsIntOrZero(fieldName));
            break;
        case "modelSizeStats":
            modelSnapshot.setModelSizeStats(new ModelSizeStatsParser(parser).parseJson());
            break;
        case "quantiles":
            modelSnapshot.setQuantiles(new QuantilesParser(parser).parseJson());
            break;
        case "latestRecordTimeStamp":
            modelSnapshot.setLatestRecordTimeStamp(new Date(parseAsLongOrZero(fieldName)));
            break;
        case "latestResultTimeStamp":
            modelSnapshot.setLatestResultTimeStamp(new Date(parseAsLongOrZero(fieldName)));
            break;
        default:
            LOGGER.warn(String.format(Locale.ROOT, "Parse error unknown field in ModelSnapshot %s:%s",
                    fieldName, token.asString()));
            break;
        }
    }
}
