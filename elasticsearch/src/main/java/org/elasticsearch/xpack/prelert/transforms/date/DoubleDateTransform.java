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
 * an epoch time in seconds.
 * If isMillisecond is true, it assumes the number represents
 * time in milli-seconds and will convert to seconds
 */
public class DoubleDateTransform extends DateTransform {
    private final boolean isMillisecond;

    public DoubleDateTransform(boolean isMillisecond, List<TransformIndex> readIndexes,
            List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);
        this.isMillisecond = isMillisecond;
    }

    @Override
    protected long toEpochMs(String field) throws TransformException {
        try {
            long longValue = Double.valueOf(field).longValue();
            return isMillisecond ? longValue : longValue * SECONDS_TO_MS;
        } catch (NumberFormatException e) {
            String message = String.format(Locale.ROOT, "Cannot parse timestamp '%s' as epoch value", field);
            throw new ParseTimestampException(message);
        }
    }
}

