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
 * The categorizer state does not need to be loaded on the Java side.
 * However, the Java process DOES set up a mapping on the Elasticsearch
 * index to tell Elasticsearch not to analyse the categorizer state documents
 * in any way.
 */
public class CategorizerState {
    /**
     * The type of this class used when persisting the data
     */
    public static final String TYPE = "categorizer_state";

    public static final String categorizerStateDocId(String jobId, int docNum) {
        return jobId + "#" + docNum;
    }

    private CategorizerState() {
    }
}

