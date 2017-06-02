/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.utils;


/**
 * Parameters used by machine learning for controlling X Content serialisation.
 */
public final class ToXContentParams {

    /**
     * Parameter to indicate whether we are serialising to X Content for cluster state output.
     */
    public static final String FOR_CLUSTER_STATE = "for_cluster_state";

    private ToXContentParams() {
    }
}
