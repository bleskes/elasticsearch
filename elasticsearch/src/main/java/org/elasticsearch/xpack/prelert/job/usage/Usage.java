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
package org.elasticsearch.xpack.prelert.job.usage;

/**
 * Defines the field names/mappings for the stored
 * usage documents.
 */
public final class Usage {
    public static final String TYPE = "usage";
    public static final String TIMESTAMP = "timestamp";
    public static final String INPUT_BYTES = "inputBytes";
    public static final String INPUT_FIELD_COUNT = "inputFieldCount";
    public static final String INPUT_RECORD_COUNT = "inputRecordCount";

    private Usage() {
    }
}
