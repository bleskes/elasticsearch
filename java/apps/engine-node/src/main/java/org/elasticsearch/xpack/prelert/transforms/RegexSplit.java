package org.elasticsearch.xpack.prelert.transforms;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;


public class RegexSplit extends Transform {
    private final Pattern pattern;

    public RegexSplit(String regex, List<TransformIndex> readIndexes,
                      List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);

        pattern = Pattern.compile(regex);
    }

    @Override
    public TransformResult transform(String[][] readWriteArea)
            throws TransformException {
        TransformIndex readIndex = readIndexes.get(0);
        String field = readWriteArea[readIndex.array][readIndex.index];

        String[] split = pattern.split(field);

        warnIfOutputCountIsNotMatched(split.length, field);

        int count = Math.min(split.length, writeIndexes.size());
        for (int i = 0; i < count; i++) {
            TransformIndex index = writeIndexes.get(i);
            readWriteArea[index.array][index.index] = split[i];
        }

        return TransformResult.OK;
    }

    private void warnIfOutputCountIsNotMatched(int splitCount, String field) {
        if (splitCount != writeIndexes.size()) {
            String warning = String.format(
                    "Transform 'split' has %d output(s) but splitting value '%s' resulted to %d part(s)",
                    writeIndexes.size(), field, splitCount);
            logger.warn(warning);
        }
    }
}

