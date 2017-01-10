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
package org.elasticsearch.xpack.ml.job.detectionrules;

import java.util.Locale;

public enum RuleAction {
    FILTER_RESULTS;

    /**
     * Case-insensitive from string method.
     *
     * @param value String representation
     * @return The rule action
     */
    public static RuleAction forString(String value) {
        return RuleAction.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
