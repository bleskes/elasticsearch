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
    private static final String PARTITION_FIELD_OPTION = " partitionfield=";
    private static final String EXCLUDE_FREQUENT_OPTION = " excludefrequent=";

    private DefaultDetectorDescription() {
        // do nothing
    }

    /**
     * Returns the default description for the given {@code detector}
     *
     * @param detector the {@code Detector} for which a default description is requested
     * @return the default description
     */
    public static String of(Detector detector) {
        StringBuilder sb = new StringBuilder();
        appendOn(detector, sb);
        return sb.toString();
    }

    /**
     * Appends to the given {@code StringBuilder} the default description
     * for the given {@code detector}
     *
     * @param detector the {@code Detector} for which a default description is requested
     * @param sb       the {@code StringBuilder} to append to
     */
    public static void appendOn(Detector detector, StringBuilder sb) {
        if (isNotNullOrEmpty(detector.getFunction())) {
            sb.append(detector.getFunction());
            if (isNotNullOrEmpty(detector.getFieldName())) {
                sb.append('(').append(quoteField(detector.getFieldName()))
                .append(')');
            }
        } else if (isNotNullOrEmpty(detector.getFieldName())) {
            sb.append(quoteField(detector.getFieldName()));
        }

        if (isNotNullOrEmpty(detector.getByFieldName())) {
            sb.append(BY_TOKEN).append(quoteField(detector.getByFieldName()));
        }

        if (isNotNullOrEmpty(detector.getOverFieldName())) {
            sb.append(OVER_TOKEN).append(quoteField(detector.getOverFieldName()));
        }

        if (detector.isUseNull()) {
            sb.append(USE_NULL_OPTION).append(detector.isUseNull());
        }

        if (isNotNullOrEmpty(detector.getPartitionFieldName())) {
            sb.append(PARTITION_FIELD_OPTION).append(quoteField(detector.getPartitionFieldName()));
        }

        if (detector.getExcludeFrequent() != null) {
            sb.append(EXCLUDE_FREQUENT_OPTION).append(detector.getExcludeFrequent().getToken());
        }
    }

    private static String quoteField(String field) {
        return PrelertStrings.doubleQuoteIfNotAlphaNumeric(field);
    }

    private static boolean isNotNullOrEmpty(String arg) {
        return !Strings.isNullOrEmpty(arg);
    }
}
