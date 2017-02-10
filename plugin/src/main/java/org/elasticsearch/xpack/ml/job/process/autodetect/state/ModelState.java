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


import org.elasticsearch.common.ParseField;

/**
 * The serialised models can get very large and only the C++ code
 * understands how to decode them, hence there is no reason to load
 * them into the Java process.
 * However, the Java process DOES set up a mapping on the Elasticsearch
 * index to tell Elasticsearch not to analyse the model state documents
 * in any way.  (Otherwise Elasticsearch would go into a spin trying to
 * make sense of such large JSON documents.)
 */
public class ModelState {
    /**
     * The type of this class used when persisting the data
     */
    public static final ParseField TYPE = new ParseField("model_state");

    private ModelState() {
    }
}

