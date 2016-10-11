package org.elasticsearch.xpack.prelert.transforms;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

public class RegexExtract extends Transform {
    private final Pattern pattern;

    public RegexExtract(String regex, List<TransformIndex> readIndexes,
                        List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);

        pattern = Pattern.compile(regex);
    }

    @Override
    public TransformResult transform(String[][] readWriteArea)
            throws TransformException {
        TransformIndex readIndex = readIndexes.get(0);
        String field = readWriteArea[readIndex.array][readIndex.index];

        Matcher match = pattern.matcher(field);

        if (match.find()) {
            int maxMatches = Math.min(writeIndexes.size(), match.groupCount());
            for (int i = 0; i < maxMatches; i++) {
                TransformIndex index = writeIndexes.get(i);
                readWriteArea[index.array][index.index] = match.group(i + 1);
            }

            return TransformResult.OK;
        } else {
            logger.warn("Transform 'extract' failed to match field: " + field);
        }

        return TransformResult.FAIL;
    }
}
