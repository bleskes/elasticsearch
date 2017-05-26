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
package org.elasticsearch.xpack.ml.job.process.autodetect.state;


/**
 * The categorizer state does not need to be understood on the Java side.
 * The Java code only needs to know how to form the document IDs so that
 * it can retrieve and delete the correct documents.
 */
public class CategorizerState {

    /**
     * Legacy type, now used only as a discriminant in the document ID
     */
    public static final String TYPE = "categorizer_state";

    public static final String documentId(String jobId, int docNum) {
        return jobId + "_" + TYPE + "#" + docNum;
    }

    /**
     * This is how the IDs were formed in v5.4
     */
    public static final String legacyDocumentId(String jobId, int docNum) {
        return jobId + "#" + docNum;
    }

    private CategorizerState() {
    }
}

