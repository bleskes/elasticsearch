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
package org.elasticsearch.xpack.ml.job.results;

import org.elasticsearch.common.ParseField;

/**
 * Common attributes of the result types
 */
public class Result {

    /**
     * Serialisation fields
     */
    public static final ParseField TYPE = new ParseField("result");
    public static final ParseField RESULT_TYPE = new ParseField("result_type");
    public static final ParseField TIMESTAMP = new ParseField("timestamp");
}
