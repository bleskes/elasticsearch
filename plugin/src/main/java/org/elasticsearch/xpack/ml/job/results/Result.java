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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;

/**
 * A wrapper for concrete result objects plus meta information.
 * Also contains common attributes for results.
 */
public class Result<T> {

    /**
     * Serialisation fields
     */
    public static final ParseField TYPE = new ParseField("result");
    public static final ParseField RESULT_TYPE = new ParseField("result_type");
    public static final ParseField TIMESTAMP = new ParseField("timestamp");
    public static final ParseField IS_INTERIM = new ParseField("is_interim");

    @Nullable
    public final String index;
    @Nullable
    public final T result;

    public Result(String index, T result) {
        this.index = index;
        this.result = result;
    }
}
