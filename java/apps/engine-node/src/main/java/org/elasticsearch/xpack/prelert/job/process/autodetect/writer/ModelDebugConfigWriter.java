
package org.elasticsearch.xpack.prelert.job.process.autodetect.writer;

import static org.elasticsearch.xpack.prelert.job.process.autodetect.writer.WriterConstants.EQUALS;
import static org.elasticsearch.xpack.prelert.job.process.autodetect.writer.WriterConstants.NEW_LINE;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

import org.elasticsearch.xpack.prelert.job.ModelDebugConfig;
import org.elasticsearch.xpack.prelert.utils.Strings;

public class ModelDebugConfigWriter {
    private static final String WRITE_TO_STR = "writeto";
    private static final String BOUNDS_PERCENTILE_STR = "boundspercentile";
    private static final String TERMS_STR = "terms";

    private final ModelDebugConfig modelDebugConfig;
    private final Writer writer;

    public ModelDebugConfigWriter(ModelDebugConfig modelDebugConfig, Writer writer) {
        this.modelDebugConfig = Objects.requireNonNull(modelDebugConfig);
        this.writer = Objects.requireNonNull(writer);
    }

    public void write() throws IOException {
        StringBuilder contents = new StringBuilder();
        if (modelDebugConfig.getWriteTo() != null) {
            contents.append(WRITE_TO_STR)
                    .append(EQUALS)
                    .append(modelDebugConfig.getWriteTo())
                    .append(NEW_LINE);
        }

        contents.append(BOUNDS_PERCENTILE_STR)
                .append(EQUALS)
                .append(modelDebugConfig.getBoundsPercentile())
                .append(NEW_LINE);

        contents.append(TERMS_STR)
                .append(EQUALS)
                .append(Strings.nullToEmpty(modelDebugConfig.getTerms()))
                .append(NEW_LINE);

        writer.write(contents.toString());
    }
}
